package com.sequenceiq.cloudbreak.ambari.flow;


import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AmbariComponenstJoinStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariComponenstJoinStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext ambariHostsCheckerContext) {
        try {
            AmbariClient ambariClient = ambariHostsCheckerContext.getAmbariClient();
            Map<String, Map<String, String>> hostComponentsStates = ambariClient.getHostComponentsStates();
            return areAllComponentsJoined(hostComponentsStates);
        } catch (Exception e) {
            LOGGER.info("Not components joined yet. Continuing polling.", e);
            return false;
        }
    }

    private boolean areAllComponentsJoined(Map<String, Map<String, String>> hostComponentsStates) {
        return hostComponentsStates.entrySet().stream().allMatch(
                host -> host.getValue().values().stream().noneMatch("UNKNOWN"::equalsIgnoreCase));
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext ambariHostsCheckerContext) {
        LOGGER.error("Operation timed out. Failed to wait for all Ambari components to join. Stack: '{}' cannot be started.",
                ambariHostsCheckerContext.getStack().getId());
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("All Ambari components joined and Stack '%s' is ready to be started.", t.getStack().getId());
    }
}
