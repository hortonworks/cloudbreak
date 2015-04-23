package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;

@Service
public class AmbariHostsRemover {

    private static final int UNREGISTER_RETRY = 5;

    @Autowired
    private AmbariClientProvider ambariClientProvider;

    public void deleteHosts(Stack stack, List<String> hosts, List<String> components) {
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());
        for (String hostName : hosts) {
            if (stack.getCluster() != null) {
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
