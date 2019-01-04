package com.sequenceiq.cloudbreak.service.cluster.ambari;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Component
public class AmbariClientFactory {

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public AmbariClient getDefaultAmbariClient(Stack stack) {
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return ambariClientProvider.getDefaultAmbariClient(clientConfig, stack.getGatewayPort());
    }

    public AmbariClient getAmbariClient(Stack stack, Cluster cluster) {
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
    }

    public AmbariClient getAmbariClient(Stack stack, String username, String password) {
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), username, password);
    }
}
