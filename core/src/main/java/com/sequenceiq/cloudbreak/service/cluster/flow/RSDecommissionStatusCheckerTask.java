package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class RSDecommissionStatusCheckerTask extends StackBasedStatusCheckerTask<AmbariHostsWithNames> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSDecommissionStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsWithNames t) {
        MDCBuilder.buildMdcContext(t.getStack());
        AmbariClient ambariClient = t.getAmbariClient();
        Map<String, Long> rs = ambariClient.getHBaseRegionServersWithData(t.getHostNames());
        for (Map.Entry<String, Long> entry : rs.entrySet()) {
            LOGGER.info("RegionServer: {} decommission is in progress, {} storefiles left", entry.getKey(), entry.getValue());
        }
        return rs.isEmpty();
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
