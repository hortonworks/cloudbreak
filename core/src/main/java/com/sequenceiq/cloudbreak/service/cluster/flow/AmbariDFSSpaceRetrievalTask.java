package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariDFSSpaceRetrievalTask extends ClusterBasedStatusCheckerTask<AmbariClientPollerObject> {
    public static final int AMBARI_RETRYING_INTERVAL = 5000;
    public static final int AMBARI_RETRYING_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDFSSpaceRetrievalTask.class);
    private Map<String, Map<Long, Long>> dfsSpace;

    @Override
    public boolean checkStatus(AmbariClientPollerObject ambariClientPollerObject) {
        try {
            dfsSpace = ambariClientPollerObject.getAmbariClient().getDFSSpace();
            return true;
        } catch (Exception ex) {
            LOGGER.warn("Error during getting dfs space from ambari", ex);
            return false;
        }
    }

    @Override
    public void handleTimeout(AmbariClientPollerObject ambariClientPollerObject) {
    }

    @Override
    public String successMessage(AmbariClientPollerObject ambariClientPollerObject) {
        return "Dfs space successfully get from ambari.";
    }

    public Map<String, Map<Long, Long>> getDfsSpace() {
        return dfsSpace;
    }
}
