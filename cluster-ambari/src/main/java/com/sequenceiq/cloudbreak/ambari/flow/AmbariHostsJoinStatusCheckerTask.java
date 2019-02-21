package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariHostsJoinStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsJoinStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext hosts) {
        try {
            AmbariClient ambariClient = hosts.getAmbariClient();
            Map<String, String> hostNames = ambariClient.getHostStatuses();
            for (HostMetadata hostMetadata : hosts.getHostsInCluster()) {
                boolean contains = false;
                for (Entry<String, String> hostName : hostNames.entrySet()) {
                    if (hostName.getKey().equals(hostMetadata.getHostName()) && !"UNKNOWN".equals(hostName.getValue())) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    LOGGER.debug("The host {} currently not part of the cluster, waiting for join", hostMetadata.getHostName());
                    return false;
                }
            }
        } catch (Exception ignored) {
            LOGGER.debug("Did not join all hosts yet, polling");
            return false;
        }
        return true;
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext t) {
        LOGGER.info("Operation timed out. Failed to find all '{}' Ambari hosts. Stack: '{}'", t.getHostCount(), t.getStack().getId());
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

}
