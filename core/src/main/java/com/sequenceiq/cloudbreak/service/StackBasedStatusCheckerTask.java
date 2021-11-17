package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class StackBasedStatusCheckerTask<T extends StackContext> extends SimpleStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackBasedStatusCheckerTask.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean exitPolling(T t) {
        try {
            return !stackService.stackExistsAndNotDeleting(t.getStack().getId());
        } catch (Exception ex) {
            LOGGER.debug("Error occurred when check status checker exit criteria: ", ex);
            return true;
        }
    }

}
