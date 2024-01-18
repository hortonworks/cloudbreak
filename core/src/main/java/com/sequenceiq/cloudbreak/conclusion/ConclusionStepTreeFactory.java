package com.sequenceiq.cloudbreak.conclusion;

import static com.sequenceiq.cloudbreak.conclusion.ConclusionStepNode.stepNode;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.conclusion.step.CmStatusCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.InfoCollectorConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NetworkCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NodeServicesCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.SaltCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.VmStatusCheckerConclusionStep;

class ConclusionStepTreeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConclusionStepTreeFactory.class);

    private static final Map<ConclusionCheckerType, ConclusionStepNode> CONCLUSION_CHECKER_TYPE_MAP = new EnumMap<>(ConclusionCheckerType.class);

    static {
        CONCLUSION_CHECKER_TYPE_MAP.put(ConclusionCheckerType.DEFAULT, initDefaultConclusionStepTree());
        CONCLUSION_CHECKER_TYPE_MAP.put(ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP,
                initClusterProvisionBeforeSaltBootstrapConclusionStepTree());
        CONCLUSION_CHECKER_TYPE_MAP.put(ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP,
                initClusterProvisionAfterSaltBootstrapConclusionStepTree());
        CONCLUSION_CHECKER_TYPE_MAP.put(ConclusionCheckerType.STACK_PROVISION, initStackProvisionConclusionStepTree());
    }

    private ConclusionStepTreeFactory() {
    }

    public static ConclusionStepNode getConclusionStepTree(ConclusionCheckerType conclusionCheckerType) {
        if (!CONCLUSION_CHECKER_TYPE_MAP.containsKey(conclusionCheckerType)) {
            String error = "Unknown conclusion checker type: " + conclusionCheckerType;
            LOGGER.error(error);
            throw new IllegalArgumentException(error);
        }
        return CONCLUSION_CHECKER_TYPE_MAP.get(conclusionCheckerType);
    }

    private static ConclusionStepNode initDefaultConclusionStepTree() {
        ConclusionStepNode root = stepNode(InfoCollectorConclusionStep.class)
                .withSuccessNode(stepNode(SaltCheckerConclusionStep.class)
                        .withSuccessNode(stepNode(NodeServicesCheckerConclusionStep.class)
                                .withSuccessNode(stepNode(NetworkCheckerConclusionStep.class)))
                        .withFailureNode(stepNode(CmStatusCheckerConclusionStep.class)
                                .withSuccessNode(stepNode(NetworkCheckerConclusionStep.class))
                                .withFailureNode(stepNode(VmStatusCheckerConclusionStep.class))));
        return root;
    }

    private static ConclusionStepNode initClusterProvisionBeforeSaltBootstrapConclusionStepTree() {
        ConclusionStepNode root = stepNode(InfoCollectorConclusionStep.class)
                .withSuccessNode(stepNode(CmStatusCheckerConclusionStep.class)
                        .withFailureNode(stepNode(VmStatusCheckerConclusionStep.class)));
        return root;
    }

    private static ConclusionStepNode initClusterProvisionAfterSaltBootstrapConclusionStepTree() {
        ConclusionStepNode root = stepNode(InfoCollectorConclusionStep.class)
                .withSuccessNode(stepNode(SaltCheckerConclusionStep.class)
                        .withFailureNode(stepNode(CmStatusCheckerConclusionStep.class)
                                .withFailureNode(stepNode(VmStatusCheckerConclusionStep.class))));
        return root;
    }

    private static ConclusionStepNode initStackProvisionConclusionStepTree() {
        ConclusionStepNode root = stepNode(InfoCollectorConclusionStep.class)
                .withSuccessNode(stepNode(VmStatusCheckerConclusionStep.class));
        return root;
    }
}
