package com.sequenceiq.freeipa.orchestrator;

import java.util.Objects;
import java.util.Optional;

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class StackBasedExitCriteriaModel extends ExitCriteriaModel {
    private final Long stackId;

    public StackBasedExitCriteriaModel(Long stackId) {
        this.stackId = stackId;
    }

    public static ExitCriteriaModel nonCancellableModel() {
        return new StackBasedExitCriteriaModel(null);
    }

    public Optional<Long> getStackId() {
        return Optional.ofNullable(stackId);
    }

    @Override
    public String toString() {
        return "StackBasedExitCriteriaModel{"
                + "stackId=" + stackId
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            StackBasedExitCriteriaModel that = (StackBasedExitCriteriaModel) o;
            return Objects.equals(stackId, that.stackId);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stackId);
    }
}
