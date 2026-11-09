package com.example.drivingcoach

import java.util.UUID

internal enum class PracticeLesson(val title: String) {
    StopControl("정차 제어"), LaneChange("차로 변경")
}

internal enum class PracticeEvidence(val lesson: PracticeLesson, val qualifier: String?) {
    LowSpeedStops(PracticeLesson.StopControl, StopSpeedBand.Low.label),
    MediumSpeedStops(PracticeLesson.StopControl, StopSpeedBand.Medium.label),
    LaneChangeChecks(PracticeLesson.LaneChange, null)
}

internal enum class PracticeFocus(val lesson: PracticeLesson, val guidance: String) {
    RepeatStops(PracticeLesson.StopControl, "반복 정차에서 같은 속도대의 기록을 더 모아 보세요."),
    ReviewStops(PracticeLesson.StopControl, "감속 후보가 나온 정차를 복기하고 같은 시나리오를 다시 연습해 보세요."),
    TryLaneChange(PracticeLesson.LaneChange, "다음에는 미리 신호를 켜고 옆 차로의 간격을 확인해 보세요."),
    WaitForClearance(PracticeLesson.LaneChange, "옆 차로가 확인될 때까지 기다리는 장면을 다시 연습해 보세요."),
    SignalEarlier(PracticeLesson.LaneChange, "차로를 바꾸기 전에 맞는 방향의 신호를 미리 켜 보세요."),
    KeepSignal(PracticeLesson.LaneChange, "차로 변경이 끝날 때까지 같은 방향의 신호를 유지해 보세요."),
    CancelSignal(PracticeLesson.LaneChange, "새 차로에 자리 잡으면 방향지시등이 꺼졌는지 확인해 보세요."),
    TryAnotherSituation(PracticeLesson.LaneChange, "다른 방향이나 다른 교통 상황에서도 같은 순서로 연습해 보세요.")
}

internal enum class GapOutcome {
    WaitedForClearance, MovedAfterCheck, ChoseExtraWait, MovedWithoutClearance;

    val metSafetyGoal: Boolean
        get() = this != MovedWithoutClearance
}

internal data class PracticeRecord(
    val id: String,
    val completedAtMillis: Long,
    val evidence: PracticeEvidence,
    val met: Int,
    val observed: Int,
    val focus: PracticeFocus
) {
    val resultText: String
        get() = when (evidence.lesson) {
            PracticeLesson.StopControl -> "강한 감속 후보 미감지 ${met}/${observed}회"
            PracticeLesson.LaneChange -> "관찰한 행동 중 ${met}/${observed}개 충족"
        }

    companion object {
        fun fromStops(result: DecelerationResult): PracticeRecord? {
            // A record describes one approach speed band so different kinds of stops stay comparable.
            val representative = result.stopObservations?.maxByOrNull { it.observedStops } ?: return null
            val evidence = when (representative.speedBand) {
                StopSpeedBand.Low -> PracticeEvidence.LowSpeedStops
                StopSpeedBand.Medium -> PracticeEvidence.MediumSpeedStops
            }
            val focus = when {
                representative.observedStops < MIN_COMPARABLE_STOPS -> PracticeFocus.RepeatStops
                representative.stopsWithStrongCandidate > 0 -> PracticeFocus.ReviewStops
                else -> PracticeFocus.TryLaneChange
            }
            return new(evidence, representative.stopsWithoutStrongCandidate, representative.observedStops, focus)
        }

        fun fromLaneChange(report: LaneChangeSignalReport, gap: GapOutcome): PracticeRecord {
            val observed = 1 + listOf(
                report.onset != SignalOnset.Unobserved,
                report.hold != SignalHold.Unobserved,
                report.clear != SignalClear.Unobserved
            ).count { it }
            val met = listOf(
                gap.metSafetyGoal,
                report.onset == SignalOnset.InAdvance,
                report.hold == SignalHold.Maintained,
                report.clear == SignalClear.InTime
            ).count { it }
            return new(PracticeEvidence.LaneChangeChecks, met, observed, focusForLaneChange(report, gap))
        }

        fun focusForLaneChange(report: LaneChangeSignalReport, gap: GapOutcome): PracticeFocus = when {
            !gap.metSafetyGoal -> PracticeFocus.WaitForClearance
            report.onset != SignalOnset.InAdvance -> PracticeFocus.SignalEarlier
            report.hold != SignalHold.Maintained -> PracticeFocus.KeepSignal
            report.clear != SignalClear.InTime -> PracticeFocus.CancelSignal
            else -> PracticeFocus.TryAnotherSituation
        }

        private fun new(evidence: PracticeEvidence, met: Int, observed: Int, focus: PracticeFocus) =
            PracticeRecord(UUID.randomUUID().toString(), System.currentTimeMillis(), evidence, met, observed, focus)
    }
}
