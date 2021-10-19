package com.sequenceiq.periscope.monitor.client;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.InstanceConfig;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request.HostGroupInstanceType;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component
public class YarnMetricsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnMetricsClient.class);

    private static final String YARN_API_URL = "%s/proxy/%s/resourcemanager/v1/cluster/scaling";

    private static final String HEADER_ACTOR_CRN = "x-cdp-actor-crn";

    private static final String PARAM_UPSCALE_FACTOR_NODE_RESOURCE_TYPE = "upscaling-factor-in-node-resource-types";

    private static final String PARAM_DOWNSCALE_FACTOR_IN_NODE_COUNT = "downscaling-factor-in-node-count";

    private static final String DEFAULT_UPSCALE_RESOURCE_TYPE = "memory-mb";

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Inject
    private RequestLogging requestLogging;

    @Inject
    private YarnServiceConfigClient yarnServiceConfigClient;

    @Retryable(value = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 5000))
    public YarnScalingServiceV1Response getYarnMetricsForCluster(Cluster cluster, StackV4Response stackV4Response,
            String hostGroup, Optional<Integer> mandatoryDownScaleCount) throws Exception {
        TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
        String clusterProxyUrl = clusterProxyConfigurationService.getClusterProxyUrl()
                .orElseThrow(() -> new RuntimeException(String.format("ClusterProxy Not Configured for Cluster %s, " +
                        " cannot query YARN Metrics.", cluster.getStackCrn())));

        Client restClient = RestClientUtil.createClient(tlsConfig.getServerCert(),
                tlsConfig.getClientCert(), tlsConfig.getClientKey(), true);
        String yarnApiUrl = String.format(YARN_API_URL, clusterProxyUrl, cluster.getStackCrn());

        InstanceConfig instanceConfig = yarnServiceConfigClient.getInstanceConfigFromCM(cluster, stackV4Response, hostGroup);
        YarnScalingServiceV1Request yarnScalingServiceV1Request = new YarnScalingServiceV1Request();
        yarnScalingServiceV1Request.setInstanceTypes(List.of(
                new HostGroupInstanceType(instanceConfig.getInstanceName(),
                        instanceConfig.getMemoryInMb().intValue(), instanceConfig.getCoreCPU())));

        String pollingUserCrn = Optional.ofNullable(cluster.getMachineUserCrn()).orElse(cluster.getClusterPertain().getUserCrn());
        LOGGER.info("Using actorCrn '{}' for Cluster '{}' yarn polling.", pollingUserCrn, cluster.getStackCrn());

        UriBuilder yarnMetricsURI = UriBuilder.fromPath(yarnApiUrl)
                .queryParam(PARAM_UPSCALE_FACTOR_NODE_RESOURCE_TYPE, DEFAULT_UPSCALE_RESOURCE_TYPE);

        mandatoryDownScaleCount.ifPresent(
                scaleDownCount -> yarnMetricsURI.queryParam(PARAM_DOWNSCALE_FACTOR_IN_NODE_COUNT, stackV4Response.getNodeCount()));

        YarnScalingServiceV1Response yarnResponse = requestLogging.logResponseTime(
                () -> restClient.target(yarnMetricsURI).request()
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .header(HEADER_ACTOR_CRN, pollingUserCrn)
                        .post(Entity.json(yarnScalingServiceV1Request), YarnScalingServiceV1Response.class),
                String.format("YarnScalingAPI query for cluster crn '%s'", cluster.getStackCrn()));

        LOGGER.info("YarnScalingAPI response for cluster crn '{}',  response '{}'", cluster.getStackCrn(), yarnResponse);
        return yarnResponse;
    }
}