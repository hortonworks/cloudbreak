package com.sequenceiq.periscope.monitor.evaluator.cm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerException;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;

@Component("ClouderaManagerTotalHostsEvaluator")
@Scope("prototype")
public class ClouderaManagerTotalHostsEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTotalHostsEvaluator.class);

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Inject
    private SecretService secretService;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public int getTotalHosts(Cluster cluster) {
        try {
            Long clusterId = cluster.getId();
            LOGGER.debug("Checking number of total hosts for cluster {}.", clusterId);
            HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(cluster.getStackCrn(),
                    cluster.getClusterManager().getHost(), cluster.getTunnel());
            ClusterManager cm = cluster.getClusterManager();
            String user = secretService.get(cm.getUser());
            String pass = secretService.get(cm.getPass());
            ApiClient client = clouderaManagerApiClientProvider.getClient(Integer.valueOf(cm.getPort()), user, pass, httpClientConfig);
            HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
            return hostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()).getItems().size();
        } catch (Exception e) {
            LOGGER.info("Failed to retrieve number of total hosts. Original message: {}", e.getMessage());
            throw new ClusterManagerException("Failed to retrieve number of total hosts", e);
        }
    }
}
