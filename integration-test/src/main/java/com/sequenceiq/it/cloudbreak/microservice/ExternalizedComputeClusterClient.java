package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.externalizedcompute.api.ExternalizedComputeClusterApi;
import com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterApiKeyClient;
import com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterCrnEndpoint;
import com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterInternalCrnClient;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.externalizedcompute.ExternalizedComputeClusterWaitObject;

public class ExternalizedComputeClusterClient extends MicroserviceClient<com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterClient,
        ExternalizedComputeClusterCrnEndpoint, ExternalizedComputeClusterApiStatus, ExternalizedComputeClusterWaitObject> {

    private final com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterClient externalizedComputeClusterClient;

    private final ExternalizedComputeClusterInternalCrnClient externalizedComputeClusterInternalCrnClient;

    public ExternalizedComputeClusterClient(CloudbreakUser cloudbreakUser, RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator,
            String serviceAddress, String serviceInternalAddress) {
        ConfigKey configKey = new ConfigKey(false, true, true);
        externalizedComputeClusterClient = new ExternalizedComputeClusterApiKeyClient(serviceAddress, configKey)
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());

        externalizedComputeClusterInternalCrnClient = new ExternalizedComputeClusterInternalCrnClient(serviceInternalAddress, configKey,
                ExternalizedComputeClusterApi.API_ROOT_CONTEXT, regionAwareInternalCrnGenerator);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        // TODO: Flow checking works based on resource crn and flowId/flowChainId. In case of flowChainId it won't work because
        //  we return the environmentCrn in the ExternalizedComputeClusterTestDto.getCrn method.
        return externalizedComputeClusterClient.getFlowPublicEndpoint();
    }

    @Override
    public com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterClient getDefaultClient() {
        return externalizedComputeClusterClient;
    }

    @Override
    public ExternalizedComputeClusterWaitObject waitObject(CloudbreakTestDto entity, String name,
            Map<String, ExternalizedComputeClusterApiStatus> desiredStatuses, TestContext testContext,
            Set<ExternalizedComputeClusterApiStatus> ignoredFailedStatuses) {
        ExternalizedComputeClusterResponse externalizedComputeCluster = ((ExternalizedComputeClusterTestDto) entity).getResponse();
        return new ExternalizedComputeClusterWaitObject(this, externalizedComputeCluster.getEnvironmentCrn(),
                externalizedComputeCluster.getName(), desiredStatuses.get("status"));
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(ExternalizedComputeClusterTestDto.class.getSimpleName());
    }

    @Override
    public ExternalizedComputeClusterCrnEndpoint getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        return externalizedComputeClusterInternalCrnClient.withInternalCrn();
    }
}
