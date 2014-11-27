package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

public class DNDecommissionStatusCheckerTask implements StatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DNDecommissionStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariOperations t) {
        MDCBuilder.buildMdcContext(t.getStack());
        AmbariClient ambariClient = t.getAmbariClient();
        Map<String, Long> dataNodes = ambariClient.getDecommissioningDataNodes();
        boolean finished = dataNodes.isEmpty();
        if (!finished) {
            LOGGER.info("DataNode decommission is in progress: {}", dataNodes);
        }
        return finished;
    }

    @Override
    public void handleTimeout(AmbariOperations t) {
        throw new IllegalStateException("DataNode decommission timed out");

    }

    @Override
    public String successMessage(AmbariOperations t) {
        return "Requested DataNode decommission operations completed";
    }

}
