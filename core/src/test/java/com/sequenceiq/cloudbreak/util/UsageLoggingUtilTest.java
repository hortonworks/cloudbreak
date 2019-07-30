package com.sequenceiq.cloudbreak.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import org.junit.Before;
import org.junit.Test;

// This test makes sure that we do not blow-up on null values during usage logging.
public class UsageLoggingUtilTest {

    UsageLoggingUtil util;

    Cluster cluster;

    Stack stack;

    @Test
    public void logClusterRequestedUsageEvent() {
        util.logClusterRequestedUsageEvent(cluster);
        util.logClusterRequestedUsageEvent(null);
    }

    @Test
    public void logClusterStatusChangeUsageEvent() {
        util.logClusterStatusChangeUsageEvent(Status.AVAILABLE, cluster);
        util.logClusterStatusChangeUsageEvent(null, null);
        cluster.setStatus(null);
        util.logClusterStatusChangeUsageEvent(Status.AVAILABLE, cluster);
    }

    @Before
    public void setUp() {
        util = new UsageLoggingUtil();
        cluster = new Cluster();

        stack = new Stack();
        stack.setCluster(cluster);
        stack.setCloudPlatform("mock");

        cluster.setStack(stack);
        cluster.setId(1L);
        cluster.setName("test");
        cluster.setStatus(Status.CREATE_FAILED);
    }
}