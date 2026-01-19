package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CertificateSwapTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsWaitObject;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.client.RedbeamsApiKeyClient;
import com.sequenceiq.redbeams.client.RedbeamsApiKeyEndpoints;

public class RedbeamsClient extends MicroserviceClient<com.sequenceiq.redbeams.client.RedbeamsClient, Void, Status, RedbeamsWaitObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsClient.class);

    private com.sequenceiq.redbeams.client.RedbeamsClient redbeamsClient;

    private com.sequenceiq.redbeams.client.RedbeamsClient alternativeRedbeamsClient;

    public RedbeamsClient(CloudbreakUser cloudbreakUser, String redbeamsAddress, String alternativeRedbeamsAddress) {
        setActing(cloudbreakUser);
        redbeamsClient = createRedbeamsClient(cloudbreakUser, redbeamsAddress);
        if (isNotEmpty(alternativeRedbeamsAddress)) {
            alternativeRedbeamsClient = createRedbeamsClient(cloudbreakUser, alternativeRedbeamsAddress);
        }
    }

    private RedbeamsApiKeyEndpoints createRedbeamsClient(CloudbreakUser cloudbreakUser, String redbeamsAddress) {
        return new RedbeamsApiKeyClient(
                redbeamsAddress,
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        LOGGER.info("Flow does not support by rdbms client");
        return null;
    }

    @Override
    public RedbeamsWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, Status> desiredStatuses,
            TestContext testContext, Set<Status> ignoredFailedStatuses) {
        return new RedbeamsWaitObject(this, entity.getCrn(), desiredStatuses.get("status"), ignoredFailedStatuses, testContext);
    }

    @Override
    public com.sequenceiq.redbeams.client.RedbeamsClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeRedbeamsClient;
        } else {
            return redbeamsClient;
        }
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                RedbeamsDatabaseServerTestDto.class.getSimpleName(),
                RedbeamsDatabaseTestDto.class.getSimpleName(),
                CertificateSwapTestDto.class.getSimpleName());
    }
}
