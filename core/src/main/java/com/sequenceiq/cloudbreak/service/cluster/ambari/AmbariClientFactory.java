package com.sequenceiq.cloudbreak.service.cluster.ambari;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class AmbariClientFactory {

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackService stackService;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

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

    public AmbariClient getAmbariClient(DatalakeResources datalakeResources, Credential credential) {
        if (datalakeResources.getDatalakeStackId() != null) {
            Stack datalakeStack = stackService.getById(datalakeResources.getDatalakeStackId());
            return getAmbariClient(datalakeStack, datalakeStack.getCluster());
        } else if (credentialPrerequisiteService.isCumulusCredential(credential.getAttributes())) {
            return credentialPrerequisiteService.createCumulusAmbariClient(credential.getAttributes());
        } else {
            throw new CloudbreakServiceException("Can not create Ambari Clientas there is no Datalake Stack and the credential is not for Cumulus");
        }
    }
}
