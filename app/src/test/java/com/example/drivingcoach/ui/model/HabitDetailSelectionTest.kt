package com.example.drivingcoach.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HabitDetailSelectionTest {
    private val stop = DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, ObservationCount(2, 3))
    private val signal = DrivingHabitAnalysis(HabitKind.SIGNAL_TIMING, HabitStatus.MET, ObservationCount(3, 3))

    @Test
    fun savedIdentitySurvivesListReorderingInsteadOfSelectingByPosition() {
        val key = signal.kind.name
        assertSame(signal, findHabitDetail(listOf(stop, signal), key))
        assertSame(signal, findHabitDetail(listOf(signal, stop), key))
    }

    @Test
    fun currentObservationsReplaceThePreviouslySelectedAssessment() {
        val updated = signal.copy(status = HabitStatus.REVIEW, observations = ObservationCount(0, 4))
        val selected = findHabitDetail(listOf(updated, stop), signal.kind.name)
        assertSame(updated, selected)
        assertEquals(ObservationCount(0, 4), selected?.observations)
        assertEquals(HabitStatus.MET, signal.status)
    }

    @Test
    fun aRemovedSelectionNeverFallsBackToAnotherHabit() {
        assertNull(findHabitDetail(listOf(stop), signal.kind.name))
        assertNull(findHabitDetail(emptyList(), signal.kind.name))
    }

    @Test
    fun absentOrUnknownSavedKeysDoNotOpenADefaultHabit() {
        for (key in listOf(null, "", "0", "REMOVED_KIND")) {
            assertNull(findHabitDetail(listOf(stop, signal), key))
        }
    }

    @Test
    fun ambiguousDuplicateKindsAreNotPresentedAsOneAssessment() {
        val duplicate = stop.copy(observations = ObservationCount(0, 2))
        val habits = listOf(stop, duplicate, signal)
        assertNull(findHabitDetail(habits, stop.kind.name))
        assertSame(signal, findHabitDetail(habits, signal.kind.name))
    }
}
