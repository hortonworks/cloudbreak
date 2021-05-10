package com.sequenceiq.cloudbreak.conclusion;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.conclusion.step.Conclusion;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;

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
        List<Conclusion> conclusions = new ArrayList<>();
        try {
            while (hasMoreSteps() && doNext()) {
                ConclusionStep conclusionStep = conclusionSteps.get(actualStep++);
                LOGGER.debug("Conclusion step: {}", conclusionStep);

                Conclusion conclusion = measure(() -> conclusionStep.check(resourceId),
                        LOGGER, "Conclusion step finished in {} ms for resourceId {}, step {}", resourceId, conclusionStep.getClass().getSimpleName());
                if (conclusion.isFailureFound()) {
                    LOGGER.debug("Conclusion step found a failure, conclusion: {}, details: {}", conclusion.getConclusion(), conclusion.getDetails());
                } else {
                    LOGGER.debug("Conclusion step succeeded");
                }
                conclusions.add(conclusion);
                doNext = conclusion.isFailureFound();
            }
            ConclusionResult result = new ConclusionResult(conclusions);
            LOGGER.info("Conclusion checker finished: {}", result.isFailureFound() ? "failure not found" : conclusions);
            return result;
        } catch (RuntimeException e) {
            LOGGER.error("Conclusion checker error: {}, collected conclusions before error happened: {}", e.getMessage(), conclusions, e);
            throw e;
        }
    }

    private boolean hasMoreSteps() {
        return actualStep < conclusionSteps.size();
    }

    private boolean doNext() {
        return doNext;
    }
}
