package com.monta.ocpp.emulator.v16.data.service

import com.monta.library.ocpp.v16.smartcharge.ChargingProfile
import com.monta.library.ocpp.v16.smartcharge.ChargingProfilePurposeType
import com.monta.library.ocpp.v16.smartcharge.ClearChargingProfileRequest
import com.monta.ocpp.emulator.chargepoint.entity.ChargePointDAO
import com.monta.ocpp.emulator.chargepointconnector.entity.ChargePointConnectorDAO
import com.monta.ocpp.emulator.common.util.ChargingProfileCalculator
import com.monta.ocpp.emulator.v16.data.entity.TxDefaultDAO
import com.monta.ocpp.emulator.v16.data.repository.TxDefaultRepository
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import javax.inject.Singleton

data class AppliedTxDefaultProfile(
    val profileId: Int?,
    val watts: Double?,
)

@Singleton
class TxDefaultService(
    private val txDefaultRepository: TxDefaultRepository,
) {

    fun store(
        chargePoint: ChargePointDAO,
        chargePointConnector: ChargePointConnectorDAO,
        txProfile: ChargingProfile,
    ): TxDefaultDAO {
        return transaction {
            txDefaultRepository.store(chargePoint, chargePointConnector, txProfile)
        }
    }

    fun clear(
        chargePoint: ChargePointDAO,
        connectorDAO: ChargePointConnectorDAO?,
        request: ClearChargingProfileRequest,
    ) {
        if (request.chargingProfilePurpose == null ||
            request.chargingProfilePurpose == ChargingProfilePurposeType.TxDefaultProfile
        ) {
            return transaction {
                txDefaultRepository.delete(chargePoint, connectorDAO, request)
            }
        }
    }

    fun getApplicableWatts(
        chargePoint: ChargePointDAO,
        connectorDAO: ChargePointConnectorDAO,
        fallbackScheduleStart: Instant,
    ): Double? = getApplicableProfile(
        chargePoint = chargePoint,
        connectorDAO = connectorDAO,
        fallbackScheduleStart = fallbackScheduleStart,
    )?.watts

    fun getApplicableProfile(
        chargePoint: ChargePointDAO,
        connectorDAO: ChargePointConnectorDAO,
        fallbackScheduleStart: Instant,
    ): AppliedTxDefaultProfile? {
        return transaction {
            val profile = txDefaultRepository.getApplicable(
                chargePointDAO = chargePoint,
                connectorDAO = connectorDAO,
            )?.txDefaultProfile ?: return@transaction null

            AppliedTxDefaultProfile(
                profileId = profile.chargingProfileId,
                watts = ChargingProfileCalculator.getWatts(
                    chargingProfile = profile,
                    fallbackScheduleStart = fallbackScheduleStart,
                ),
            )
        }
    }
}
