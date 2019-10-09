package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.VALIDATION_FINISHED_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
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
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.FlowLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class OperationRetryServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_ID = "flowId";

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(2018, 1, 1, 0, 1);

    @InjectMocks
    private OperationRetryService underTest;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private Stack stackMock;

    @Mock
    private Cluster clusterMock;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    private StackCreationFlowConfig flowConfig;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(underTest, "failHandledEvents", List.of(STACKCREATION_FAILURE_HANDLED_EVENT.event()));

        flowConfig = new StackCreationFlowConfig();
        ReflectionTestUtils.setField(underTest, "flowConfigs", List.of(flowConfig));
    }

    @Test(expected = BadRequestException.class)
    public void retryPending() {
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, Instant.now().toEpochMilli(), START_CREATION_EVENT.event()),
                createFlowLog("START_STATE", StateStatus.PENDING, Instant.now().toEpochMilli(), VALIDATION_FINISHED_EVENT.event())
                );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        try {
            underTest.retry(STACK_ID);
        } finally {
            verify(flow2Handler, times(0)).restartFlow(any(FlowLog.class));
        }
    }

    private FlowLog createFlowLog(String currentState, StateStatus stateStatus, long created, String name) {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, currentState, true, stateStatus);
        flowLog.setCreated(created);
        flowLog.setFlowType(flowConfig.getClass());
        flowLog.setNextEvent(name);
        return flowLog;
    }

    @Test
    public void retry() {
        FlowLog lastSuccessfulState = createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 5, VALIDATION_FINISHED_EVENT.event());
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 7, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 6, STACKCREATION_FAILURE_HANDLED_EVENT.event()),
                lastSuccessfulState,
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 4, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 3, STACKCREATION_FAILURE_HANDLED_EVENT.event()),
                createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, VALIDATION_FINISHED_EVENT.event()),
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, 1, START_CREATION_EVENT.event())
                );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        underTest.retry(STACK_ID);

        verify(flow2Handler, times(1)).restartFlow(ArgumentMatchers.eq(lastSuccessfulState));
    }

    @Test(expected = BadRequestException.class)
    public void retryNoFailed() {
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, 4, null),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, 3, SETUP_FINISHED_EVENT.event()),
                createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, 2, VALIDATION_FINISHED_EVENT.event()),
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, 1, START_CREATION_EVENT.event())
        );
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(STACK_ID, PageRequest.of(0, 50))).thenReturn(pendingFlowLogs);
        underTest.retry(STACK_ID);

        verify(flow2Handler, never()).restartFlow(any(FlowLog.class));
    }
}
