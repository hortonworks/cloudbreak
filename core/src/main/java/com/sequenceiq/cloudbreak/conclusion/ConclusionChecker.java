package com.sequenceiq.cloudbreak.conclusion;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStepResult;

public class ConclusionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConclusionChecker.class);

    private final List<ConclusionStep> conclusionSteps;

    private int actualStep;

    private boolean doNext = true;

    public ConclusionChecker(List<ConclusionStep> conclusionSteps) {
        this.conclusionSteps = conclusionSteps;
    }

    public ConclusionResult doCheck(Long resourceId) {
        LOGGER.info("Conclusion checker started, steps: {}, resourceId: {}", conclusionSteps, resourceId);
        List<String> conclusions = new ArrayList<>();
        try {
            while (hasMoreSteps() && doNext()) {
                ConclusionStep conclusionStep = conclusionSteps.get(actualStep++);
                LOGGER.debug("Conclusion step: {}", conclusionStep);

                ConclusionStepResult stepResult = conclusionStep.check(resourceId);
                LOGGER.debug("Conclusion step {}, conclusion: {}", stepResult.isStepFailed() ? "failed" : "succeeded", stepResult.getConclusion());
                if (stepResult.getConclusion() != null) {
                    conclusions.add(stepResult.getConclusion());
                }
                doNext = stepResult.isStepFailed();
            }
            return new ConclusionResult(conclusions);
        } catch (RuntimeException e) {
            LOGGER.error("Conclusion checker error: {}", e.getMessage(), e);
            throw e;
        } finally {
            LOGGER.info("Conclusion checker finished, conclusions: {}", conclusions);
        }
    }

    private boolean hasMoreSteps() {
        return actualStep < conclusionSteps.size();
    }

    private boolean doNext() {
        return doNext;
    }
}
