package com.sequenceiq.freeipa.orchestrator;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class StackBasedExitCriteria implements ExitCriteria {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackBasedExitCriteria.class);

    @Override
    public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
        StackBasedExitCriteriaModel model = (StackBasedExitCriteriaModel) exitCriteriaModel;
        LOGGER.debug("Check isExitNeeded for model: {}", model);

        Optional<Long> stackIdOpt = model.getStackId();
        if (stackIdOpt.isPresent()) {
            Optional<PollGroup> stackPollGroup = Optional.ofNullable(InMemoryStateStore.getStack(stackIdOpt.get()));
            if (stackPollGroup.isPresent() && CANCELLED.equals(stackPollGroup.get())) {
                LOGGER.debug("Stack is getting terminated, polling is cancelled.");
                return true;
            } else if (stackPollGroup.isEmpty()) {
                LOGGER.debug("No InMemoryState found, cancel polling");
                return true;
            }
        }
        return false;
    }

    @Override
    public String exitMessage() {
        return "FreeIPA is getting terminated, polling is cancelled";
    }
}
