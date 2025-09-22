package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

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
            throw new RemoteEnvironmentException("Failed to initialize Cloudera Manager root client for " + cluster.getManagerUri(), e);
        }
    }

    private HttpClientConfig getHttpClientConfig(OnPremisesApiProto.Cluster cluster) {
        return new HttpClientConfig(cluster.getManagerUri())
                .withClusterProxy(clusterProxyConfiguration.getClusterProxyUrl(), cluster.getClusterCrn(), CLUSTER_PROXY_SERVICE_NAME);
    }

}
