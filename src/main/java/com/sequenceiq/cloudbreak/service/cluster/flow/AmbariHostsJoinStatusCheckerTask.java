package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;

@Component
@Scope("prototype")
public class AmbariHostsJoinStatusCheckerTask implements StatusCheckerTask<AmbariHostsPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsJoinStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsPollerObject hosts) {
        MDCBuilder.buildMdcContext(hosts.getStack());
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
    public void handleTimeout(AmbariHostsPollerObject t) {
        throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s'",
                t.getHostCount(), t.getStack().getId()));
    }

    @Override
    public String successMessage(AmbariHostsPollerObject t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

    @Override
    public void handleExit(AmbariHostsPollerObject ambariHostsPollerObject) {
        return;
    }

}
