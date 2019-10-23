package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class AvailableStackProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvailableStackProvider.class);

    @Inject
    private StackService stackService;

    public List<Stack> getAvailableStacks() {
        LOGGER.debug("Retrieving all stacks");
        return filterAvailableStacks(stackService.findAllRunning());
    }

    public List<Stack> getAvailableStacksByAccountId(String accountId) {
        LOGGER.debug("Retrieving stacks for account {}", accountId);
        return filterAvailableStacks(stackService.getAllByAccountId(accountId));
    }

    public List<Stack> getAvailableStacksByAccountIdAndEnvironmentCrns(String accountId, Collection<String> environmentCrnFilter) {
        if (environmentCrnFilter.isEmpty()) {
            return getAvailableStacksByAccountId(accountId);
        } else {
            LOGGER.debug("Retrieving stacks for account {} that match environment crns {}", accountId, environmentCrnFilter);
            return filterAvailableStacks(stackService.getMultipleByEnvironmentCrnAndAccountId(environmentCrnFilter, accountId));
        }
    }

    private List<Stack> filterAvailableStacks(List<Stack> stacks) {
        return stacks.stream().filter(Stack::isAvailable).collect(Collectors.toList());
    }
}