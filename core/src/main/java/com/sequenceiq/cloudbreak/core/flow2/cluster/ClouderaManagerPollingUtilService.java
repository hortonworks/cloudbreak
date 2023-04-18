package com.sequenceiq.cloudbreak.core.flow2.cluster;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;

@Service
public class ClouderaManagerPollingUtilService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerPollingUtilService.class);

    private static final int MAX_READ_COUNT = 15;

    private static final int SLEEP_INTERVAL = 10;

    public void pollClouderaManagerServices(ClusterApi clusterApi, String service, String status) throws Exception {
        LOGGER.debug("Starting polling on CM Service {} to check if {}", service, status);
        Polling.waitPeriodly(SLEEP_INTERVAL, TimeUnit.SECONDS).stopIfException(false).stopAfterAttempt(MAX_READ_COUNT)
                .run(() -> {
                    LOGGER.debug("Polling CM Service {} to check if {}", service, status);
                    Map<String, String> readResults = clusterApi.clusterModificationService().fetchServiceStatuses();
                    if (status.equals(readResults.get(service.toLowerCase()))) {
                        return AttemptResults.justFinish();
                    }
                    return AttemptResults.justContinue();
                });
    }
}
