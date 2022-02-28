package com.sequenceiq.cloudbreak.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.usage.strategy.CompositeUsageProcessingStrategy;
import com.sequenceiq.cloudbreak.usage.strategy.LoggingUsageProcessingStrategy;
import com.sequenceiq.cloudbreak.usage.UsageReportProcessor;

// This test makes sure that we do not blow-up on null values during usage logging.
public class UsageLoggingUtilTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1:user:2";

    UsageLoggingUtil util;

    Cluster cluster;

    Stack stack;

    @Test
    public void logClusterRequestedUsageEvent() {
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> util.logClusterRequestedUsageEvent(cluster));
        util.logClusterRequestedUsageEvent(null);
    }

    @Test
    public void logClusterStatusChangeUsageEvent() {
        util.logClusterStatusChangeUsageEvent(Status.AVAILABLE, Status.AVAILABLE, cluster);
        util.logClusterStatusChangeUsageEvent(null, null, null);
        util.logClusterStatusChangeUsageEvent(Status.AVAILABLE, Status.AVAILABLE, cluster);
    }

    @BeforeEach
    public void setUp() {
        CompositeUsageProcessingStrategy usageProcessingStrategy = new CompositeUsageProcessingStrategy(new LoggingUsageProcessingStrategy(),
                null, null, null, null);
        UsageReportProcessor usageReportProcessor = new UsageReportProcessor(usageProcessingStrategy);
        util = new UsageLoggingUtil(usageReportProcessor);
        cluster = new Cluster();

        stack = new Stack();
        stack.setCluster(cluster);
        stack.setCloudPlatform("mock");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.CLUSTER_CREATE_FAILED));

        cluster.setStack(stack);
        cluster.setId(1L);
        cluster.setName("test");
    }
}