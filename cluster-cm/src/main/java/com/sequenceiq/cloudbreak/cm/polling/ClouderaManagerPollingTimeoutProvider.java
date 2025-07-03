package com.sequenceiq.cloudbreak.cm.polling;

import java.util.concurrent.TimeUnit;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class ClouderaManagerPollingTimeoutProvider {

    private static final long POLL_FOR_15_MINUTES = TimeUnit.MINUTES.toSeconds(15);

    private static final long DEFAULT_MOCK_TIMEOUT = TimeUnit.MINUTES.toSeconds(1);

    private static final long POLL_FOR_ONE_HOUR = TimeUnit.HOURS.toSeconds(1);

    private static final long POLL_FOR_THREE_HOURS = TimeUnit.HOURS.toSeconds(3);

    private ClouderaManagerPollingTimeoutProvider() {

    }

    public static long getDefaultTimeout(String cloudPlatform) {
        if (CloudPlatform.MOCK.equals(CloudPlatform.valueOf(cloudPlatform))) {
            return DEFAULT_MOCK_TIMEOUT;
        }
        return POLL_FOR_ONE_HOUR;
    }

    public static long getSyncApiCommandTimeout(String cloudPlatform) {
        if (CloudPlatform.MOCK.equals(CloudPlatform.valueOf(cloudPlatform))) {
            return DEFAULT_MOCK_TIMEOUT;
        }
        return POLL_FOR_15_MINUTES;
    }

    public static long getRemoveHostsTimeout(String cloudPlatform) {
        if (CloudPlatform.MOCK.equals(CloudPlatform.valueOf(cloudPlatform))) {
            return DEFAULT_MOCK_TIMEOUT;
        }
        return POLL_FOR_THREE_HOURS;
    }
}
