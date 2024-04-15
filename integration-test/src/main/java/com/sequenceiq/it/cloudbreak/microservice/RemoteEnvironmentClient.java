package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.RemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentCrnEndpoint;

public class RemoteEnvironmentClient<E extends Enum<E>, W extends WaitObject>
        extends MicroserviceClient<com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient, Void, E, W> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentClient.class);

    private final CloudbreakUser cloudbreakUser;

    private com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient client;

    public RemoteEnvironmentClient(CloudbreakUser cloudbreakUser, String address,  RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        setActing(cloudbreakUser);
        client = new com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient(address, configKey(),
                "/api", regionAwareInternalCrnGenerator);
        this.cloudbreakUser = cloudbreakUser;
    }

    private ConfigKey configKey() {
        return new ConfigKey(false, true, true, TIMEOUT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        LOGGER.info("Flow does not support by remote environment client");
        return null;
    }

    @Override
    public com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient getDefaultClient() {
        return client;
    }

    public RemoteEnvironmentCrnEndpoint getEndpoint() {
        return client.withCrn(cloudbreakUser.getCrn());
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                RemoteEnvironmentTestDto.class.getSimpleName());
    }
}
