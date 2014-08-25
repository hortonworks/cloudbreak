package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;

public class AmbariHostsStatusCheckerTask implements StatusCheckerTask<AmbariHosts> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHosts t) {
        Map<String, String> hostNames = t.getAmbariClient().getHostNames();
        int hostsFound = hostNames.size();
        LOGGER.info("Ambari client found {} hosts ({} needed). [Stack: '{}']", hostsFound, t.getHostCount(), t.getStackId());
        if (hostsFound == t.getHostCount()) {
            return true;
        }
        return false;
    }

    @Override
    public void handleTimeout(AmbariHosts t) {
        throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s'",
                t.getHostCount(), t.getStackId()));
    }

    @Override
    public String successMessage(AmbariHosts t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStackId());
    }

}
