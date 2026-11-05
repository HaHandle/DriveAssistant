package com.pleos.driveassistant.domain.model;

/** Ordered curriculum used by the training session and dashboard. */
public enum TrainingStep {
    PREPARATION(0, "주행 준비", "시트, 미러, 안전벨트와 차량 조작계를 확인합니다."),
    START_AND_STOP(1, "출발 및 정차", "부드럽게 출발하고 원하는 위치에 안정적으로 정차합니다."),
    STRAIGHT_DRIVING(2, "직선 도로 주행", "안전거리와 속도를 지키며 차선 중앙을 유지합니다."),
    CURVE_DRIVING(3, "곡선 도로 주행", "진입 전에 감속하고 부드럽게 조향합니다."),
    LANE_CHANGE(4, "차선 변경", "주변을 확인하고 충분한 간격에서 안전하게 이동합니다."),
    INTERSECTION(5, "교차로 주행", "신호, 정지선, 보행자와 통행 우선권을 확인합니다."),
    PARKING(6, "주차", "주변을 확인하며 저속으로 주차하고 안전하게 마무리합니다.");

    private final int order;
    private final String title;
    private final String objective;

    TrainingStep(int order, String title, String objective) {
        this.order = order;
        this.title = title;
        this.objective = objective;
    }

    public int getOrder() {
        return order;
    }

    public String getTitle() {
        return title;
    }

    public String getObjective() {
        return objective;
    }
}

