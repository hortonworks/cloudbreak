package com.sequenceiq.freeipa.service.freeipa.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

public class FreeIpaAdminUserReplicatedPoller implements AttemptMaker<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAdminUserReplicatedPoller.class);

    private final Stack stack;

    private FreeIpaClientFactory freeIpaClientFactory;

    private int attempt;

    public FreeIpaAdminUserReplicatedPoller(Stack stack, FreeIpaClientFactory freeIpaClientFactory) {
        this.stack = stack;
        this.freeIpaClientFactory = freeIpaClientFactory;
    }

    @Override
    public AttemptResult<Void> process() throws Exception {
        attempt++;
        LOGGER.debug("Checking if admin user is replicated on all instances. Attempt: [{}]", attempt);
        try {
            freeIpaClientFactory.createClientForAllInstances(stack);
            LOGGER.info("We were able to create FreeIpa client for all instances, so admin user password is replicated on them");
            return AttemptResults.justFinish();
        } catch (FreeIpaClientException e) {
            LOGGER.debug("We were unable to create FreeIpa client for all instances", e);
            return AttemptResults.continueFor(e);
        }
    }
}
