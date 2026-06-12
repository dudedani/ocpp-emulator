package com.monta.ocpp.emulator.chargepoint.service

import com.monta.library.ocpp.v16.core.ChargePointStatus
import com.monta.ocpp.emulator.chargepoint.entity.ChargePointDAO
import com.monta.ocpp.emulator.chargepoint.model.ChargePointDisplay
import com.monta.ocpp.emulator.chargepoint.model.ChargePointDisplayState
import com.monta.ocpp.emulator.chargepointconnector.entity.ChargePointConnectorDAO
import com.monta.ocpp.emulator.chargepointtransaction.entity.ChargePointTransactionDAO
import org.jetbrains.exposed.sql.transactions.transaction
import javax.inject.Singleton

@Singleton
class ChargePointDisplayService {

    fun updateIdleDisplay(
        chargePoint: ChargePointDAO,
    ) {
        val state = transaction {
            val connector = chargePoint.getConnectors().minByOrNull { it.position }
            ChargePointDisplayState(
                connected = chargePoint.connected,
                status = connector?.status ?: chargePoint.status,
                connectorId = connector?.position,
            )
        }
        updateDisplay(chargePoint, state)
    }

    fun updateTransactionDisplay(
        chargePoint: ChargePointDAO,
        connector: ChargePointConnectorDAO,
        chargePointTransaction: ChargePointTransactionDAO,
        chargingProfileWatts: Double?,
    ) {
        val state = transaction {
            ChargePointDisplayState(
                connected = chargePoint.connected,
                status = connector.status,
                connectorId = connector.position,
                transactionId = chargePointTransaction.externalId,
                watts = connector.kw * 1000,
                numberPhases = connector.vehicleNumberPhases,
                energyWh = chargePointTransaction.endMeter,
                startTime = chargePointTransaction.startTime,
                chargingProfileWatts = chargingProfileWatts,
            )
        }
        updateDisplay(chargePoint, state)
    }

    fun updateDisconnectedDisplay(
        chargePoint: ChargePointDAO,
    ) {
        updateDisplay(
            chargePoint = chargePoint,
            state = ChargePointDisplayState(
                connected = false,
                status = ChargePointStatus.Unavailable,
            ),
        )
    }

    private fun updateDisplay(
        chargePoint: ChargePointDAO,
        state: ChargePointDisplayState,
    ) {
        transaction {
            chargePoint.displayAutomaticText = ChargePointDisplay.automaticText(state)
            chargePoint.displayText = ChargePointDisplay.merge(
                automaticText = chargePoint.displayAutomaticText,
                overrideText = chargePoint.displayOverrideText,
            )
        }
    }
}
