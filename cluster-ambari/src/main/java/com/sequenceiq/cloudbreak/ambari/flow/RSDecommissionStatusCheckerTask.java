package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class RSDecommissionStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsWithNames> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSDecommissionStatusCheckerTask.class);

    private static final String FINAL_STATE = "INSTALLED";

    @Override
    public boolean checkStatus(AmbariHostsWithNames t) {
        MDCBuilder.buildMdcContext(t.getStack());
        AmbariClient ambariClient = t.getAmbariClient();
        Map<String, String> rs = ambariClient.getHBaseRegionServersState(t.getHostNames());
        for (Entry<String, String> entry : rs.entrySet()) {
            if (!FINAL_STATE.equals(entry.getValue())) {
                LOGGER.debug("RegionServer: {} decommission is in progress, current state: {}", entry.getKey(), entry.getValue());
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(AmbariHostsWithNames t) {
        throw new IllegalStateException("RegionServer decommission timed out");
    }

    @Override
    public String successMessage(AmbariHostsWithNames t) {
        return "Requested RegionServer decommission operations completed";
    }

}
