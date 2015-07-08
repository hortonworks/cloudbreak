package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;

@Component
public class AmbariHostsStatusCheckerTask extends StackBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext t) {
        Map<String, String> healthyHostNames = t.getAmbariClient().getHostNamesByState("HEALTHY");
        Map<String, String> unHealthyHostNames = t.getAmbariClient().getHostNamesByState("UNHEALTHY");
        int totalNodes = healthyHostNames.size() + unHealthyHostNames.size();
        LOGGER.info("Ambari client found {} hosts ({} needed). [Stack: '{}']", totalNodes, t.getHostCount(), t.getStack().getId());
        if (totalNodes >= t.getHostCount()) {
            return true;
        }
        return false;
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext t) {
        throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s'",
                t.getHostCount(), t.getStack().getId()));
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

}
