package com.sequenceiq.periscope.monitor.handler;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy.STOPSTART;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.LimitsConfigurationResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.SecurityConfig;

@Service
public class CloudbreakCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakCommunicator.class);

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private RequestLogging requestLogging;

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public StackV4Response getByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().get(stackCrn);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public AutoscaleStackV4Response getAutoscaleClusterByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn()
                .autoscaleEndpoint().getAutoscaleClusterByCrn(stackCrn);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
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
        ScalingStrategy scalingStrategy = STOPSTART;
        requestLogging.logResponseTime(() -> {
            cloudbreakInternalCrnClient.withInternalCrn()
                    .autoscaleEndpoint().decommissionInternalInstancesForClusterCrn(cluster.getStackCrn(), decommissionNodeIds, false);
            return Optional.empty();
        }, String.format("DecommissionInstancesForCluster query for cluster crn %s, Scaling strategy %s, NodeIds %s",
                cluster.getStackCrn(), scalingStrategy, decommissionNodeIds));
    }

    public void stopInstancesForCluster(Cluster cluster, List<String> decommissionNodeIds) {
        ScalingStrategy scalingStrategy = STOPSTART;
        requestLogging.logResponseTime(() -> {
            cloudbreakInternalCrnClient.withInternalCrn()
                    .autoscaleEndpoint().stopInstancesForClusterCrn(cluster.getStackCrn(), decommissionNodeIds, false, scalingStrategy);
            return Optional.empty();
        }, String.format("StopInstancesForCluster query for cluster crn: %s, Scaling strategy: %s, NodeIds: %s", cluster.getStackCrn(),
                scalingStrategy, decommissionNodeIds));
    }

    public void putStackForCluster(Cluster cluster, UpdateStackV4Request updateStackJson) {
        requestLogging.logResponseTime(() -> {
            cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().putStack(cluster.getStackCrn(), cluster.getClusterPertain().getUserId(),
                    updateStackJson);
            return Optional.empty();
        }, String.format("PutStack query for cluster crn: %s, Scaling adjustment: %s, Host group: %s", cluster.getStackCrn(),
                updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment(), updateStackJson.getInstanceGroupAdjustment().getInstanceGroup()));
    }

    public void putStackStartInstancesForCluster(Cluster cluster, UpdateStackV4Request updateStackJson) {
        requestLogging.logResponseTime(() -> {
            cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().putStackStartInstancesByCrn(cluster.getStackCrn(), updateStackJson);
            return Optional.empty();
        }, String.format("PutStackStartInstances query for cluster crn: %s, Scaling adjustment: %s, Host group: %s", cluster.getStackCrn(),
                updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment(), updateStackJson.getInstanceGroupAdjustment().getInstanceGroup()));
    }

    public void deleteInstancesForCluster(Cluster cluster, List<String> instanceIds) {
        requestLogging.logResponseTime(() -> {
            MultipleInstanceDeleteRequest multiDeleteRequest = new MultipleInstanceDeleteRequest();
            multiDeleteRequest.setInstances(instanceIds);
            cloudbreakInternalCrnClient.withInternalCrn().distroXV1Endpoint().deleteInstancesByCrn(cluster.getStackCrn(), instanceIds, multiDeleteRequest,
                    false);
            return Optional.empty();
        }, String.format("DeleteInstancesForCluster query for cluster crn: %s, InstanceIds: %s", cluster.getStackCrn(), instanceIds));
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public ClusterProxyConfiguration getClusterProxyconfiguration() {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getClusterProxyconfiguration();
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public LimitsConfigurationResponse getLimitsConfiguration(String accountId) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getLimitsConfiguration(accountId);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public SecurityConfig getRemoteSecurityConfig(String stackCrn) {
        LOGGER.info("Looks like that SecurityConfig is not in database, calling Cloudbreak: {}", stackCrn);
        CertificateV4Response response = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getCertificate(stackCrn);
        LOGGER.info("We got a certificate back from Cloudbreak: {}", stackCrn);
        return new SecurityConfig(response.getClientKeyPath(), response.getClientCertPath(), response.getServerCert());
    }
}
