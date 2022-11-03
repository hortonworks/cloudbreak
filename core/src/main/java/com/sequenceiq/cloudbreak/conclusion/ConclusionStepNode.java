package com.sequenceiq.cloudbreak.conclusion;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;

public class ConclusionStepNode {

    private Class<? extends ConclusionStep> stepClass;

    private ConclusionStepNode successNode;

    private ConclusionStepNode failureNode;

    private ConclusionStepNode(Class<? extends ConclusionStep> stepClass) {
        this.stepClass = stepClass;
    }

    public static ConclusionStepNode stepNode(Class<? extends ConclusionStep> step) {
        return new ConclusionStepNode(step);
    }

    public ConclusionStepNode withSuccessNode(ConclusionStepNode successNode) {
        this.successNode = successNode;
        return this;
    }

    public ConclusionStepNode withFailureNode(ConclusionStepNode failureNode) {
        this.failureNode = failureNode;
        return this;
    }

    public ConclusionStepNode getChildNode(boolean failure) {
        return failure ? failureNode : successNode;
    }

    public ConclusionStepNode getSuccessNode() {
        return successNode;
    }

    public ConclusionStepNode getFailureNode() {
        return failureNode;
    }

    public Class<? extends ConclusionStep> getStepClass() {
        return stepClass;
    }

    public Set<Class<? extends ConclusionStep>> getAllSteps() {
        Set<Class<? extends ConclusionStep>> steps = new HashSet<>();
        steps.add(stepClass);
        if (successNode != null) {
            steps.addAll(successNode.getAllSteps());
        }
        if (failureNode != null) {
            steps.addAll(failureNode.getAllSteps());
        }
        return steps;
    }

    @Override
    public String toString() {
        StringBuilder stepNode = new StringBuilder();
        stepNode.append("[StepNode: ")
                .append(stepClass.getSimpleName())
                .append(" - success: ");
        if (successNode != null) {
            stepNode.append(successNode.toString());
        } else {
            stepNode.append("stop");
        }
        stepNode.append(" - failure: ");
        if (failureNode != null) {
            stepNode.append(failureNode.toString());
        } else {
            stepNode.append("stop");
        }
        stepNode.append("]");
        return stepNode.toString();
    }
}
