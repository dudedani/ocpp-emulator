package com.monta.ocpp.emulator.common.util

import com.monta.library.ocpp.common.chargingprofile.ChargingProfileKind
import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.v16.smartcharge.ChargingProfile
import com.monta.library.ocpp.v16.smartcharge.ChargingProfilePurposeType
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedule
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedulePeriod
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChargingProfileCalculatorTest {

    @Test
    fun `applies amp profile while duration is active`() {
        val scheduleStart = Instant.parse("2026-06-12T10:00:00Z")
        val profile = chargingProfile(
            schedule = ChargingSchedule(
                duration = 60,
                startSchedule = scheduleStart.atZone(ZoneOffset.UTC),
                chargingRateUnit = ChargingRateUnit.A,
                chargingSchedulePeriod = listOf(
                    ChargingSchedulePeriod(
                        startPeriod = 0,
                        limit = 18.0,
                        numberPhases = 3,
                    ),
                ),
                minChargingRate = null,
            ),
        )

        assertEquals(
            expected = 12_420.0,
            actual = ChargingProfileCalculator.getWatts(
                chargingProfile = profile,
                fallbackScheduleStart = scheduleStart,
                now = scheduleStart.plusSeconds(30),
            ),
        )
    }

    @Test
    fun `does not apply profile after duration expires`() {
        val scheduleStart = Instant.parse("2026-06-12T10:00:00Z")
        val profile = chargingProfile(
            schedule = ChargingSchedule(
                duration = 60,
                startSchedule = scheduleStart.atZone(ZoneOffset.UTC),
                chargingRateUnit = ChargingRateUnit.A,
                chargingSchedulePeriod = listOf(
                    ChargingSchedulePeriod(
                        startPeriod = 0,
                        limit = 18.0,
                        numberPhases = 3,
                    ),
                ),
                minChargingRate = null,
            ),
        )

        assertNull(
            ChargingProfileCalculator.getWatts(
                chargingProfile = profile,
                fallbackScheduleStart = scheduleStart,
                now = scheduleStart.plusSeconds(60),
            ),
        )
    }

    @Test
    fun `uses latest started schedule period`() {
        val scheduleStart = Instant.parse("2026-06-12T10:00:00Z")
        val profile = chargingProfile(
            schedule = ChargingSchedule(
                duration = null,
                startSchedule = scheduleStart.atZone(ZoneOffset.UTC),
                chargingRateUnit = ChargingRateUnit.A,
                chargingSchedulePeriod = listOf(
                    ChargingSchedulePeriod(
                        startPeriod = 0,
                        limit = 24.0,
                        numberPhases = 3,
                    ),
                    ChargingSchedulePeriod(
                        startPeriod = 300,
                        limit = 12.0,
                        numberPhases = 3,
                    ),
                ),
                minChargingRate = null,
            ),
        )

        assertEquals(
            expected = 8_280.0,
            actual = ChargingProfileCalculator.getWatts(
                chargingProfile = profile,
                fallbackScheduleStart = scheduleStart,
                now = scheduleStart.plusSeconds(301),
            ),
        )
    }

    @Test
    fun `uses watts schedule without amp conversion`() {
        val scheduleStart = Instant.parse("2026-06-12T10:00:00Z")
        val profile = chargingProfile(
            schedule = ChargingSchedule(
                duration = null,
                startSchedule = scheduleStart.atZone(ZoneOffset.UTC),
                chargingRateUnit = ChargingRateUnit.W,
                chargingSchedulePeriod = listOf(
                    ChargingSchedulePeriod(
                        startPeriod = 0,
                        limit = 5_000.0,
                        numberPhases = 3,
                    ),
                ),
                minChargingRate = null,
            ),
        )

        assertEquals(
            expected = 5_000.0,
            actual = ChargingProfileCalculator.getWatts(
                chargingProfile = profile,
                fallbackScheduleStart = scheduleStart,
                now = scheduleStart.plusSeconds(10),
            ),
        )
    }

    private fun chargingProfile(
        schedule: ChargingSchedule,
    ): ChargingProfile {
        return ChargingProfile(
            chargingProfileId = 9301,
            transactionId = 77,
            stackLevel = 10,
            chargingProfilePurpose = ChargingProfilePurposeType.TxProfile,
            chargingProfileKind = ChargingProfileKind.Absolute,
            recurrencyKind = null,
            validFrom = null,
            validTo = null,
            chargingSchedule = schedule,
        )
    }
}
