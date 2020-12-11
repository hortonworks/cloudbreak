package com.sequenceiq.it.cloudbreak;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.freeipa.api.client.FreeIpaApiKeyClient;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaWaitObject;

public class FreeIpaClient extends MicroserviceClient {
    public static final String FREEIPA_CLIENT = "FREEIPA_CLIENT";

    private static String crn;

    private com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient;

    FreeIpaClient(String newId) {
        super(newId);
    }

    FreeIpaClient() {
        this(FREEIPA_CLIENT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return freeIpaClient.getFlowPublicEndpoint();
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        return new WaitService<>();
    }

    @Override
    public <E extends Enum<E>, W extends WaitObject> W waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext) {
        FreeIpaTestDto freeIpaTestDto = (FreeIpaTestDto) entity;
        return (W) new FreeIpaWaitObject(this, entity.getName(), freeIpaTestDto.getResponse().getEnvironmentCrn(), (Status) desiredStatuses.get("status"));
    }

    public static Function<IntegrationTestContext, FreeIpaClient> getTestContextFreeIpaClient(String key) {
        return testContext -> testContext.getContextParam(key, FreeIpaClient.class);
    }

    public static Function<IntegrationTestContext, FreeIpaClient> getTestContextFreeIpaClient() {
        return getTestContextFreeIpaClient(FREEIPA_CLIENT);
    }

    public static synchronized FreeIpaClient createProxyFreeIpaClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        FreeIpaClient clientEntity = new FreeIpaClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.freeIpaClient = new FreeIpaApiKeyClient(
                testParameter.get(FreeIpaTest.FREEIPA_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public com.sequenceiq.freeipa.api.client.FreeIpaClient getFreeIpaClient() {
        return freeIpaClient;
    }

    public void setFreeIpaClient(com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient) {
        this.freeIpaClient = freeIpaClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(FreeIpaTestDto.class.getSimpleName(),
                LdapTestDto.class.getSimpleName(),
                FreeIpaChildEnvironmentTestDto.class.getSimpleName(),
                KerberosTestDto.class.getSimpleName());
    }
}
