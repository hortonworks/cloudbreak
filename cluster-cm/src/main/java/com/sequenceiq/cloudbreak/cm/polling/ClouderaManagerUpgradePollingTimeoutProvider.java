package com.sequenceiq.cloudbreak.cm.polling;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Component
public class ClouderaManagerUpgradePollingTimeoutProvider {

    private static final long DEFAULT_MOCK_TIMEOUT = TimeUnit.MINUTES.toSeconds(1);

    private static final long POLL_FOR_ONE_HOUR = TimeUnit.HOURS.toSeconds(1);

    private static final long POLL_FOR_TWO_HOURS = TimeUnit.HOURS.toSeconds(2);

    private static final int ROLLING_UPGRADE_BASE_TIMEOUT_MIN = 30;

    private static final long ROLLING_UPGRADE_TIMEOUT_PER_NODE_MIN = 5L;

    public long getParcelDownloadTimeout(String cloudPlatform) {
        if (CloudPlatform.MOCK.equals(CloudPlatform.valueOf(cloudPlatform))) {
            return DEFAULT_MOCK_TIMEOUT;
        }
        return POLL_FOR_TWO_HOURS;
    }

    public long getParcelDistributeTimeout(String cloudPlatform) {
        if (CloudPlatform.MOCK.equals(CloudPlatform.valueOf(cloudPlatform))) {
            return DEFAULT_MOCK_TIMEOUT;
        }
        return POLL_FOR_TWO_HOURS;
    }

    public long getCdhUpgradeTimeout(StackDtoDelegate stack, boolean rollingUpgrade) {
        if (CloudPlatform.MOCK.equals(CloudPlatform.valueOf(stack.getCloudPlatform()))) {
            return DEFAULT_MOCK_TIMEOUT;
        }
        return rollingUpgrade ? getRollingUpgradeTimeout(stack) : POLL_FOR_ONE_HOUR;
    }

    private long getRollingUpgradeTimeout(StackDtoDelegate stack) {
        int numberOfInstances = stack.getAllAvailableInstances().size();
        return TimeUnit.MINUTES.toSeconds(ROLLING_UPGRADE_BASE_TIMEOUT_MIN + numberOfInstances * ROLLING_UPGRADE_TIMEOUT_PER_NODE_MIN);
    }
}
