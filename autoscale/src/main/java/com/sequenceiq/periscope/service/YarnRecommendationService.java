package com.sequenceiq.periscope.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
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
                .orElseThrow(NotFoundException.notFound("Cluster", clusterName)).getStackCrn();
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return clusterService.findOneByStackCrnAndTenant(resourceCrn, restRequestThreadLocalService.getCloudbreakTenant()).map(Cluster::getEnvironmentCrn);
    }

    public List<String> getRecommendationFromYarn(String clusterCrn) throws Exception {
        Cluster cluster = clusterService.findOneByStackCrnAndTenant(clusterCrn, restRequestThreadLocalService.getCloudbreakTenant()).get();
        LoadAlert loadAlert = cluster.getLoadAlerts().iterator().next();
        String policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();
        String pollingUserCrn = cluster.getClusterPertain().getUserCrn();

        StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(clusterCrn);

        YarnScalingServiceV1Response yarnResponse =
                yarnMetricsClient.getYarnMetricsForCluster(cluster, policyHostGroup,
                        pollingUserCrn, Optional.of(yarnConfig.getConnectionTimeOutMs()), Optional.of(yarnConfig.getReadTimeOutMs()));

        Map<String, String> hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup);

        List<String> yarnRecommendation = yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, hostFqdnsToInstanceId);

        LOGGER.info("yarnRecommendedDecommission={}, for Cluster= {}", yarnRecommendation, cluster.getStackName());

        return yarnRecommendation;
    }

}
