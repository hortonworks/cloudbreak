package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.RedBeamsTest;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CertificateSwapTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsWaitObject;
import com.sequenceiq.it.util.TestParameter;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.client.RedbeamsApiKeyClient;

public class RedbeamsClient extends MicroserviceClient<com.sequenceiq.redbeams.client.RedbeamsClient, Void, Status, RedbeamsWaitObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsClient.class);

    private com.sequenceiq.redbeams.client.RedbeamsClient redbeamsClient;

    public static synchronized RedbeamsClient createRedbeamsClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        RedbeamsClient clientEntity = new RedbeamsClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.redbeamsClient = new RedbeamsApiKeyClient(
                testParameter.get(RedBeamsTest.REDBEAMS_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        LOGGER.info("Flow does not support by rdbms client");
        return null;
    }

    @Override
    public RedbeamsWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, Status> desiredStatuses,
            TestContext testContext, Set<Status> ignoredFailedStatuses) {
        return new RedbeamsWaitObject(this, entity.getCrn(), desiredStatuses.get("status"), ignoredFailedStatuses);
    }

    @Override
    public com.sequenceiq.redbeams.client.RedbeamsClient getDefaultClient() {
        return redbeamsClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                RedbeamsDatabaseServerTestDto.class.getSimpleName(),
                RedbeamsDatabaseTestDto.class.getSimpleName(),
                CertificateSwapTestDto.class.getSimpleName());
    }
}
