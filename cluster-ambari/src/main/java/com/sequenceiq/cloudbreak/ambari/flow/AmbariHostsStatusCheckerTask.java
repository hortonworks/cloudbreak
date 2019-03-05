package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ambari.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariHostsStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext t) {
        Map<String, String> hostNamesWithStatus = t.getAmbariClient().getHostStatuses();
        int totalNodes = hostNamesWithStatus.size();
        LOGGER.debug("Ambari client found {} hosts ({} needed). [Stack: '{}'] | Known hosts with status: [{}]",
                totalNodes, t.getHostsInCluster().size(), t.getStack().getId(), hostNamesWithStatus);
        return totalNodes >= t.getHostsInCluster().size();
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext t) {
        Map<String, String> hostNamesWithStatus = t.getAmbariClient().getHostStatuses();
        throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s' "
                        + "| Known hosts with status: [%s]",
                t.getHostsInCluster().size(), t.getStack().getId(), hostNamesWithStatus));
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostsInCluster().size(), t.getStack().getId());
    }

}
