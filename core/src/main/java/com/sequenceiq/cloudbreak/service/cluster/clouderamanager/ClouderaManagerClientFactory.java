package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Service
public class ClouderaManagerClientFactory {

    @Inject
    private ClouderaManagerClientProvider clouderaManagerClientProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public ApiClient getDefaultClient(Stack stack) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return clouderaManagerClientProvider.getClouderaManagerClient(httpClientConfig, stack.getGatewayPort(), "admin", "admin");
    }

    public ApiClient getClient(Stack stack, Cluster cluster) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return clouderaManagerClientProvider.getClouderaManagerClient(httpClientConfig,
                stack.getGatewayPort(), cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword());
    }

    public ApiClient getClient(Stack stack, String username, String password) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getAmbariIp());
        return clouderaManagerClientProvider.getClouderaManagerClient(httpClientConfig, stack.getGatewayPort(), username, password);
    }
}
