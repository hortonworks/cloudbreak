package com.sequenceiq.periscope.monitor.client;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.periscope.domain.MetricType.YARN_FORBIDDEN_EXCEPTION;

import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.service.sslcontext.SSLContextProvider;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.config.YarnConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.InstanceConfig;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request.HostGroupInstanceType;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component
public class YarnMetricsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnMetricsClient.class);

    private static final String YARN_API_CLUSTER_PROXY_URL = "%s/proxy/%s/resourcemanager/v1/cluster/scaling";

    private static final String YARN_API_QUERY_PARAM_MOCK_CLOUD_RECOMMEND_ONLY = "actionType=verify";

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
    private PeriscopeMetricService metricService;

    @Inject
    private Clock clock;

    @Inject
    private YarnServiceConfigClient yarnServiceConfigClient;

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Value("${periscope.yarn.mock.cluster.url}")
    private String yarnMockClusterUrl;

    @Inject
    private YarnConfig yarnConfig;

    @Inject
    private SSLContextProvider sslContextProvider;

    @Retryable(value = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 5000))
    public YarnScalingServiceV1Response getYarnMetricsForCluster(Cluster cluster, StackV4Response stackV4Response,
            String hostGroup, String pollingUserCrn, Optional<Integer> maxDecommissionNodeCount) throws Exception {
        TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
        SSLContext sslContext = sslContextProvider.getSSLContext(tlsConfig.getServerCert(), Optional.empty(),
                tlsConfig.getClientCert(), tlsConfig.getClientKey());
        Client restClient = RestClientUtil.createClient(sslContext, true);
        long start = clock.getCurrentTimeMillis();
        InstanceConfig instanceConfig = null;
        if (CloudPlatform.valueOf(cluster.getCloudPlatform()) == CloudPlatform.MOCK) {
            instanceConfig = defaultInstanceConfig();
        } else {
            instanceConfig = yarnServiceConfigClient.getInstanceConfigFromCM(cluster, stackV4Response, hostGroup);
        }
        metricService.recordClusterManagerInvocation(cluster, start);

        YarnScalingServiceV1Request yarnScalingServiceV1Request = new YarnScalingServiceV1Request();
        yarnScalingServiceV1Request.setInstanceTypes(List.of(
                new HostGroupInstanceType(instanceConfig.getInstanceName(),
                        instanceConfig.getMemoryInMb().intValue(), instanceConfig.getCoreCPU())));

        LOGGER.debug("Using actorCrn '{}' for Cluster '{}' yarn polling.", pollingUserCrn, cluster.getStackCrn());

        String yarnApiUrl = getYarnApiUrl(cluster);

        UriBuilder yarnMetricsURI = UriBuilder.fromPath(yarnApiUrl)
                .queryParam(PARAM_UPSCALE_FACTOR_NODE_RESOURCE_TYPE, DEFAULT_UPSCALE_RESOURCE_TYPE);

        maxDecommissionNodeCount.ifPresent(
                scaleDownCount -> yarnMetricsURI.queryParam(PARAM_DOWNSCALE_FACTOR_IN_NODE_COUNT, stackV4Response.getNodeCount()));

        YarnScalingServiceV1Response yarnResponse =
                invokeYarnAPIWithExceptionHandling(restClient, yarnMetricsURI, pollingUserCrn, yarnScalingServiceV1Request, cluster);

        LOGGER.info("YarnScalingAPI response for cluster crn '{}',  response '{}'", cluster.getStackCrn(), yarnResponse);
        return yarnResponse;
    }

    @Retryable(value = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 5000))
    public YarnScalingServiceV1Response getYarnMetricsForCluster(Cluster cluster,
            String hostGroup, String pollingUserCrn, Optional<Integer> connectionTimeout, Optional<Integer> readTimeout) throws Exception {
        TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
        SSLContext sslContext = sslContextProvider.getSSLContext(tlsConfig.getServerCert(), Optional.empty(),
                tlsConfig.getClientCert(), tlsConfig.getClientKey());
        Client restClient = RestClientUtil.createClient(sslContext, connectionTimeout.get(), readTimeout.get(), true);
        String yarnApiUrl = getYarnApiUrl(cluster);
        long start = clock.getCurrentTimeMillis();
        InstanceConfig instanceConfig = defaultInstanceConfig();
        metricService.recordClusterManagerInvocation(cluster, start);

        YarnScalingServiceV1Request yarnScalingServiceV1Request = new YarnScalingServiceV1Request();
        yarnScalingServiceV1Request.setInstanceTypes(List.of(
                new HostGroupInstanceType(instanceConfig.getInstanceName(),
                        instanceConfig.getMemoryInMb().intValue(), instanceConfig.getCoreCPU())));

        LOGGER.debug("Using actorCrn '{}' for Cluster '{}' yarn polling.", pollingUserCrn, cluster.getStackCrn());

        UriBuilder yarnMetricsURI = UriBuilder.fromPath(yarnApiUrl);

        if (CloudPlatform.valueOf(cluster.getCloudPlatform()) == CloudPlatform.MOCK) {
            LOGGER.info("Add query parameter {} for Mock cloud", YARN_API_QUERY_PARAM_MOCK_CLOUD_RECOMMEND_ONLY);
            String [] queryParam = YARN_API_QUERY_PARAM_MOCK_CLOUD_RECOMMEND_ONLY.split("=");
            yarnMetricsURI = yarnMetricsURI.queryParam(queryParam[0], queryParam[1]);
        }

        YarnScalingServiceV1Response yarnResponse;
        try {
            yarnResponse = invokeYarnAPIWithExceptionHandling(restClient, yarnMetricsURI, pollingUserCrn, yarnScalingServiceV1Request, cluster);
        } catch (Exception ex) {
            throw new ServiceUnavailableException("Currently yarn is not available please try again in some time");
        }

        LOGGER.info("YarnScalingAPI response for cluster crn '{}',  response '{}'", cluster.getStackCrn(), yarnResponse);
        return yarnResponse;
    }

    private YarnScalingServiceV1Response invokeYarnAPIWithExceptionHandling(Client restClient, UriBuilder yarnMetricsURI, String pollingUserCrn,
            YarnScalingServiceV1Request yarnScalingServiceV1Request, Cluster cluster) {
        try {
            return requestLogging.logResponseTime(
                    () -> restClient.target(yarnMetricsURI).request()
                            .accept(MediaType.APPLICATION_JSON_VALUE)
                            .header(ACTOR_CRN_HEADER, pollingUserCrn)
                            .post(Entity.json(yarnScalingServiceV1Request), YarnScalingServiceV1Response.class),
                    String.format("YarnScalingAPI query for cluster crn '%s'", cluster.getStackCrn()));
        } catch (Exception ex) {
            String message = String.format("Encountered exception when invoking YarnScalingAPI for cluster: %s", cluster.getStackCrn());
            LOGGER.error(message, ex);
            if (ex instanceof ForbiddenException) {
                metricService.incrementMetricCounter(YARN_FORBIDDEN_EXCEPTION,
                        MetricTag.CLOUD_PROVIDER.name(), Optional.ofNullable(cluster.getCloudPlatform()).orElse("NA"));
            }
            throw ex;
        }
    }

    public String getYarnApiUrl(Cluster cluster) {
        String yarnApiUrl = null;
        if (tlsHttpClientConfigurationService.isClusterProxyApplicable(cluster.getCloudPlatform())) {
            String clusterProxyUrl = clusterProxyConfigurationService.getClusterProxyUrl()
                    .orElseThrow(() -> new RuntimeException(String.format("ClusterProxy Not Configured for Cluster %s, " +
                            " cannot query YARN Metrics.", cluster.getStackCrn())));
            yarnApiUrl = String.format(YARN_API_CLUSTER_PROXY_URL, clusterProxyUrl, cluster.getStackCrn());
        } else if (CloudPlatform.valueOf(cluster.getCloudPlatform()) == CloudPlatform.MOCK) {
            yarnApiUrl = String.format(yarnMockClusterUrl, cluster.getStackCrn());
        } else {
            throw new RuntimeException(String.format("Endpoint for Yarn Metrics is not configured for %s, " +
                    " cannot query YARN Metrics.", cluster.getStackCrn()));
        }
        LOGGER.info(String.format("Yarn Api Url for %s is %s", cluster.getStackName(), yarnApiUrl));
        return yarnApiUrl;
    }

    private InstanceConfig defaultInstanceConfig() {
        InstanceConfig instanceConfig = new InstanceConfig("trial");
        instanceConfig.setCoreCPU(yarnConfig.getCpuCores());
        instanceConfig.setMemoryInMb(yarnConfig.getMemoryInMb());
        return instanceConfig;
    }
}
