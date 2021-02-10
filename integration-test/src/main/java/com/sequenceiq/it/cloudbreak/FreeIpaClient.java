package com.sequenceiq.it.cloudbreak;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.freeipa.api.client.FreeIpaApiKeyClient;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnClient;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnClientBuilder;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnEndpoint;
import com.sequenceiq.freeipa.api.client.FreeipaInternalCrnClient;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncStatusDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaUsedImagesTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaOperationWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaUserSyncWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaWaitObject;

public class FreeIpaClient extends MicroserviceClient<com.sequenceiq.freeipa.api.client.FreeIpaClient, FreeIpaApiUserCrnEndpoint> {
    public static final String FREEIPA_CLIENT = "FREEIPA_CLIENT";

    private com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient;

    private FreeipaInternalCrnClient freeipaInternalCrnClient;

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
    public <E extends Enum<E>, W extends WaitObject> W waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext) {
        if (entity instanceof FreeIpaUserSyncTestDto) {
            FreeIpaUserSyncTestDto freeIpaSyncTestDto = (FreeIpaUserSyncTestDto) entity;
            if (freeIpaSyncTestDto.getOperationId() == null) {
                return (W) new FreeIpaUserSyncWaitObject(this, freeIpaSyncTestDto.getName(),
                        freeIpaSyncTestDto.getEnvironmentCrn(), (UserSyncState) desiredStatuses.get("status"));
            } else {
                return (W) new FreeIpaOperationWaitObject(this, freeIpaSyncTestDto.getOperationId(), freeIpaSyncTestDto.getName(),
                        freeIpaSyncTestDto.getEnvironmentCrn(), (OperationState) desiredStatuses.get("status"));
            }
        } else {
            FreeIpaTestDto freeIpaTestDto = (FreeIpaTestDto) entity;
            return (W) new FreeIpaWaitObject(this, entity.getName(), freeIpaTestDto.getResponse().getEnvironmentCrn(), (Status) desiredStatuses.get("status"));
        }
    }

    @Override
    public com.sequenceiq.freeipa.api.client.FreeIpaClient getDefaultClient() {
        return freeIpaClient;
    }

    public static Function<IntegrationTestContext, FreeIpaClient> getTestContextFreeIpaClient(String key) {
        return testContext -> testContext.getContextParam(key, FreeIpaClient.class);
    }

    public static synchronized FreeIpaClient createProxyFreeIpaClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        FreeIpaClient clientEntity = new FreeIpaClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.freeIpaClient = new FreeIpaApiKeyClient(testParameter.get(FreeIpaTest.FREEIPA_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.freeipaInternalCrnClient = createFreeipaInternalClient(testParameter.get(FreeIpaTest.FREEIPA_SERVER_INTERNAL_ROOT));
        return clientEntity;
    }

    public static synchronized FreeipaInternalCrnClient createFreeipaInternalClient(String serverRoot) {
        FreeIpaApiUserCrnClient freeIpaApiUserCrnClient = new FreeIpaApiUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new FreeipaInternalCrnClient(freeIpaApiUserCrnClient, new InternalCrnBuilder(Crn.Service.IAM));
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(FreeIpaTestDto.class.getSimpleName(),
                FreeIpaUserSyncTestDto.class.getSimpleName(),
                LdapTestDto.class.getSimpleName(),
                FreeIpaChildEnvironmentTestDto.class.getSimpleName(),
                KerberosTestDto.class.getSimpleName(),
                FreeIpaUserSyncStatusDto.class.getSimpleName(),
                FreeipaUsedImagesTestDto.class.getSimpleName());
    }

    @Override
    public FreeIpaApiUserCrnEndpoint getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        return freeipaInternalCrnClient.withInternalCrn();
    }
}
