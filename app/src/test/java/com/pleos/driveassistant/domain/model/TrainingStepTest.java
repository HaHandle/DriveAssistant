package com.pleos.driveassistant.domain.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class TrainingStepTest {
    @Test
    public void curriculumContainsExpectedStepsInOrder() {
        TrainingStep[] expected = {
            TrainingStep.PREPARATION,
            TrainingStep.START_AND_STOP,
            TrainingStep.STRAIGHT_DRIVING,
            TrainingStep.CURVE_DRIVING,
            TrainingStep.LANE_CHANGE,
            TrainingStep.INTERSECTION,
            TrainingStep.PARKING
        };

        assertArrayEquals(expected, TrainingStep.values());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(index, expected[index].getOrder());
        }
    }

    @Test
    public void curriculumUsesExpectedTitlesAndObjectives() {
        assertStep(
                TrainingStep.PREPARATION,
                "주행 준비",
                "시트, 미러, 안전벨트와 차량 조작계를 확인합니다.");
        assertStep(
                TrainingStep.START_AND_STOP,
                "출발 및 정차",
                "부드럽게 출발하고 원하는 위치에 안정적으로 정차합니다.");
        assertStep(
                TrainingStep.STRAIGHT_DRIVING,
                "직선 도로 주행",
                "안전거리와 속도를 지키며 차선 중앙을 유지합니다.");
        assertStep(
                TrainingStep.CURVE_DRIVING,
                "곡선 도로 주행",
                "진입 전에 감속하고 부드럽게 조향합니다.");
        assertStep(
                TrainingStep.LANE_CHANGE,
                "차선 변경",
                "주변을 확인하고 충분한 간격에서 안전하게 이동합니다.");
        assertStep(
                TrainingStep.INTERSECTION,
                "교차로 주행",
                "신호, 정지선, 보행자와 통행 우선권을 확인합니다.");
        assertStep(
                TrainingStep.PARKING,
                "주차",
                "주변을 확인하며 저속으로 주차하고 안전하게 마무리합니다.");
    }

    @Test
    public void everyStepHasUniqueOrderAndDisplayContent() {
        Set<Integer> orders = new HashSet<Integer>();

        for (TrainingStep step : TrainingStep.values()) {
            assertFalse(step.getTitle().trim().isEmpty());
            assertFalse(step.getObjective().trim().isEmpty());
            assertFalse("duplicate order: " + step.getOrder(), orders.contains(step.getOrder()));
            orders.add(step.getOrder());
        }

        assertEquals(7, orders.size());
    }

    private static void assertStep(TrainingStep step, String title, String objective) {
        assertEquals(title, step.getTitle());
        assertEquals(objective, step.getObjective());
    }
}
