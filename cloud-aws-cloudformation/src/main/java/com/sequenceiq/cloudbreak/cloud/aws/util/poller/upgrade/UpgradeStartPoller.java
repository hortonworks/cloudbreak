package com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.Poller;

@Component
public class UpgradeStartPoller {

    @Value("${cb.aws.rds.upgrade.start.wait.sleep:15}")
    private long sleepTimeSeconds;

    @Value("${cb.aws.rds.upgrade.start.wait.duration:5}")
    private long durationMinutes;

    @Inject
    private Poller<Boolean> poller;

    public void waitForUpgradeToStart(UpgradeStartWaitTask upgradeStartWaitTask) {
        poller.runPoller(sleepTimeSeconds, durationMinutes, upgradeStartWaitTask);
    }

}
