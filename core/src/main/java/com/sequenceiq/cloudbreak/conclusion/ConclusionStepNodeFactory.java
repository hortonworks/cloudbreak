package com.sequenceiq.cloudbreak.conclusion;

import static com.sequenceiq.cloudbreak.conclusion.ConclusionStepNode.stepNode;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.conclusion.step.NetworkCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NodeServicesCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.SaltCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.VmStatusCheckerConclusionStep;

public class ConclusionStepNodeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConclusionStepNodeFactory.class);

    private static final Map<ConclusionCheckerType, ConclusionStepNode> CONCLUSION_CHECKER_TYPE_MAP = new HashMap<>();

    static {
        CONCLUSION_CHECKER_TYPE_MAP.put(ConclusionCheckerType.DEFAULT, initDefaultConclusionCheckerType());
    }

    private ConclusionStepNodeFactory() {
    }

    public static ConclusionStepNode getConclusionStepNode(ConclusionCheckerType conclusionCheckerType) {
        if (!CONCLUSION_CHECKER_TYPE_MAP.containsKey(conclusionCheckerType)) {
            String error = "Unknown conclusion checker type: " + conclusionCheckerType;
            LOGGER.error(error);
            throw new IllegalArgumentException(error);
        }
        return CONCLUSION_CHECKER_TYPE_MAP.get(conclusionCheckerType);
    }

    private static ConclusionStepNode initDefaultConclusionCheckerType() {
        ConclusionStepNode root = stepNode(SaltCheckerConclusionStep.class)
                .withSuccessNode(stepNode(NodeServicesCheckerConclusionStep.class)
                        .withSuccessNode(stepNode(NetworkCheckerConclusionStep.class)))
                .withFailureNode(stepNode(VmStatusCheckerConclusionStep.class));
        return root;
    }
}
