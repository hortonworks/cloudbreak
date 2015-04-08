package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;

@Service
public class AmbariHostsRemover {

    @Autowired
    private AmbariClientProvider ambariClientProvider;

    public void deleteHosts(Stack stack, List<String> hosts, List<String> components) {
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());

        for (String hostName : hosts) {
            if (stack.getCluster() != null) {
                ambariClient.deleteHostComponents(hostName, components);
                ambariClient.deleteHost(hostName);
            }
            ambariClient.unregisterHost(hostName);
        }
    }
}
