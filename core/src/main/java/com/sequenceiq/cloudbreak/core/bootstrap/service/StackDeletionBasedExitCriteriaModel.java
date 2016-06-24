package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class StackDeletionBasedExitCriteriaModel extends ExitCriteriaModel {

    private Long stackId;

    public StackDeletionBasedExitCriteriaModel(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public static ExitCriteriaModel stackDeletionBasedExitCriteriaModel(Long stackId) {
        return new StackDeletionBasedExitCriteriaModel(stackId);
    }

    @Override
    public String toString() {
        return "StackDeletionBasedExitCriteriaModel{"
                + "stackId=" + stackId + '}';
    }
}
