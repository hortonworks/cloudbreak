package com.sequenceiq.environment.environment.service.datahub;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DatahubUpgradeCcmPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubUpgradeCcmPollerService.class);

    @Value("${env.upgradeccm.datahub.polling.attempt:45}")
    private Integer attempt;

    @Value("${env.upgradeccm.datahub.polling.sleep.time:20}")
    private Integer sleeptime;

    private final DatahubPollerProvider pollerProvider;

    private final MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    public DatahubUpgradeCcmPollerService(DatahubPollerProvider pollerProvider, MultipleFlowsResultEvaluator multipleFlowsResultEvaluator) {
        this.pollerProvider = pollerProvider;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
    }

    public void waitForUpgradeOnFlowIds(Long envId, List<FlowIdentifier> upgradeFlows) {
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(pollerProvider.multipleFlowsPoller(envId, upgradeFlows));
        } catch (PollerStoppedException e) {
            LOGGER.info("Data Hubs Upgrade CCM timed out or error happened.", e);
            throw new DatahubOperationFailedException("Data Hub upgrade of Cluster Connectivity Manager timed out or error happened: " + e.getMessage());
        }
        if (multipleFlowsResultEvaluator.anyFailed(upgradeFlows)) {
            throw new DatahubOperationFailedException(
                    "Data Hub upgrade of Cluster Connectivity Manager error happened. One or more Data Hubs are not upgraded.");
        }
    }
}
