package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Service
public class StackDeletionBasedExitCriteria implements ExitCriteria {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionBasedExitCriteria.class);

    @Override
    public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
        StackDeletionBasedExitCriteriaModel model = (StackDeletionBasedExitCriteriaModel) exitCriteriaModel;
        LOGGER.debug("Check isExitNeeded for model: {}", model);
        PollGroup pollGroup = InMemoryStateStore.getStack(model.getStackId());
        if (CANCELLED.equals(pollGroup)) {
            LOGGER.debug("Stack is getting terminated, polling is cancelled.");
            return true;
        }
        return false;
    }

    @Override
    public String exitMessage() {
        return "Stack is getting terminated, polling is cancelled.";
    }
}
