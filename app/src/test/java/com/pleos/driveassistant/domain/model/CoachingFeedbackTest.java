package com.pleos.driveassistant.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CoachingFeedbackTest {
    @Test
    public void exposesValidFeedbackValues() {
        CoachingFeedback feedback = new CoachingFeedback(
                TrainingStep.LANE_CHANGE,
                FeedbackTiming.LIVE,
                FeedbackPriority.SAFETY,
                "사각지대를 확인하세요.");

        assertSame(TrainingStep.LANE_CHANGE, feedback.getStep());
        assertSame(FeedbackTiming.LIVE, feedback.getTiming());
        assertSame(FeedbackPriority.SAFETY, feedback.getPriority());
        assertEquals("사각지대를 확인하세요.", feedback.getMessage());
    }

    @Test
    public void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new CoachingFeedback(
                null, FeedbackTiming.LIVE, FeedbackPriority.COACHING, "메시지"));
        assertThrows(NullPointerException.class, () -> new CoachingFeedback(
                TrainingStep.LANE_CHANGE, null, FeedbackPriority.COACHING, "메시지"));
        assertThrows(NullPointerException.class, () -> new CoachingFeedback(
                TrainingStep.LANE_CHANGE, FeedbackTiming.LIVE, null, "메시지"));
        assertThrows(NullPointerException.class, () -> new CoachingFeedback(
                TrainingStep.LANE_CHANGE,
                FeedbackTiming.LIVE,
                FeedbackPriority.COACHING,
                null));
    }

    @Test
    public void rejectsBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> new CoachingFeedback(
                TrainingStep.LANE_CHANGE,
                FeedbackTiming.LIVE,
                FeedbackPriority.COACHING,
                "  \t"));
    }
}
