package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterApiKeyClient;
import com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterCrnEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.externalizedcompute.ExternalizedComputeClusterWaitObject;

public class ExternalizedComputeClusterClient extends MicroserviceClient<com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterClient,
        ExternalizedComputeClusterCrnEndpoint, ExternalizedComputeClusterApiStatus, ExternalizedComputeClusterWaitObject> {

    private final com.sequenceiq.externalizedcompute.api.client.ExternalizedComputeClusterClient externalizedComputeClusterClient;

    public ExternalizedComputeClusterClient(CloudbreakUser cloudbreakUser, String serviceAddress) {
        ConfigKey configKey = new ConfigKey(false, true, true);
        externalizedComputeClusterClient = new ExternalizedComputeClusterApiKeyClient(serviceAddress, configKey)
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
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
        return new ExternalizedComputeClusterWaitObject(this, ((ExternalizedComputeClusterTestDto) entity).getResponse().getName(),
                desiredStatuses.get("status"));
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(ExternalizedComputeClusterTestDto.class.getSimpleName());
    }
}
