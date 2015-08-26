package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.domain.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_IN_PROGRESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Service
public class StackDeletionBasedExitCriteria implements ExitCriteria {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionBasedExitCriteria.class);

    @Override
    public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
        StackDeletionBasedExitCriteriaModel model = (StackDeletionBasedExitCriteriaModel) exitCriteriaModel;
        LOGGER.debug("Check isExitNeeded for model: {}", model);
        Status stackStatus = InMemoryStateStore.get(model.getStackId());
        if (stackStatus != null && (DELETE_COMPLETED.equals(stackStatus) || DELETE_IN_PROGRESS.equals(stackStatus))) {
            LOGGER.warn("Stack is getting terminated, polling is cancelled.");
            return true;
        }
        return false;
    }

    @Override
    public String exitMessage() {
        return "Stack is getting terminated, polling is cancelled.";
    }
}
