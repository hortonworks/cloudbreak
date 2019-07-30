package com.sequenceiq.cloudbreak.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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

        cluster.setStack(stack);
        cluster.setId(1L);
        cluster.setName("test");
        cluster.setStatus(Status.CREATE_FAILED);
    }
}