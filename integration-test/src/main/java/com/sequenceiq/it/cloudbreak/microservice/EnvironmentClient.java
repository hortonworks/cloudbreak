package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyClient;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClientBuilder;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.EnvironmentTest;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.environment.EnvironmentWaitObject;
import com.sequenceiq.it.util.TestParameter;

public class EnvironmentClient extends MicroserviceClient<com.sequenceiq.environment.client.EnvironmentClient, EnvironmentServiceCrnEndpoints,
        EnvironmentStatus, EnvironmentWaitObject> {

    private com.sequenceiq.environment.client.EnvironmentClient environmentClient;

    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return environmentClient.flowPublicEndpoint();
    }

    @Override
    public EnvironmentWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, EnvironmentStatus> desiredStatuses,
            TestContext testContext, Set<EnvironmentStatus> ignoredFailedStatuses) {
        return new EnvironmentWaitObject(this, entity.getName(), entity.getCrn(), desiredStatuses.get("status"), ignoredFailedStatuses);
    }

    public static synchronized EnvironmentClient createEnvironmentClient(TestParameter testParameter, CloudbreakUser cloudbreakUser,
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        EnvironmentClient clientEntity = new EnvironmentClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.environmentClient = new EnvironmentServiceApiKeyClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.environmentInternalCrnClient = createInternalEnvironmentClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_INTERNAL_SERVER_ROOT),
                regionAwareInternalCrnGenerator);
        return clientEntity;
    }

    public static synchronized EnvironmentInternalCrnClient createInternalEnvironmentClient(String serverRoot,
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        EnvironmentServiceUserCrnClient userCrnClient = new EnvironmentServiceUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new EnvironmentInternalCrnClient(userCrnClient, regionAwareInternalCrnGenerator);
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(EnvironmentTestDto.class.getSimpleName(),
                EnvironmentClient.class.getSimpleName(),
                ProxyTestDto.class.getSimpleName(),
                CredentialTestDto.class.getSimpleName(),
                TermsPolicyDto.class.getSimpleName());
    }

    @Override
    public com.sequenceiq.environment.client.EnvironmentClient getDefaultClient() {
        return environmentClient;
    }

    @Override
    public EnvironmentServiceCrnEndpoints getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        return environmentInternalCrnClient.withInternalCrn();
    }
}
