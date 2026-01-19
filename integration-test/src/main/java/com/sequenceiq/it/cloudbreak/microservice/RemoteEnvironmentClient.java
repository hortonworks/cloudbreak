package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.RemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentCrnEndpoint;
import com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient;

public class RemoteEnvironmentClient<E extends Enum<E>, W extends WaitObject>
        extends MicroserviceClient<com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient, Void, E, W> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentClient.class);

    private final CloudbreakUser cloudbreakUser;

    private com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient client;

    private com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient alternativeClient;

    public RemoteEnvironmentClient(CloudbreakUser cloudbreakUser, String address,  RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator,
            String alternativeAddress) {
        setActing(cloudbreakUser);
        this.cloudbreakUser = cloudbreakUser;
        client = createRemoteEnvironmentClient(address, regionAwareInternalCrnGenerator);

        if (isNotEmpty(alternativeAddress)) {
            alternativeClient = createRemoteEnvironmentClient(alternativeAddress, regionAwareInternalCrnGenerator);
        }
    }

    private RemoteEnvironmentInternalCrnClient createRemoteEnvironmentClient(String address, RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        return new RemoteEnvironmentInternalCrnClient(address, configKey(),
                "/api", regionAwareInternalCrnGenerator);
    }

    private ConfigKey configKey() {
        return new ConfigKey(false, true, true, TIMEOUT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        LOGGER.info("Flow does not support by remote environment client");
        return null;
    }

    @Override
    public com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentInternalCrnClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeClient;
        } else {
            return client;
        }
    }

    public RemoteEnvironmentCrnEndpoint getEndpoint(TestContext testContext) {
        return getDefaultClient(testContext).withCrn(cloudbreakUser.getCrn());
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                RemoteEnvironmentTestDto.class.getSimpleName());
    }
}
