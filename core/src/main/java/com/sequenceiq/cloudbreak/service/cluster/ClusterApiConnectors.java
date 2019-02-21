package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService.BEAN_POST_TAG;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Service
public class ClusterApiConnectors {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public ClusterApi getConnector(Stack stack) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return (ClusterApi) applicationContext.getBean(stack.getCluster().getVariant(), stack, httpClientConfig);
    }

    public ClusterPreCreationApi getConnector(Cluster cluster) {
        return applicationContext.getBean(cluster.getVariant() + BEAN_POST_TAG, ClusterPreCreationApi.class);
    }
}
