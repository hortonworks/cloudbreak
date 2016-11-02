package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;

@Component
public class RSDecommissionStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsWithNames> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSDecommissionStatusCheckerTask.class);

    private static final String FINAL_STATE = "INSTALLED";

    @Override
    public boolean checkStatus(AmbariHostsWithNames t) {
        MDCBuilder.buildMdcContext(t.getStack());
        AmbariClient ambariClient = t.getAmbariClient();
        Map<String, String> rs = ambariClient.getHBaseRegionServersState(t.getHostNames());
        for (Map.Entry<String, String> entry : rs.entrySet()) {
            if (!FINAL_STATE.equals(entry.getValue())) {
                LOGGER.info("RegionServer: {} decommission is in progress, current state: {}", entry.getKey(), entry.getValue());
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
