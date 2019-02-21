package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariHostsLeaveStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsWithNames> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsLeaveStatusCheckerTask.class);

    private static final String LEFT_STATE = "UNKNOWN";

    @Override
    public boolean checkStatus(AmbariHostsWithNames hosts) {
        try {
            AmbariClient ambariClient = hosts.getAmbariClient();
            List<String> hostNames = hosts.getHostNames();
            Map<String, String> hostStatuses = ambariClient.getHostStatuses();
            for (String hostName : hostNames) {
                String status = hostStatuses.get(hostName);
                if (!LEFT_STATE.equals(status)) {
                    LOGGER.debug("{} didn't leave the cluster yet", hostName);
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.info("Failed to check the left hosts", e);
            return false;
        }
        return true;
    }

    @Override
    public void handleTimeout(AmbariHostsWithNames t) {
        LOGGER.info("Operation timed out. Hosts didn't leave in time, hosts: '{}' stack: '{}'", t.getHostNames(), t.getStack().getId());
    }

    @Override
    public String successMessage(AmbariHostsWithNames t) {
        return String.format("Hosts left the cluster, hosts: '%s' stack '%s'", t.getHostNames(), t.getStack().getId());
    }

}
