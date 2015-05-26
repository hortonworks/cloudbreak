package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class AmbariHostsJoinStatusCheckerTask extends StackBasedStatusCheckerTask<AmbariHosts> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsJoinStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHosts hosts) {
        try {
            AmbariClient ambariClient = hosts.getAmbariClient();
            List<String> hostNames = ambariClient.getClusterHosts();
            for (String hostName : hostNames) {
                if ("UNKNOWN".equals(ambariClient.getHostState(hostName))) {
                    LOGGER.info("The state of the {} is UNKNOWN, waiting for join", hostName);
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
    public void handleTimeout(AmbariHosts t) {
        LOGGER.error("Operation timed out. Failed to find all '{}' Ambari hosts. Stack: '{}'", t.getHostCount(), t.getStack().getId());
    }

    @Override
    public String successMessage(AmbariHosts t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

}
