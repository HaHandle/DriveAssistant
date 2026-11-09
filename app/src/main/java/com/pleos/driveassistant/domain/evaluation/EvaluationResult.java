package com.pleos.driveassistant.domain.evaluation;

import com.pleos.driveassistant.domain.model.TrainingStep;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Result shown after driving; detailed results should not distract a moving driver. */
public final class EvaluationResult {
    private final TrainingStep step;
    private final int score;
    private final List<String> strengths;
    private final String improvement;
    private final String nextTask;

    public EvaluationResult(
            TrainingStep step,
            int score,
            List<String> strengths,
            String improvement,
            String nextTask) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        this.step = Objects.requireNonNull(step, "step");
        this.score = score;
        this.strengths = Collections.unmodifiableList(
                new ArrayList<String>(Objects.requireNonNull(strengths, "strengths")));
        this.improvement = requireText(improvement, "improvement");
        this.nextTask = requireText(nextTask, "nextTask");
    }

    public TrainingStep getStep() {
        return step;
    }

    public int getScore() {
        return score;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public String getImprovement() {
        return improvement;
    }

    public String getNextTask() {
        return nextTask;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

