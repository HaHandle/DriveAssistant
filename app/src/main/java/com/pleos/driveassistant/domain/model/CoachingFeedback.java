package com.pleos.driveassistant.domain.model;

import java.util.Objects;

/** Framework-independent message selected by a feature evaluator. */
public final class CoachingFeedback {
    private final TrainingStep step;
    private final FeedbackTiming timing;
    private final FeedbackPriority priority;
    private final String message;

    public CoachingFeedback(
            TrainingStep step,
            FeedbackTiming timing,
            FeedbackPriority priority,
            String message) {
        this.step = Objects.requireNonNull(step, "step");
        this.timing = Objects.requireNonNull(timing, "timing");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.message = requireText(message, "message");
    }

    public TrainingStep getStep() {
        return step;
    }

    public FeedbackTiming getTiming() {
        return timing;
    }

    public FeedbackPriority getPriority() {
        return priority;
    }

    public String getMessage() {
        return message;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

