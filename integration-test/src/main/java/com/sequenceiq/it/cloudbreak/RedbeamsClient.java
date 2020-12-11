package com.sequenceiq.it.cloudbreak;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsWaitObject;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.client.RedbeamsApiKeyClient;

public class RedbeamsClient extends MicroserviceClient {
    public static final String REDBEAMS_CLIENT = "REDBEAMS_CLIENT";

    private com.sequenceiq.redbeams.client.RedbeamsClient endpoints;

    private String environmentCrn;

    private RedbeamsClient() {
        super(REDBEAMS_CLIENT);
    }

    public static synchronized RedbeamsClient createProxyRedbeamsClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        RedbeamsClient clientEntity = new RedbeamsClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.endpoints = new RedbeamsApiKeyClient(
                testParameter.get(RedBeamsTest.REDBEAMS_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public com.sequenceiq.redbeams.client.RedbeamsClient getEndpoints() {
        return endpoints;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow does not support on rdbms client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        return null;
    }

    @Override
    public <E extends Enum<E>, W extends WaitObject> W waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext) {
        return (W) new RedbeamsWaitObject(this, entity.getCrn(), (Status) desiredStatuses.get("status"));
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(RedbeamsDatabaseServerTestDto.class.getSimpleName(),
                RedbeamsDatabaseTestDto.class.getSimpleName());
    }
}
