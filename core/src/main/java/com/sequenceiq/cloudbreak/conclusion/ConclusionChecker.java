package com.sequenceiq.cloudbreak.conclusion;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.conclusion.step.Conclusion;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;

public class ConclusionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConclusionChecker.class);

    private ConclusionStepNode rootNode;

    private Map<Class<? extends ConclusionStep>, ConclusionStep> conclusionStepInstances;

    public ConclusionChecker(ConclusionStepNode rootNode, Map<Class<? extends ConclusionStep>, ConclusionStep> conclusionStepInstances) {
        this.rootNode = rootNode;
        this.conclusionStepInstances = conclusionStepInstances;
    }

    public ConclusionResult doCheck(Long resourceId) {
        LOGGER.info("Conclusion checker started, steps: {}, resourceId: {}", rootNode, resourceId);
        List<Conclusion> conclusions = new ArrayList<>();
        ConclusionStepNode actualStepNode = rootNode;
        try {
            while (actualStepNode != null) {
                ConclusionStep conclusionStep = conclusionStepInstances.get(actualStepNode.getStepClass());
                LOGGER.debug("Conclusion step: {}", conclusionStep);

                Conclusion conclusion = measure(() -> conclusionStep.check(resourceId),
                        LOGGER, "Conclusion step finished in {} ms for resourceId {}, step {}", resourceId, conclusionStep.getClass().getSimpleName());
                if (conclusion.isFailureFound()) {
                    LOGGER.debug("Conclusion step found a failure, conclusion: {}, details: {}", conclusion.getConclusion(), conclusion.getDetails());
                } else {
                    LOGGER.debug("Conclusion step succeeded");
                }
                conclusions.add(conclusion);
                actualStepNode = actualStepNode.getChildNode(conclusion.isFailureFound());
            }
            ConclusionResult result = new ConclusionResult(conclusions);
            LOGGER.info("Conclusion checker finished: {}", result.isFailureFound() ? conclusions : "failure not found");
            return result;
        } catch (RuntimeException e) {
            LOGGER.error("Conclusion checker error: {}, collected conclusions before error happened: {}", e.getMessage(), conclusions, e);
            throw e;
        }
    }

}
