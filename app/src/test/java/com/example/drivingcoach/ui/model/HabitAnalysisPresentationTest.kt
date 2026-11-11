package com.example.drivingcoach.ui.model

import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitAnalysisPresentationTest {
    @Test
    fun missingEvidenceDefersEveryReportedStatusWithoutInventingCounts() {
        for (status in HabitStatus.entries) {
            val result = DrivingHabitAnalysis(HabitKind.STOP_CONTROL, status)
                .toHabitAnalysisPresentation()

            assertEquals(status, result.reportedStatus)
            assertEquals(HabitEvidenceState.NOT_PROVIDED, result.evidence)
            assertEquals(HabitAssessmentState.DEFERRED, result.assessment)
            assertNull(result.observations)
            assertNull(result.countsForDisplay)
        }
    }

    @Test
    fun zeroObservationsAreRetainedButNeverDisplayedAsZeroOutOfZero() {
        for (status in HabitStatus.entries) {
            val counts = ObservationCount(0, 0)
            val result = DrivingHabitAnalysis(HabitKind.STOP_CONTROL, status, counts)
                .toHabitAnalysisPresentation()

            assertEquals(status, result.reportedStatus)
            assertEquals(counts, result.observations)
            assertEquals(HabitEvidenceState.NO_OBSERVATIONS, result.evidence)
            assertEquals(HabitAssessmentState.DEFERRED, result.assessment)
            assertNull(result.countsForDisplay)
        }
    }

    @Test
    fun observedUnmetGoalsKeepTheirCountsAndReviewAssessment() {
        val counts = ObservationCount(0, 3)
        val result = DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, counts)
            .toHabitAnalysisPresentation()

        assertEquals(HabitEvidenceState.NONE_MET, result.evidence)
        assertEquals(HabitAssessmentState.REVIEW, result.assessment)
        assertEquals(counts, result.countsForDisplay)
    }

    @Test
    fun someMetObservationsDoNotPromoteAReportedReview() {
        val result = DrivingHabitAnalysis(HabitKind.SIGNAL_TIMING, HabitStatus.REVIEW, ObservationCount(2, 3))
            .toHabitAnalysisPresentation()

        assertEquals(HabitEvidenceState.HAS_MET, result.evidence)
        assertEquals(HabitAssessmentState.REVIEW, result.assessment)
    }

    @Test
    fun allMetObservationsStillDoNotPromoteAReportedReview() {
        val result = DrivingHabitAnalysis(HabitKind.SIGNAL_HOLD, HabitStatus.REVIEW, ObservationCount(3, 3))
            .toHabitAnalysisPresentation()

        assertEquals(HabitAssessmentState.REVIEW, result.assessment)
        assertEquals(ObservationCount(3, 3), result.countsForDisplay)
    }

    @Test
    fun reportedMetDoesNotRequireAnInventedAllMetThreshold() {
        val result = DrivingHabitAnalysis(HabitKind.SIGNAL_CANCEL, HabitStatus.MET, ObservationCount(1, 3))
            .toHabitAnalysisPresentation()

        assertEquals(HabitEvidenceState.HAS_MET, result.evidence)
        assertEquals(HabitAssessmentState.MET, result.assessment)
        assertEquals(ObservationCount(1, 3), result.countsForDisplay)
    }

    @Test
    fun anUnobservedAssessmentIsNotInferredFromPositiveCounts() {
        val result = DrivingHabitAnalysis(HabitKind.SIGNAL_TIMING, HabitStatus.UNOBSERVED, ObservationCount(3, 3))
            .toHabitAnalysisPresentation()

        assertEquals(HabitStatus.UNOBSERVED, result.reportedStatus)
        assertEquals(HabitEvidenceState.HAS_MET, result.evidence)
        assertEquals(HabitAssessmentState.DEFERRED, result.assessment)
        assertEquals(ObservationCount(3, 3), result.countsForDisplay)
    }

    @Test
    fun aReportedSuccessWithoutAnyMetObservationIsDeferredAndPreserved() {
        val source = DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.MET, ObservationCount(0, 3))
        val result = source.toHabitAnalysisPresentation()

        assertEquals(HabitStatus.MET, result.reportedStatus)
        assertEquals(HabitEvidenceState.NONE_MET, result.evidence)
        assertEquals(HabitAssessmentState.DEFERRED, result.assessment)
        assertEquals(source.observations, result.countsForDisplay)
        assertEquals(HabitStatus.MET, source.status)
    }

    @Test
    fun eachHabitKeepsItsIdentityAndIndependentAssessment() {
        for (kind in HabitKind.entries) {
            val result = DrivingHabitAnalysis(kind, HabitStatus.REVIEW, ObservationCount(0, 1))
                .toHabitAnalysisPresentation()

            assertEquals(kind, result.kind)
            assertEquals(HabitAssessmentState.REVIEW, result.assessment)
            assertEquals(ObservationCount(0, 1), result.countsForDisplay)
        }
    }

    @Test
    fun largeCountsArePreservedWithoutPercentageArithmetic() {
        val counts = ObservationCount(Int.MAX_VALUE - 1, Int.MAX_VALUE)
        val result = DrivingHabitAnalysis(HabitKind.SIGNAL_HOLD, HabitStatus.REVIEW, counts)
            .toHabitAnalysisPresentation()

        assertEquals(counts, result.countsForDisplay)
        assertEquals(HabitAssessmentState.REVIEW, result.assessment)
    }

    @Test
    fun normalMockKeepsExistingHabitOrderStatusesAndCounts() {
        val overview = (MockDriverOverviewData.state(OverviewScenario.NORMAL) as DriverOverviewState.Ready).overview
        val results = overview.habits.map { it.toHabitAnalysisPresentation() }

        assertEquals(overview.habits.map { it.kind }, results.map { it.kind })
        assertEquals(overview.habits.map { it.status }, results.map { it.reportedStatus })
        assertEquals(overview.habits.map { it.observations }, results.map { it.countsForDisplay })
        assertEquals(listOf(HabitAssessmentState.REVIEW, HabitAssessmentState.MET,
            HabitAssessmentState.MET, HabitAssessmentState.REVIEW), results.map { it.assessment })
    }

    @Test
    fun partialMockDistinguishesMissingEvidenceFromZeroObservations() {
        val overview = (MockDriverOverviewData.state(OverviewScenario.PARTIAL) as DriverOverviewState.Ready).overview
        val results = overview.habits.associate { it.kind to it.toHabitAnalysisPresentation() }
        val hold = results.getValue(HabitKind.SIGNAL_HOLD)
        val cancel = results.getValue(HabitKind.SIGNAL_CANCEL)

        assertEquals(HabitEvidenceState.NOT_PROVIDED, hold.evidence)
        assertEquals(HabitEvidenceState.NO_OBSERVATIONS, cancel.evidence)
        assertEquals(HabitAssessmentState.DEFERRED, hold.assessment)
        assertEquals(HabitAssessmentState.DEFERRED, cancel.assessment)
        assertNull(hold.countsForDisplay)
        assertNull(cancel.countsForDisplay)
    }
}
