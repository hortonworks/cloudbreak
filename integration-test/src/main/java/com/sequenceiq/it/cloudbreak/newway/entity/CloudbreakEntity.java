package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public interface CloudbreakEntity {

    Logger LOGGER = LoggerFactory.getLogger(CloudbreakEntity.class);

    CloudbreakEntity valid();

    String getName();

    default void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.warn("Did not clean up resource: {}", getName());
    }

    default CloudbreakEntity refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.warn("It is not refreshable resource: {}", getName());
        return this;
    }

    default CloudbreakEntity wait(Map<String, String> desiredStatuses, RunningParameter runningParameter) {
        LOGGER.warn("Did not wait: {}", getName());
        return this;
    }
}
