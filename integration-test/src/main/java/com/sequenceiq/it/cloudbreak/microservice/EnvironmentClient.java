package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyEndpoints;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClientBuilder;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTrustSetupDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.environment.EnvironmentWaitObject;

public class EnvironmentClient extends MicroserviceClient<com.sequenceiq.environment.client.EnvironmentClient, EnvironmentServiceCrnEndpoints,
        EnvironmentStatus, EnvironmentWaitObject> {

    private com.sequenceiq.environment.client.EnvironmentClient environmentClient;

    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    private com.sequenceiq.environment.client.EnvironmentClient alternativeEnvironmentClient;

    private EnvironmentInternalCrnClient alternativeEnvironmentInternalCrnClient;

    public EnvironmentClient(CloudbreakUser cloudbreakUser,
            RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator, String environmentAddress, String environmentInternalAddress,
            String alternativeEnvironmentAddress, String alternativeEnvironmentInternalAddress) {
        setActing(cloudbreakUser);
        environmentClient = createEnvironmentClient(cloudbreakUser, environmentAddress);
        environmentInternalCrnClient = createInternalEnvironmentClient(environmentInternalAddress, regionAwareInternalCrnGenerator);

        if (isNotEmpty(alternativeEnvironmentAddress) && isNotEmpty(alternativeEnvironmentInternalAddress)) {
            alternativeEnvironmentClient = createEnvironmentClient(cloudbreakUser, alternativeEnvironmentAddress);
            alternativeEnvironmentInternalCrnClient = createInternalEnvironmentClient(alternativeEnvironmentInternalAddress, regionAwareInternalCrnGenerator);
        }
    }

    private EnvironmentServiceApiKeyEndpoints createEnvironmentClient(CloudbreakUser cloudbreakUser, String environmentAddress) {
        return new EnvironmentServiceApiKeyClient(
                environmentAddress,
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeEnvironmentClient.flowPublicEndpoint();
        } else {
            return environmentClient.flowPublicEndpoint();
        }
    }

    @Override
    public EnvironmentWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, EnvironmentStatus> desiredStatuses,
            TestContext testContext, Set<EnvironmentStatus> ignoredFailedStatuses) {
        return new EnvironmentWaitObject(this, entity.getName(), entity.getCrn(), desiredStatuses.get("status"), ignoredFailedStatuses,
                testContext);
    }

    public EnvironmentInternalCrnClient createInternalEnvironmentClient(String serverRoot,
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
                TermsPolicyDto.class.getSimpleName(),
                EnvironmentTrustSetupDto.class.getSimpleName());
    }

    @Override
    public com.sequenceiq.environment.client.EnvironmentClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeEnvironmentClient;
        } else {
            return environmentClient;
        }
    }

    @Override
    public EnvironmentServiceCrnEndpoints getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        EnvironmentInternalCrnClient client;
        if (testContext.shouldUseAlternativeEndpoints()) {
            client = alternativeEnvironmentInternalCrnClient;
        } else {
            client = environmentInternalCrnClient;
        }
        return client.withInternalCrn();
    }
}
