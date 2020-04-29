package com.sequenceiq.cloudbreak.domain.stack.instance;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;

@RunWith(Parameterized.class)
public class InstanceMetaDataReachableTest {

    private InstanceStatus instanceStatus;

    private boolean reachable;

    public InstanceMetaDataReachableTest(InstanceStatus instanceStatus, boolean reachable) {
        this.instanceStatus = instanceStatus;
        this.reachable = reachable;
    }

    @Parameterized.Parameters(name = "{index}: status={0}, reachable={1}")
    public static Iterable<Object[]> data() {
        EnumSet<InstanceStatus> notReachableStates = EnumSet.of(
                InstanceStatus.TERMINATED,
                InstanceStatus.DELETED_ON_PROVIDER_SIDE,
                InstanceStatus.FAILED,
                InstanceStatus.ORCHESTRATION_FAILED,
                InstanceStatus.STOPPED);

        return Arrays.stream(InstanceStatus.values())
                .map(status -> {
                    if (notReachableStates.contains(status)) {
                        return new Object[]{status, false};
                    } else {
                        return new Object[]{status, true};
                    }
                }).collect(Collectors.toList());
    }

    @Test
    public void testStatusIsReachable() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);

        boolean result = instanceMetaData.isReachable();

        assertEquals(reachable, result);
    }
}