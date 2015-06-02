package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;

public class StackDeletionBasedExitCriteriaModel extends ExitCriteriaModel {

    private Long stackId;

    public StackDeletionBasedExitCriteriaModel(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public static final ExitCriteriaModel stackDeletionBasedExitCriteriaModel(Long stackId) {
        return new StackDeletionBasedExitCriteriaModel(stackId);
    }
}
