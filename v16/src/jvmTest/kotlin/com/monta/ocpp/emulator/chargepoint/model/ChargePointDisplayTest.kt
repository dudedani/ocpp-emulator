package com.monta.ocpp.emulator.chargepoint.model

import com.monta.library.ocpp.v16.core.ChargePointStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargePointDisplayTest {

    @Test
    fun `renders non blank idle display`() {
        val display = ChargePointDisplay.automaticText(
            ChargePointDisplayState(
                connected = true,
                status = ChargePointStatus.Available,
                connectorId = 1,
            ),
        )

        assertEquals(
            """
            Available
            Connector: 1
            Power: 0.0 kW  Current: 0.0 A
            Energy: -
            Tx: -
            """.trimIndent(),
            display,
        )
    }

    @Test
    fun `renders active transaction display`() {
        val display = ChargePointDisplay.automaticText(
            ChargePointDisplayState(
                connected = true,
                status = ChargePointStatus.Charging,
                connectorId = 1,
                transactionId = 417,
                watts = 7_360.0,
                numberPhases = 1,
                energyWh = 12_400.0,
                startTime = Instant.now(),
            ),
        )

        assertEquals(
            """
            Charging
            Power: 7.4 kW  Current: 32.0 A
            Energy: 12.4 kWh
            Profile: none
            Tx: 417  Connector: 1
            """.trimIndent(),
            display,
        )
    }

    @Test
    fun `renders smart charging limit`() {
        val display = ChargePointDisplay.automaticText(
            ChargePointDisplayState(
                connected = true,
                status = ChargePointStatus.SuspendedEVSE,
                connectorId = 1,
                transactionId = 418,
                watts = 0.0,
                numberPhases = 3,
                energyWh = 1_000.0,
                startTime = Instant.now(),
                chargingProfileWatts = 12_420.0,
                txProfileId = 9301,
            ),
        )

        assertEquals(
            """
            SuspendedEVSE
            Power: 0.0 kW  Current: 0.0 A
            Energy: 1.0 kWh
            TxProfile: 9301  Limit: 18.0 A
            Tx: 418  Connector: 1
            """.trimIndent(),
            display,
        )
    }

    @Test
    fun `renders applied default tx profile`() {
        val display = ChargePointDisplay.automaticText(
            ChargePointDisplayState(
                connected = true,
                status = ChargePointStatus.Charging,
                connectorId = 1,
                transactionId = 419,
                watts = 11_040.0,
                numberPhases = 3,
                energyWh = 2_000.0,
                startTime = Instant.now(),
                chargingProfileWatts = 11_040.0,
                appliedDefaultTxProfileId = 9100,
            ),
        )

        assertEquals(
            """
            Charging
            Power: 11.0 kW  Current: 16.0 A
            Energy: 2.0 kWh
            DefaultTxProfile: 9100  Limit: 16.0 A
            Tx: 419  Connector: 1
            """.trimIndent(),
            display,
        )
    }

    @Test
    fun `manual override lines merge over automatic display`() {
        val automatic = ChargePointDisplay.automaticText(
            ChargePointDisplayState(
                connected = true,
                status = ChargePointStatus.Charging,
                connectorId = 1,
                transactionId = 417,
            ),
        )
        val override = ChargePointDisplay.overrideText(
            currentOverrideText = null,
            lineIndex = 0,
            text = "Smart Charging",
        )

        assertEquals(
            """
            Smart Charging
            Power: 0.0 kW  Current: 0.0 A
            Energy: -
            Profile: none
            Tx: 417  Connector: 1
            """.trimIndent(),
            ChargePointDisplay.merge(automatic, override),
        )
        assertEquals(automatic, ChargePointDisplay.merge(automatic, null))
    }
}
