package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientRetryer;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariHostsUnavailableException;

@Component
public class AmbariHostsStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsStatusCheckerTask.class);

    @Inject
    private AmbariClientRetryer ambariClientRetryer;

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext t) {
        Map<String, String> hostNamesWithStatus = ambariClientRetryer.getHostStatuses(t.getAmbariClient());
        int totalNodes = hostNamesWithStatus.size();
        LOGGER.info("Ambari client found {} hosts ({} needed). [Stack: '{}'] | Known hosts with status: [{}]",
                totalNodes, t.getHostsInCluster().size(), t.getStack().getId(), hostNamesWithStatus);
        return totalNodes >= t.getHostsInCluster().size();
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext t) {
        Map<String, String> hostNamesWithStatus = ambariClientRetryer.getHostStatuses(t.getAmbariClient());
        throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s' "
                        + "| Known hosts with status: [%s]",
                t.getHostsInCluster().size(), t.getStack().getId(), hostNamesWithStatus));
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostsInCluster().size(), t.getStack().getId());
    }

}
