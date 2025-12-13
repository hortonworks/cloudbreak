package com.sequenceiq.flow.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.config.TestFlowConfig;
import com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowEvent;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.FlowLogRepository;

@ExtendWith(MockitoExtension.class)
class FlowRetryServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_ID = "flowId";

    private static final String IGNORED_EVENT = "IGNORED_EVENT";

    @InjectMocks
    private FlowRetryService underTest;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private FlowLogRepository flowLogRepository;

    private TestFlowConfig flowConfig;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(underTest, "retryableEvents", List.of(TestFlowConfig.TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()));
        ReflectionTestUtils.setField(underTest, "ignoredFromRetryEvents", Set.of(IGNORED_EVENT));

        flowConfig = new TestFlowConfig();
        ReflectionTestUtils.setField(underTest, "flowConfigs", List.of(flowConfig));
    }

    @Test
    void retryPending() {
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, Instant.now().toEpochMilli(), TestFlowEvent.TEST_FLOW_EVENT.event()),
                createFlowLog("TEST_STATE", StateStatus.PENDING, Instant.now().toEpochMilli(), TestFlowEvent.TEST_FINISHED_EVENT.event())
                );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        try {
            assertThrows(BadRequestException.class, () -> underTest.retry(STACK_ID));
        } finally {
            verify(flow2Handler, times(0)).restartFlow(any(FlowLog.class));
        }
    }

    private FlowLog createFlowLog(String currentState, StateStatus stateStatus, long created, String name) {
        return createFlowLog(FLOW_ID, currentState, stateStatus, created, name);
    }

    private FlowLog createFlowLog(String flowId, String currentState, StateStatus stateStatus, long created, String name) {
        FlowLog flowLog = new FlowLog(STACK_ID, flowId, currentState, true, stateStatus, OperationType.UNKNOWN);
        flowLog.setCreated(created);
        flowLog.setFlowType(ClassValue.of(flowConfig.getClass()));
        flowLog.setNextEvent(name);
        return flowLog;
    }

    @Test
    void retry() {
        FlowLog lastSuccessfulState = createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 5, TestFlowEvent.TEST_FINISHED_EVENT.event());
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 7, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 6, TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()),
                lastSuccessfulState,
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 4, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 3, TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()),
                createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, TestFlowEvent.TEST_FINISHED_EVENT.event()),
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, 1, TestFlowEvent.TEST_FLOW_EVENT.event())
                );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        underTest.retry(STACK_ID);

        verify(flow2Handler, times(1)).restartFlow(eq(lastSuccessfulState));
    }

    @Test
    void retryWithBeforeRestartFunction() {
        FlowLog lastSuccessfulState = createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 5, TestFlowEvent.TEST_FINISHED_EVENT.event());
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 7, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 6, TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()),
                lastSuccessfulState,
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 4, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 3, TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()),
                createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, TestFlowEvent.TEST_FINISHED_EVENT.event()),
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, 1, TestFlowEvent.TEST_FLOW_EVENT.event())
        );
        Consumer<FlowLog> beforeRestart = mock(Consumer.class);
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);

        underTest.retry(STACK_ID, beforeRestart);

        verify(beforeRestart, times(1)).accept(lastSuccessfulState);
        verify(flow2Handler, times(1)).restartFlow(eq(lastSuccessfulState));
    }

    @Test
    void retryNoFailed() {
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 4, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 3, TestFlowEvent.TEST_FINALIZED_EVENT.event()),
                createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, TestFlowEvent.TEST_FINISHED_EVENT.event()),
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, 1, TestFlowEvent.TEST_FLOW_EVENT.event())
        );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        assertThrows(BadRequestException.class, () -> underTest.retry(STACK_ID));

        verify(flow2Handler, never()).restartFlow(any(FlowLog.class));
    }

    @Test
    void retryLastFailedWithSuccessfulAfter() {
        FlowLog lastSuccessfulState = createFlowLog("1", "INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 5, TestFlowEvent.TEST_FINISHED_EVENT.event());
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("2", IGNORED_EVENT, StateStatus.SUCCESSFUL, 7, null),
                createFlowLog("1", "NEXT_STATE", StateStatus.FAILED, 6, TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()),
                lastSuccessfulState,
                createFlowLog("1", "INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, TestFlowEvent.TEST_FINISHED_EVENT.event()),
                createFlowLog("1", "INIT_STATE", StateStatus.SUCCESSFUL, 1, TestFlowEvent.TEST_FLOW_EVENT.event())
        );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        underTest.retry(STACK_ID);

        verify(flow2Handler, times(1)).restartFlow(eq(lastSuccessfulState));
    }
}
