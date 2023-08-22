package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.periscope.config.YarnConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Service
public class YarnRecommendationService implements AuthorizationResourceCrnProvider, AuthorizationEnvironmentCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnRecommendationService.class);

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private YarnResponseUtils yarnResponseUtils;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private YarnConfig yarnConfig;

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATAHUB;
    }

    @Override
    @Retryable(value = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public String getResourceCrnByResourceName(String clusterName) {
        return clusterService.findOneByStackNameAndTenant(clusterName, restRequestThreadLocalService.getCloudbreakTenant())
                .orElseGet(() -> fetchClusterFromCBUsingName(clusterName)).getStackCrn();
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return clusterService.findOneByStackCrnAndTenant(resourceCrn, restRequestThreadLocalService.getCloudbreakTenant()).map(Cluster::getEnvironmentCrn);
    }

    protected Cluster fetchClusterFromCBUsingName(String stackName) {
        String accountId = restRequestThreadLocalService.getCloudbreakTenant();
        return Optional.ofNullable(cloudbreakCommunicator.getAutoscaleClusterByName(stackName, accountId))
                .filter(stack -> WORKLOAD.equals(stack.getStackType()))
                .map(stack -> clusterService.create(stack))
                .orElseThrow(NotFoundException.notFound("cluster", stackName));
    }

    public List<String> getRecommendationFromYarn(String clusterCrn) throws Exception {
        Cluster cluster = clusterService.findOneByStackCrnAndTenant(clusterCrn, restRequestThreadLocalService.getCloudbreakTenant()).get();
        LoadAlert loadAlert = cluster.getLoadAlerts().iterator().next();
        String policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();
        String pollingUserCrn = cluster.getClusterPertain().getUserCrn();

        YarnScalingServiceV1Response yarnResponse =
                yarnMetricsClient.getYarnMetricsForCluster(cluster, policyHostGroup,
                        pollingUserCrn, Optional.of(yarnConfig.getConnectionTimeOutMs()), Optional.of(yarnConfig.getReadTimeOutMs()));

        List<String> yarnRecommendation = yarnResponse.getScaleDownCandidates().orElse(List.of()).stream()
                .map(YarnScalingServiceV1Response.DecommissionCandidate::getNodeId)
                .filter(i -> i.contains(policyHostGroup)).collect(Collectors.toList());

        LOGGER.info("yarnRecommendedDecommission={}, for Cluster= {}", yarnRecommendation, cluster.getStackName());

        return yarnRecommendation;
    }

}
