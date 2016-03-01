package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public abstract class StackBasedStatusCheckerTask<T extends StackContext> extends SimpleStatusCheckerTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBasedStatusCheckerTask.class);

    @Inject
    private StackRepository stackRepository;

    public boolean exitPolling(T t) {
        try {
            Stack stack = stackRepository.findByIdLazy(t.getStack().getId());
            if (stack == null || stack.isDeleteInProgress() || stack.isDeleteCompleted()) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("Error occurred when check status checker exit criteria: ", ex);
            return true;
        }
    }

}
