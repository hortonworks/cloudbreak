package com.sequenceiq.flow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.config.TestFlowConfig;
import com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowEvent;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.FlowLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowRetryServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_ID = "flowId";

    @InjectMocks
    private FlowRetryService underTest;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private FlowLogRepository flowLogRepository;

    private TestFlowConfig flowConfig;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(underTest, "retryableEvents", List.of(TestFlowConfig.TestFlowEvent.TEST_FAIL_HANDLED_EVENT.event()));

        flowConfig = new TestFlowConfig();
        ReflectionTestUtils.setField(underTest, "flowConfigs", List.of(flowConfig));
    }

    @Test(expected = BadRequestException.class)
    public void retryPending() {
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, Instant.now().toEpochMilli(), TestFlowEvent.TEST_FLOW_EVENT.event()),
                createFlowLog("TEST_STATE", StateStatus.PENDING, Instant.now().toEpochMilli(), TestFlowEvent.TEST_FINISHED_EVENT.event())
                );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        try {
            underTest.retry(STACK_ID);
        } finally {
            verify(flow2Handler, times(0)).restartFlow(any(FlowLog.class));
        }
    }

    private FlowLog createFlowLog(String currentState, StateStatus stateStatus, long created, String name) {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, currentState, true, stateStatus, OperationType.UNKNOWN);
        flowLog.setCreated(created);
        flowLog.setFlowType(flowConfig.getClass());
        flowLog.setNextEvent(name);
        return flowLog;
    }

    @Test
    public void retry() {
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

        verify(flow2Handler, times(1)).restartFlow(ArgumentMatchers.eq(lastSuccessfulState));
    }

    @Test(expected = BadRequestException.class)
    public void retryNoFailed() {
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 4, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 3, TestFlowEvent.TEST_FINALIZED_EVENT.event()),
                createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, TestFlowEvent.TEST_FINISHED_EVENT.event()),
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, 1, TestFlowEvent.TEST_FLOW_EVENT.event())
        );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        underTest.retry(STACK_ID);

        verify(flow2Handler, never()).restartFlow(any(FlowLog.class));
    }
}
