package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.concurrent.TimeUnit;

public class GcpRequestTimeout {

    private static final int THREE_MINUTES = 3;

    private GcpRequestTimeout() {

    }

    public static int getTimeout() {
        return (int) TimeUnit.MINUTES.toMillis(THREE_MINUTES);
    }

}
