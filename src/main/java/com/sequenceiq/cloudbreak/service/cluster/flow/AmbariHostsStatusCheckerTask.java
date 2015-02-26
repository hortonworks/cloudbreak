package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;

@Component
public class AmbariHostsStatusCheckerTask extends StackBasedStatusCheckerTask<AmbariHosts> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHosts t) {
        MDCBuilder.buildMdcContext(t.getStack());
        Map<String, String> hostNames = t.getAmbariClient().getHostNamesByState("HEALTHY");
        int hostsFound = hostNames.size();
        LOGGER.info("Ambari client found {} hosts ({} needed). [Stack: '{}']", hostsFound, t.getHostCount(), t.getStack().getId());
        if (hostsFound >= t.getHostCount()) {
            return true;
        }
        return false;
    }

    @Override
    public void handleTimeout(AmbariHosts t) {
        throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s'",
                t.getHostCount(), t.getStack().getId()));
    }

    @Override
    public String successMessage(AmbariHosts t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

}
