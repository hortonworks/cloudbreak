package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class AmbariHostsJoinStatusCheckerTask extends StackBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsJoinStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext hosts) {
        try {
            AmbariClient ambariClient = hosts.getAmbariClient();
            Map<String, String> hostNames = ambariClient.getHostStatuses();
            for (HostMetadata hostMetadata : hosts.getHostsInCluster()) {
                boolean contains = false;
                for (Map.Entry<String, String> hostName : hostNames.entrySet()) {
                    if (hostName.getKey().equals(hostMetadata.getHostName()) && !"UNKNOWN".equals(hostName.getValue())) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    LOGGER.info("The host {} currently not part of the cluster, waiting for join", hostMetadata.getHostName());
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.info("Did not join all hosts yet, polling");
            return false;
        }
        return true;
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext t) {
        LOGGER.error("Operation timed out. Failed to find all '{}' Ambari hosts. Stack: '{}'", t.getHostCount(), t.getStack().getId());
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

}
