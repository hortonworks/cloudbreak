package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService.BEAN_POST_TAG;

import jakarta.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
public class ClusterApiConnectors {
    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public ClusterApi getConnector(StackDtoDelegate stack) {
        return getConnector(stack, stack.getClusterManagerIp());
    }

    public ClusterApi getConnector(StackDtoDelegate stack, String clusterManagerIp) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(),
                clusterManagerIp, stack.getCloudPlatform());
        return (ClusterApi) applicationContext.getBean(stack.getCluster().getVariant(), stack, httpClientConfig);
    }

    public ClusterPreCreationApi getConnector(ClusterView cluster) {
        return applicationContext.getBean(cluster.getVariant() + BEAN_POST_TAG, ClusterPreCreationApi.class);
    }

}
