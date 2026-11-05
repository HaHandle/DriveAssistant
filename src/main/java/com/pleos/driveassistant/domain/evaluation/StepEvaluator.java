package com.pleos.driveassistant.domain.evaluation;

import com.pleos.driveassistant.domain.model.CoachingFeedback;
import com.pleos.driveassistant.domain.model.TrainingStep;
import java.util.Optional;

/**
 * Contract implemented by every training feature.
 *
 * @param <S> a feature-specific sample or accumulated evaluation state
 */
public interface StepEvaluator<S> {
    TrainingStep getStep();

    Optional<CoachingFeedback> observe(S sample);

    EvaluationResult finish();

    void reset();
}

