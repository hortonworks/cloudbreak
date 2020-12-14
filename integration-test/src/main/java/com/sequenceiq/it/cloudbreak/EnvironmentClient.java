package com.sequenceiq.it.cloudbreak;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClientBuilder;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.environment.EnvironmentWaitObject;

public class EnvironmentClient extends MicroserviceClient {

    public static final String ENVIRONMENT_CLIENT = "ENVIRONMENT_CLIENT";

    private com.sequenceiq.environment.client.EnvironmentClient environmentClient;

    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    EnvironmentClient(String newId) {
        super(newId);
    }

    EnvironmentClient() {
        this(ENVIRONMENT_CLIENT);
    }

    public static Function<IntegrationTestContext, EnvironmentClient> getTestContextEnvironmentClient(String key) {
        return testContext -> testContext.getContextParam(key, EnvironmentClient.class);
    }

    public static Function<IntegrationTestContext, EnvironmentClient> getTestContextEnvironmentClient() {
        return getTestContextEnvironmentClient(ENVIRONMENT_CLIENT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return environmentClient.flowPublicEndpoint();
    }

    @Override
    public <E extends Enum<E>, T extends WaitObject> T waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext) {
        return (T) new EnvironmentWaitObject(this, entity.getName(), entity.getCrn(), (EnvironmentStatus) desiredStatuses.get("status"));
    }

    public static synchronized EnvironmentClient createProxyEnvironmentClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        EnvironmentClient clientEntity = new EnvironmentClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.environmentClient = new EnvironmentServiceApiKeyClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.environmentInternalCrnClient = createInternalEnvironmentClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_INTERNAL_SERVER_ROOT));
        return clientEntity;
    }

    public static synchronized EnvironmentInternalCrnClient createInternalEnvironmentClient(String serverRoot) {
        EnvironmentServiceUserCrnClient userCrnClient = new EnvironmentServiceUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new EnvironmentInternalCrnClient(userCrnClient, new InternalCrnBuilder(Crn.Service.IAM));
    }

    public com.sequenceiq.environment.client.EnvironmentClient getEnvironmentClient() {
        return environmentClient;
    }

    public void setEnvironmentClient(com.sequenceiq.environment.client.EnvironmentClient environmentClient) {
        this.environmentClient = environmentClient;
    }

    public EnvironmentInternalCrnClient getEnvironmentInternalCrnClient() {
        return environmentInternalCrnClient;
    }

    public void setEnvironmentInternalCrnClient(EnvironmentInternalCrnClient environmentInternalCrnClient) {
        this.environmentInternalCrnClient = environmentInternalCrnClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(EnvironmentTestDto.class.getSimpleName(),
                EnvironmentClient.class.getSimpleName(),
                ProxyTestDto.class.getSimpleName(),
                CredentialTestDto.class.getSimpleName());
    }
}
