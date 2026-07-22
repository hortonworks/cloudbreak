package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.DescribeRemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.ListRemoteEnvironmentsTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentServiceApiKeyClient;

public class RemoteEnvironmentClient<E extends Enum<E>, W extends WaitObject>
        extends MicroserviceClient<com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentClient, Void, E, W> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentClient.class);

    private final CloudbreakUser cloudbreakUser;

    private com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentClient client;

    private com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentClient alternativeClient;

    public RemoteEnvironmentClient(CloudbreakUser cloudbreakUser, String address, String alternativeAddress) {
        setActing(cloudbreakUser);
        this.cloudbreakUser = cloudbreakUser;
        client = createRemoteEnvironmentClient(cloudbreakUser, address);

        if (isNotEmpty(alternativeAddress)) {
            alternativeClient = createRemoteEnvironmentClient(cloudbreakUser, alternativeAddress);
        }
    }

    private com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentClient createRemoteEnvironmentClient(CloudbreakUser cloudbreakUser, String address) {
        return new RemoteEnvironmentServiceApiKeyClient(address, configKey())
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
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
    public com.sequenceiq.remoteenvironment.api.client.RemoteEnvironmentClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeClient;
        } else {
            return client;
        }
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                ListRemoteEnvironmentsTestDto.class.getSimpleName(),
                DescribeRemoteEnvironmentTestDto.class.getSimpleName());
    }
}
