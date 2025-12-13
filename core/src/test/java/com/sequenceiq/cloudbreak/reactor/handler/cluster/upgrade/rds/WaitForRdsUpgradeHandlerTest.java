package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.WaitForDatabaseServerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.WaitForDatabaseServerUpgradeResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class WaitForRdsUpgradeHandlerTest {
    @Mock
    private ExternalDatabaseService databaseService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private WaitForRdsUpgradeHandler underTest;

    @Test
    void testDoAccept() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        WaitForDatabaseServerUpgradeRequest waitForDatabaseServerUpgradeRequest =
                new WaitForDatabaseServerUpgradeRequest(1L, TargetMajorVersion.VERSION14, flowIdentifier);
        StackDto stack = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(1L)).thenReturn(stack);

        WaitForDatabaseServerUpgradeResult actualResult = (WaitForDatabaseServerUpgradeResult) underTest.doAccept(
                new HandlerEvent<>(new Event<>(waitForDatabaseServerUpgradeRequest)));

        assertEquals(flowIdentifier, actualResult.getFlowIdentifier());
        verify(databaseService, times(1)).waitForDatabaseFlowToBeFinished(cluster, flowIdentifier);
    }

    @Test
    void testDoAcceptNoFlow() {
        WaitForDatabaseServerUpgradeRequest waitForDatabaseServerUpgradeRequest =
                new WaitForDatabaseServerUpgradeRequest(1L, TargetMajorVersion.VERSION14, null);
        StackDto stack = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(1L)).thenReturn(stack);

        WaitForDatabaseServerUpgradeResult actualResult = (WaitForDatabaseServerUpgradeResult) underTest.doAccept(
                new HandlerEvent<>(new Event<>(waitForDatabaseServerUpgradeRequest)));

        assertNull(actualResult.getFlowIdentifier());
        verify(databaseService, never()).waitForDatabaseFlowToBeFinished(any(), any());
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void testDoAcceptWhenException(Exception exception) {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        WaitForDatabaseServerUpgradeRequest waitForDatabaseServerUpgradeRequest =
                new WaitForDatabaseServerUpgradeRequest(1L, TargetMajorVersion.VERSION14, flowIdentifier);
        StackDto stack = mock(StackDto.class);
        lenient().when(stack.getName()).thenReturn("stack");
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(1L)).thenReturn(stack);
        doThrow(exception).when(databaseService).waitForDatabaseFlowToBeFinished(cluster, flowIdentifier);

        UpgradeRdsFailedEvent actualResult = (UpgradeRdsFailedEvent) underTest.doAccept(
                new HandlerEvent<>(new Event<>(waitForDatabaseServerUpgradeRequest)));

        assertEquals(exception, actualResult.getException());
    }

    private static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(new UserBreakException("userbreak")),
                Arguments.of(new PollerStoppedException("pollerstopped")),
                Arguments.of(new PollerException("pollerexception")),
                Arguments.of(new RuntimeException("runtimeexception")));
    }
}