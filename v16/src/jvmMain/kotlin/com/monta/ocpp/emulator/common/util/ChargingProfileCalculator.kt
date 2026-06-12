package com.monta.ocpp.emulator.common.util

import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.v16.smartcharge.ChargingProfile
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedule
import com.monta.ocpp.emulator.chargepointtransaction.entity.ChargePointTransactionDAO
import java.time.Instant

object ChargingProfileCalculator {

    fun getWatts(
        transaction: ChargePointTransactionDAO,
    ): Double? {
        return getWatts(
            chargingProfile = transaction.chargingProfile,
            fallbackScheduleStart = transaction.createdAt,
        )
    }

    internal fun getWatts(
        chargingProfile: ChargingProfile?,
        fallbackScheduleStart: Instant,
        now: Instant = Instant.now(),
    ): Double? {
        if (chargingProfile == null) {
            return null
        }

        val chargingSchedule: ChargingSchedule? = chargingProfile.chargingSchedule

        if (chargingSchedule == null) {
            return null
        }

        chargingProfile.validFrom?.toInstant()?.let { validFrom ->
            if (now < validFrom) return null
        }
        chargingProfile.validTo?.toInstant()?.let { validTo ->
            if (!now.isBefore(validTo)) return null
        }

        val scheduleStart = chargingSchedule.startSchedule?.toInstant() ?: fallbackScheduleStart
        val scheduleEnd: Instant? = chargingSchedule.duration?.let { duration ->
            scheduleStart.plusSeconds(duration.toLong())
        }

        if (now < scheduleStart) {
            return null
        }
        if (scheduleEnd != null && !now.isBefore(scheduleEnd)) {
            return null
        }

        val activePeriod = chargingSchedule.chargingSchedulePeriod
            .filter { it.startPeriod != null && it.limit != null }
            .sortedBy { it.startPeriod }
            .lastOrNull { chargingSchedulePeriod ->
                val periodStart = scheduleStart.plusSeconds(chargingSchedulePeriod.startPeriod!!.toLong())
                !periodStart.isAfter(now)
            } ?: return null

        val appliedLimit = activePeriod.limit?.let { limit ->
            chargingSchedule.minChargingRate?.let { minChargingRate ->
                maxOf(limit, minChargingRate)
            } ?: limit
        } ?: return null
        val phases = activePeriod.numberPhases.coerceAtLeast(1)

        return when (chargingSchedule.chargingRateUnit) {
            ChargingRateUnit.A -> (appliedLimit * 230.0) * phases.toDouble()
            ChargingRateUnit.W -> appliedLimit
            ChargingRateUnit.VAR -> null
            null -> null
        }
    }
}
