package com.sequenceiq.environment.environment.service.sdx;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.exception.SdxOperationFailedException;

@Service
public class SdxUpgradeCcmPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeCcmPollerService.class);

    @Value("${env.upgradeccm.datalake.polling.attempt:45}")
    private Integer attempt;

    @Value("${env.upgradeccm.datalake.polling.sleep.time:20}")
    private Integer sleeptime;

    private final SdxPollerProvider pollerProvider;

    public SdxUpgradeCcmPollerService(SdxPollerProvider pollerProvider) {
        this.pollerProvider = pollerProvider;
    }

    public void waitForUpgradeCcm(Long envId, String datalakeCrn) {
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(() -> pollerProvider.upgradeCcmPoller(envId, datalakeCrn));
        } catch (PollerStoppedException e) {
            LOGGER.info("Data Lake Upgrade CCM timed out or error happened.", e);
            throw new SdxOperationFailedException("Data Lake upgrade of Cluster Connectivity Manager timed out or error happened: " + e.getMessage());
        }
    }
}
