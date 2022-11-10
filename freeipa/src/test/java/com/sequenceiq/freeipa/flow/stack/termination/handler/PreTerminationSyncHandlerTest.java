package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.sync.PreTerminationSyncFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.sync.PreTerminationSyncRequest;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.FreeipaChecker;
import com.sequenceiq.freeipa.sync.FreeipaStatusInfoLogger;
import com.sequenceiq.freeipa.sync.ProviderChecker;
import com.sequenceiq.freeipa.sync.SyncResult;

@ExtendWith(MockitoExtension.class)
public class PreTerminationSyncHandlerTest {

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private StackService stackService;

    @Mock
    private FreeipaChecker freeipaChecker;

    @Mock
    private ProviderChecker providerChecker;

    @Mock
    private FreeipaStatusInfoLogger freeipaStatusInfoLogger;

    @InjectMocks
    private PreTerminationSyncHandler underTest;

    @BeforeEach
    void setup() {
        RegionAwareInternalCrnGenerator crnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(crnGenerator);
        when(crnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__");
        lenient().doNothing().when(freeipaStatusInfoLogger).logFreeipaStatus(anyLong(), anySet());
    }

    @Test
    void testSuccessSyncWhenNoCheckableInstances() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(createStack(true));
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new PreTerminationSyncRequest(1L, false))));
        assertTrue(result instanceof PreTerminationSyncFinished);
        verifyNoInteractions(freeipaChecker);
        verifyNoInteractions(providerChecker);
    }

    @Test
    void testSuccessSyncWhenStackAvailable() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(createStack(false));
        when(freeipaChecker.getStatus(any(), anySet())).thenReturn(new SyncResult("", DetailedStackStatus.AVAILABLE,
                Map.of(createInstanceMetadata(InstanceStatus.UNREACHABLE), DetailedStackStatus.AVAILABLE)));
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new PreTerminationSyncRequest(1L, false))));
        assertTrue(result instanceof PreTerminationSyncFinished);
        verifyNoInteractions(providerChecker);
    }

    @Test
    void testSuccessSyncWhenStackNotAvailable() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(createStack(false));
        when(freeipaChecker.getStatus(any(), anySet())).thenReturn(new SyncResult("", DetailedStackStatus.UNREACHABLE,
                Map.of(createInstanceMetadata(InstanceStatus.CREATED), DetailedStackStatus.UNREACHABLE)));
        when(providerChecker.updateAndGetStatuses(any(), any(), any(), anyBoolean())).thenReturn(List.of());
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new PreTerminationSyncRequest(1L, false))));
        assertTrue(result instanceof PreTerminationSyncFinished);
    }

    @Test
    void testFailedSyncWhenNotForced() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenThrow(new RuntimeException("anexception"));
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new PreTerminationSyncRequest(1L, false))));
        assertTrue(result instanceof StackFailureEvent);
    }

    @Test
    void testFailedSyncWhenForced() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenThrow(new RuntimeException("anexception"));
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new PreTerminationSyncRequest(1L, true))));
        assertTrue(result instanceof PreTerminationSyncFinished);
    }

    private Stack createStack(boolean empty) {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        Set<InstanceMetaData> instances = Sets.newHashSet();
        if (!empty) {
            instances.add(createInstanceMetadata(InstanceStatus.CREATED));
        }
        instanceGroup.setInstanceMetaData(instances);
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private InstanceMetaData createInstanceMetadata(InstanceStatus status) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(status);
        return instanceMetaData;
    }
}
