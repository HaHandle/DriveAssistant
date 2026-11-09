package com.pleos.driveassistant.domain.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import com.pleos.driveassistant.domain.model.TrainingStep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class EvaluationResultTest {
    @Test
    public void acceptsBoundaryScoresAndExposesValues() {
        List<String> strengths = Arrays.asList("안전거리 유지", "부드러운 제동");

        EvaluationResult minimum = new EvaluationResult(
                TrainingStep.START_AND_STOP,
                0,
                strengths,
                "정지 위치를 더 일찍 확인하세요.",
                "직선 도로 주행을 연습하세요.");
        EvaluationResult maximum = new EvaluationResult(
                TrainingStep.START_AND_STOP,
                100,
                strengths,
                "현재 감속 패턴을 유지하세요.",
                "곡선 도로 주행을 연습하세요.");

        assertSame(TrainingStep.START_AND_STOP, minimum.getStep());
        assertEquals(0, minimum.getScore());
        assertEquals(strengths, minimum.getStrengths());
        assertEquals("정지 위치를 더 일찍 확인하세요.", minimum.getImprovement());
        assertEquals("직선 도로 주행을 연습하세요.", minimum.getNextTask());
        assertEquals(100, maximum.getScore());
    }

    @Test
    public void rejectsScoresOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> createResult(-1));
        assertThrows(IllegalArgumentException.class, () -> createResult(101));
    }

    @Test
    public void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new EvaluationResult(
                null, 80, Collections.singletonList("안전거리 유지"), "개선점", "다음 과제"));
        assertThrows(NullPointerException.class, () -> new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING, 80, null, "개선점", "다음 과제"));
        assertThrows(NullPointerException.class, () -> new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING,
                80,
                Collections.singletonList("안전거리 유지"),
                null,
                "다음 과제"));
        assertThrows(NullPointerException.class, () -> new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING,
                80,
                Collections.singletonList("안전거리 유지"),
                "개선점",
                null));
    }

    @Test
    public void rejectsBlankImprovementAndNextTask() {
        assertThrows(IllegalArgumentException.class, () -> new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING,
                80,
                Collections.singletonList("안전거리 유지"),
                "   ",
                "다음 과제"));
        assertThrows(IllegalArgumentException.class, () -> new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING,
                80,
                Collections.singletonList("안전거리 유지"),
                "개선점",
                "\t"));
    }

    @Test
    public void strengthsAreDefensivelyCopiedAndReadOnly() {
        List<String> source = new ArrayList<String>();
        source.add("안전거리 유지");
        EvaluationResult result = new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING, 80, source, "개선점", "다음 과제");

        source.add("차선 중앙 유지");

        assertEquals(Collections.singletonList("안전거리 유지"), result.getStrengths());
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.getStrengths().add("추가 강점"));
    }

    private static EvaluationResult createResult(int score) {
        return new EvaluationResult(
                TrainingStep.STRAIGHT_DRIVING,
                score,
                Collections.singletonList("안전거리 유지"),
                "차선 중앙을 더 안정적으로 유지하세요.",
                "곡선 도로 주행을 연습하세요.");
    }
}
