package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_MANAGER_NOT_RESPONDING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.cdp.shaded.com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@ExtendWith(MockitoExtension.class)
class StackSyncServiceTest {

    @InjectMocks
    private StackSyncService underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackUpdater stackUpdater;

    @ParameterizedTest(name = "{0}: currentStatus={1} and expectedStatus={6}")
    @MethodSource("stackStatusForSync")
    void handleSyncResult(
            String methodName,
            Status currentStatus,
            int total,
            int running,
            int stopped,
            int deleted,
            SyncConfig syncConfig,
            DetailedStackStatus expectedStatus) {
        when(stack.getStatus()).thenReturn(currentStatus);
        lenient().when(stack.getId()).thenReturn(1L);
        Map<InstanceSyncState, Integer> instanceStateCounts = Maps.newHashMap();
        instanceStateCounts.put(InstanceSyncState.STOPPED, stopped);
        instanceStateCounts.put(InstanceSyncState.RUNNING, running);
        instanceStateCounts.put(InstanceSyncState.DELETED_ON_PROVIDER_SIDE, deleted);
        instanceStateCounts.put(InstanceSyncState.DELETED_BY_PROVIDER, 0);
        Set<InstanceMetaData> instances = Sets.newHashSet();
        for (int i = 0; i < total; i++) {
            instances.add(new InstanceMetaData());
        }
        underTest.handleSyncResult(stack, instanceStateCounts, syncConfig, instances);
        if (expectedStatus != null) {
            String statusReason = "Synced instance states with the cloud provider.";
            if (expectedStatus == CLUSTER_MANAGER_NOT_RESPONDING) {
                statusReason = "Cloudera Manager server not responding.";
            }
            if (!currentStatus.equals(expectedStatus.getStatus())) {
                verify(stackUpdater).updateStackStatus(1L, expectedStatus, statusReason);
            } else {
                verifyNoInteractions(stackUpdater);
            }
        } else {
            verifyNoInteractions(stackUpdater);
        }
    }

    public static Stream<Arguments> stackStatusForSync() {
        return Stream.of(
                Arguments.of(
                        "Stack was available and now 1 node stopped",
                        AVAILABLE,
                        5,
                        4,
                        1,
                        0,
                        new SyncConfig(true, true),
                        null
                        ),
                Arguments.of(
                        "Stack was available and now all stopped",
                        AVAILABLE,
                        5,
                        0,
                        5,
                        0,
                        new SyncConfig(true, true),
                        DetailedStackStatus.STOPPED
                        ),
                Arguments.of(
                        "Stack was available and now all deleted",
                        AVAILABLE,
                        0,
                        0,
                        0,
                        5,
                        new SyncConfig(true, true),
                        DetailedStackStatus.DELETED_ON_PROVIDER_SIDE
                        ),
                Arguments.of(
                        "Stack was node failure and now 1 node stopped",
                        NODE_FAILURE,
                        5,
                        3,
                        1,
                        1,
                        new SyncConfig(true, true),
                        null
                        ),
                Arguments.of(
                        "Stack was running with 1 node stopped and now available",
                        AVAILABLE,
                        5,
                        5,
                        0,
                        0,
                        new SyncConfig(true, true),
                        DetailedStackStatus.AVAILABLE
                ),
                Arguments.of(
                        "Stack was running with 1 node stopped and still running with 1 node stopped",
                        AVAILABLE,
                        5,
                        4,
                        1,
                        0,
                        new SyncConfig(true, true),
                        null
                ),
                Arguments.of(
                        "Stack was running with 1 node stopped and now running CM is not running",
                        AVAILABLE,
                        5,
                        4,
                        1,
                        0,
                        new SyncConfig(true, false),
                        CLUSTER_MANAGER_NOT_RESPONDING
                )
        );
    }
}