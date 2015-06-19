package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

@Service
public class AmbariHostsRemover {

    private static final int UNREGISTER_RETRY = 5;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void deleteHosts(Stack stack, List<String> hosts, List<String> components) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        for (String hostName : hosts) {
            if (cluster != null) {
                ambariClient.deleteHostComponents(hostName, components);
                ambariClient.deleteHost(hostName);
            }
            unregisterHost(ambariClient, hostName, UNREGISTER_RETRY);
        }
    }

    private void unregisterHost(AmbariClient ambariClient, String host, int retry) {
        try {
            ambariClient.unregisterHost(host);
        } catch (Exception e) {
            if (--retry > -1) {
                unregisterHost(ambariClient, host, retry);
            } else {
                throw new AmbariOperationFailedException("Failed to unregister certain hosts from Ambari", e);
            }
        }
    }
}
