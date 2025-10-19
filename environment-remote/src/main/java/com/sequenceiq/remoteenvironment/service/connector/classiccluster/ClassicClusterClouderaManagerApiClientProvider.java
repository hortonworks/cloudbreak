package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider.API_V_51;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

@Component
class ClassicClusterClouderaManagerApiClientProvider {

    private static final String CLUSTER_PROXY_SERVICE_NAME = "cloudera-manager";

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public ApiClient getClouderaManagerRootClient(OnPremisesApiProto.Cluster cluster)  {
        try {
            return clouderaManagerApiClientProvider.getClouderaManagerRootClient(getHttpClientConfig(cluster), null, null, null);
        } catch (ClouderaManagerClientInitException e) {
            throw new OnPremCMApiException("Failed to initialize Cloudera Manager root client for " + cluster.getManagerUri(), e, 0);
        }
    }

    public ApiClient getClouderaManagerV51Client(OnPremisesApiProto.Cluster cluster)  {
        try {
            return clouderaManagerApiClientProvider.getClouderaManagerClient(getHttpClientConfig(cluster), null, null, null, API_V_51);
        } catch (ClouderaManagerClientInitException e) {
            throw new OnPremCMApiException("Failed to initialize Cloudera Manager v51 client for " + cluster.getManagerUri(), e, 0);
        }
    }

    private HttpClientConfig getHttpClientConfig(OnPremisesApiProto.Cluster cluster) {
        return new HttpClientConfig(cluster.getManagerUri())
                .withClusterProxy(clusterProxyConfiguration.getClusterProxyUrl(), cluster.getClusterCrn(), CLUSTER_PROXY_SERVICE_NAME);
    }

}
