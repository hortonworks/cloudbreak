package com.sequenceiq.it.cloudbreak.dto;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.Orderable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public interface CloudbreakTestDto extends Orderable {

    Logger LOGGER = LoggerFactory.getLogger(CloudbreakTestDto.class);

    String getLastKnownFlowChainId();

    String getLastKnownFlowId();

    CloudbreakTestDto valid();

    String getName();

    CloudPlatform getCloudPlatform();

    default void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.warn("Did not clean up resource ({}): name={}", getClass().getSimpleName(), getName());
    }

    default CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.warn("It is not refreshable resource: {}", getName());
        return this;
    }

    default CloudbreakTestDto wait(Map<String, Status> desiredStatus, RunningParameter runningParameter) {
        LOGGER.warn("Did not wait: {}", getName());
        return this;
    }

    default void setCloudPlatform(CloudPlatform cloudPlatform) {
        LOGGER.warn("Did not set up cloud platform ({}): name={}", getClass().getSimpleName(), getName());
    }
}
