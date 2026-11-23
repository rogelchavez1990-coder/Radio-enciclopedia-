package cu.radioenciclopedia.app

import java.time.DayOfWeek
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ProgramScheduleTest {
    @Test fun currentProgrammeUsesHavanaRatherThanThePhonesTimeZone() {
        assertEquals("hablemos", ProgramSchedule.current(Instant.parse("2026-11-10T03:30:00Z"))?.programId)
        assertEquals("interludio", ProgramSchedule.current(Instant.parse("2026-11-10T05:00:00Z"))?.programId)
    }

    @Test fun utcMondayCanStillUseTheSundayScheduleInCuba() {
        val current = ProgramSchedule.current(Instant.parse("2026-11-09T01:10:00Z"))
        assertEquals("poesia", current?.programId)
        assertFalse(current!!.replay)
    }

    @Test fun sundayOnlyProgrammesAndRepetitionsAreAvailable() {
        val sunday = ProgramSchedule.day(DayOfWeek.SUNDAY)
        assertTrue(sunday.any { it.programId == "poesia" })
        assertEquals(2, sunday.count { it.programId == "secreto" })
        assertTrue(sunday.single { it.programId == "secreto" && it.startMinute == 1230 }.replay)
        assertFalse(ProgramSchedule.day(DayOfWeek.MONDAY).any { it.programId == "poesia" })
    }

    @Test fun stationIdentificationIsNotMislabelledAsAnOngoingProgramme() {
        assertNull(ProgramSchedule.current(Instant.parse("2026-11-10T10:59:00Z")))
    }

    @Test fun documentedFifteenProgrammesHaveOrderedNonOverlappingAirings() {
        assertEquals(15, ProgramSchedule.programs.size)
        for (day in DayOfWeek.values()) {
            val slots = ProgramSchedule.day(day)
            for (slot in slots) {
                assertTrue(slot.startMinute in 0..1439)
                assertTrue(slot.endMinute > slot.startMinute && slot.endMinute <= 1440)
                assertTrue(ProgramSchedule.programs.any { it.id == slot.programId })
            }
            for ((first, next) in slots.zipWithNext()) assertTrue(first.endMinute <= next.startMinute)
        }
    }
}
