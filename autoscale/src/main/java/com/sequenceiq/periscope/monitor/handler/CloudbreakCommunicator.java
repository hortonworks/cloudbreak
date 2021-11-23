package com.sequenceiq.periscope.monitor.handler;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.LimitsConfigurationResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private RequestLogging requestLogging;

    public StackV4Response getByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().get(stackCrn);
    }

    public AutoscaleStackV4Response getAutoscaleClusterByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn()
                .autoscaleEndpoint().getAutoscaleClusterByCrn(stackCrn);
    }

    public AutoscaleStackV4Response getAutoscaleClusterByName(String stackName, String accountId) {
        return cloudbreakInternalCrnClient.withInternalCrn()
                .autoscaleEndpoint().getInternalAutoscaleClusterByName(stackName, accountId);
    }

    public AutoscaleRecommendationV4Response getRecommendationForCluster(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getRecommendation(stackCrn);
    }

    public StackStatusV4Response getStackStatusByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(stackCrn);
    }

    public void decommissionInstancesForCluster(Cluster cluster, List<String> decommissionNodeIds) {
        requestLogging.logResponseTime(() -> {
            cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint()
                    .decommissionInternalInstancesForClusterCrnV2(cluster.getStackCrn(), decommissionNodeIds, false, true);
            // TODO CB-14929: Make Periscope scaling mechanism aware
            return Optional.empty();
        }, String.format("DecommissionInstancesForCluster query for cluster crn %s, NodeIds %s",
                cluster.getStackCrn(), decommissionNodeIds));
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public ClusterProxyConfiguration getClusterProxyconfiguration() {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getClusterProxyconfiguration();
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public LimitsConfigurationResponse getLimitsConfiguration() {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getLimitsConfiguration();
    }
}
