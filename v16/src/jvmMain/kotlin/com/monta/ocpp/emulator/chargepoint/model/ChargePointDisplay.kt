package com.monta.ocpp.emulator.chargepoint.model

import com.monta.library.ocpp.v16.core.ChargePointStatus
import java.time.Instant
import java.util.Locale

data class ChargePointDisplayState(
    val connected: Boolean,
    val status: ChargePointStatus,
    val connectorId: Int? = null,
    val transactionId: Int? = null,
    val watts: Double = 0.0,
    val numberPhases: Int = 3,
    val energyWh: Double? = null,
    val startTime: Instant? = null,
    val chargingProfileWatts: Double? = null,
    val txProfileId: Int? = null,
    val appliedDefaultTxProfileId: Int? = null,
)

object ChargePointDisplay {
    const val LINE_COUNT = 5
    const val MAX_LINE_LENGTH = 80
    const val TEXT_LENGTH = LINE_COUNT * (MAX_LINE_LENGTH + 1)

    val defaultText: String = toText(
        listOf(
            "Disconnected",
            "Connector: -",
            "Power: 0.0 kW  Current: 0.0 A",
            "Energy: -",
            "Tx: -",
        ),
    )

    fun automaticText(
        state: ChargePointDisplayState,
    ): String {
        return toText(
            if (state.transactionId == null) {
                idleLines(state)
            } else {
                transactionLines(state)
            },
        )
    }

    fun merge(
        automaticText: String,
        overrideText: String?,
    ): String {
        val automaticLines = toLines(automaticText)
        val overrideLines = toOverrideLines(overrideText)
        return toText(
            automaticLines.mapIndexed { index, automaticLine ->
                overrideLines.getOrNull(index) ?: automaticLine
            },
        )
    }

    fun overrideText(
        currentOverrideText: String?,
        lineIndex: Int,
        text: String,
    ): String {
        val lines = toOverrideLines(currentOverrideText).toMutableList()
        lines[lineIndex] = text
        return toText(lines.map { it ?: "" })
    }

    fun toText(
        lines: List<String>,
    ): String {
        return (0 until LINE_COUNT)
            .map { index -> lines.getOrNull(index).orEmpty().take(MAX_LINE_LENGTH) }
            .joinToString("\n")
    }

    private fun idleLines(
        state: ChargePointDisplayState,
    ): List<String> {
        val status = if (state.connected) state.status.name else "Disconnected"
        return listOf(
            status,
            "Connector: ${state.connectorId ?: "-"}",
            "Power: 0.0 kW  Current: 0.0 A",
            "Energy: -",
            "Tx: -",
        )
    }

    private fun transactionLines(
        state: ChargePointDisplayState,
    ): List<String> {
        return listOf(
            state.status.name,
            "Power: ${formatKw(state.watts)} kW  Current: ${formatAmps(state.watts, state.numberPhases)} A",
            "Energy: ${formatKwh(state.energyWh)}",
            profileLine(state),
            "Tx: ${state.transactionId}  Connector: ${state.connectorId ?: "-"}",
        )
    }

    private fun profileLine(
        state: ChargePointDisplayState,
    ): String {
        val profile = when {
            state.txProfileId != null -> "TxProfile: ${state.txProfileId}"
            state.appliedDefaultTxProfileId != null -> "DefaultTxProfile: ${state.appliedDefaultTxProfileId}"
            else -> "Profile: none"
        }
        val profileWatts = state.chargingProfileWatts ?: return profile
        return "$profile  Limit: ${formatAmps(profileWatts, state.numberPhases)} A"
    }

    private fun toLines(
        text: String,
    ): List<String> {
        return text.split("\n").let { lines ->
            (0 until LINE_COUNT).map { index -> lines.getOrNull(index).orEmpty() }
        }
    }

    private fun toOverrideLines(
        text: String?,
    ): List<String?> {
        if (text == null) {
            return List(LINE_COUNT) { null }
        }
        return toLines(text).map { line -> line.takeIf { it.isNotBlank() } }
    }

    private fun formatKw(
        watts: Double,
    ): String {
        return formatOneDecimal(watts / 1000.0)
    }

    private fun formatKwh(
        energyWh: Double?,
    ): String {
        if (energyWh == null) return "-"
        return "${formatOneDecimal(energyWh / 1000.0)} kWh"
    }

    private fun formatAmps(
        watts: Double,
        numberPhases: Int,
    ): String {
        val phases = numberPhases.coerceAtLeast(1)
        return formatOneDecimal((watts / phases.toDouble()) / 230.0)
    }

    private fun formatOneDecimal(
        value: Double,
    ): String {
        return "%.1f".format(Locale.US, value)
    }
}
