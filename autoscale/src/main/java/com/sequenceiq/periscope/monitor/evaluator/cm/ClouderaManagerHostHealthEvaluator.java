package com.sequenceiq.periscope.monitor.evaluator.cm;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerSpecificHostHealthEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component("ClouderaManagerHostHealthEvaluator")
@Scope("prototype")
public class ClouderaManagerHostHealthEvaluator implements ClusterManagerSpecificHostHealthEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHostHealthEvaluator.class);

    private static final String CM_HOST_HEARTBEAT = "Cloudera Manager Host Heartbeat";

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private SecretService secretService;

    @Override
    public ClusterManagerVariant getSupportedClusterManagerVariant() {
        return ClusterManagerVariant.CLOUDERA_MANAGER;
    }

    @Override
    public List<String> determineHostnamesToRecover(Cluster cluster) {
        long start = System.currentTimeMillis();
        Long clusterId = cluster.getId();
        try {
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Checking '{}' alerts for cluster {}.", CM_HOST_HEARTBEAT, clusterId);
            HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(cluster.getStackCrn(),
                    cluster.getClusterManager().getHost(), cluster.getTunnel());
            ClusterManager cm = cluster.getClusterManager();
            String user = secretService.get(cm.getUser());
            String pass = secretService.get(cm.getPass());
            ApiClient client = clouderaManagerApiClientProvider.getClient(Integer.valueOf(cm.getPort()), user, pass, httpClientConfig);
            HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
            return hostsResourceApi.readHosts(null, null, DataView.FULL.name()).getItems()
                    .stream()
                    .filter(isAlertStateMet())
                    .peek(apiHost -> {
                        String currentState = apiHost.getHealthSummary().getValue();
                        String hostName = apiHost.getHostname();
                        LOGGER.debug("Alert: {} is in '{}' state for host '{}'.", CM_HOST_HEARTBEAT, currentState, hostName);
                    })
                    .map(ApiHost::getHostname)
                    .peek(hn -> LOGGER.debug("Host to recover: {}", hn))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.info("Failed to retrieve '{}' alerts. Original message: {}", CM_HOST_HEARTBEAT, e.getMessage());
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
            throw new RuntimeException(e);
        } finally {
            LOGGER.debug("Finished {} for cluster {} in {} ms", CM_HOST_HEARTBEAT, clusterId, System.currentTimeMillis() - start);
        }
    }

    private Predicate<ApiHost> isAlertStateMet() {
        return apiHost -> Optional.ofNullable(apiHost)
                .map(ApiHost::getHealthSummary)
                .map(ApiHealthSummary::getValue)
                .map(ApiHealthSummary.BAD.name()::equalsIgnoreCase)
                .orElse(Boolean.FALSE);
    }

}
