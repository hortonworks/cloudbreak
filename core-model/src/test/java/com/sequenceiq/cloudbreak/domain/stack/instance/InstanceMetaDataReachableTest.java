package com.sequenceiq.cloudbreak.domain.stack.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;

class InstanceMetaDataReachableTest {

    public static Iterable<Object[]> data() {
        EnumSet<InstanceStatus> notReachableStates = EnumSet.of(
                InstanceStatus.TERMINATED,
                InstanceStatus.DELETED_ON_PROVIDER_SIDE,
                InstanceStatus.DELETED_BY_PROVIDER,
                InstanceStatus.FAILED,
                InstanceStatus.ORCHESTRATION_FAILED,
                InstanceStatus.STOPPED,
                InstanceStatus.ZOMBIE);

        return Arrays.stream(InstanceStatus.values())
                .map(status -> {
                    if (notReachableStates.contains(status)) {
                        return new Object[]{status, false};
                    } else {
                        return new Object[]{status, true};
                    }
                }).collect(Collectors.toList());
    }

    @MethodSource("data")
    @ParameterizedTest
    void testStatusIsReachable(InstanceStatus instanceStatus, boolean reachable) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);

        boolean result = instanceMetaData.isReachable();

        assertEquals(reachable, result);
    }
}