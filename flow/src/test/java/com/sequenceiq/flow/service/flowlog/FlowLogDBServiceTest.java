package com.sequenceiq.flow.service.flowlog;

import static com.sequenceiq.flow.core.FlowConstants.CANCELLED_STATE;
import static com.sequenceiq.flow.core.FlowConstants.FINISHED_STATE;
import static com.sequenceiq.flow.core.FlowConstants.TERMINATED_STATE;
import static com.sequenceiq.flow.domain.ClassValue.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowEventContext;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.repository.FlowLogRepository;

@ExtendWith(MockitoExtension.class)
class FlowLogDBServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final Long FLOW_LOG_ID = 1L;

    private static final long ID = 123L;

    private static final Long DATABASE_ID = 234L;

    private static final String NODE_ID = "node1";

    private static final String REASON = "reason";

    private static final String CLOUDBREAK_STACK_CRN = CrnTestUtil.getDatalakeCrnBuilder()
            .setAccountId("acc")
            .setResource("stack")
            .build().toString();

    @InjectMocks
    private FlowLogDBService underTest;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private ResourceIdProvider resourceIdProvider;

    @Mock
    private ApplicationFlowInformation applicationFlowInformation;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private Clock clock;

    @Captor
    private ArgumentCaptor<FlowLog> savedFlowLogCaptor;

    @Test
    void updateLastFlowLogStatus() {
        runUpdateLastFlowLogStatusTest(false, StateStatus.SUCCESSFUL);
    }

    @Test
    void updateLastFlowLogStatusFailure() {
        runUpdateLastFlowLogStatusTest(true, StateStatus.FAILED);
    }

    private void runUpdateLastFlowLogStatusTest(boolean failureEvent, StateStatus successful) {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);
        Long currentTime = 123456789L;
        doReturn(currentTime).when(clock).getCurrentTimeMillis();

        underTest.updateLastFlowLogStatus(flowLog, failureEvent, REASON);

        verify(flowLogRepository, times(1)).save(eq(flowLog));
        assertEquals(REASON, flowLog.getReason());
        assertEquals(successful, flowLog.getStateStatus());
    }

    @Test
    void getLastFlowLog() {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        Page<FlowLogWithoutPayload> flowLogOptional = new PageImpl<>(List.of(flowLog));

        when(flowLogRepository.findByFlowIdOrderByCreatedDesc(FLOW_ID, Pageable.ofSize(1))).thenReturn(flowLogOptional);

        Optional<FlowLogWithoutPayload> lastFlowLog = underTest.getLastFlowLog(FLOW_ID);
        assertEquals(flowLogOptional.stream().findFirst(), lastFlowLog);
    }

    @Test
    void updateLastFlowLogPayload() {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);

        Payload payload = new TestSelectable();
        Map<Object, Object> variables = Map.of("repeated", 2);

        underTest.updateLastFlowLogPayload(flowLog, payload, variables);

        ArgumentCaptor<FlowLog> flowLogCaptor = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(1)).save(flowLogCaptor.capture());

        FlowLog savedFlowLog = flowLogCaptor.getValue();
        assertEquals(flowLog.getId(), savedFlowLog.getId());

        String payloadJackson = JsonUtil.writeValueAsStringSilent(payload);
        String variablesJackson = TypedJsonUtil.writeValueAsStringSilent(variables);
        assertEquals(payloadJackson, savedFlowLog.getPayloadJackson());
        assertEquals(variablesJackson, savedFlowLog.getVariablesJackson());
    }

    @Test
    void testGetResourceIdIfTheInputIsCrn() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);

        underTest.getResourceIdByCrnOrName(CLOUDBREAK_STACK_CRN);

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    void testGetResourceIdIfTheInputIsNotCrn() {
        when(resourceIdProvider.getResourceIdByResourceName(anyString())).thenReturn(1L);

        underTest.getResourceIdByCrnOrName("stackName");

        verify(resourceIdProvider, times(1)).getResourceIdByResourceName(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceCrn(anyString());
    }

    @Test
    void testGetFlowLogs() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Optional.of(createFlowLog("1")));
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByResourceCrnOrName(CLOUDBREAK_STACK_CRN).size());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    void testGetFlowLogsWithChainId() {
        when(flowLogRepository.findAllByFlowIdsCreatedDesc(any(), any())).thenReturn(new PageImpl<>(List.of(createFlowLog("flow"))));

        assertEquals(1, underTest.getFlowLogsByFlowIdsCreatedDesc(Set.of("flowchain"), PageRequest.of(0, 50)).getContent().size());
    }

    @Test
    void testGetLastFlowLogWhenThereIsNoFlow() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN))
                .hasMessage("Flow log for resource not found!")
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetLastFlowLog() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Optional.of(createFlowLog("1")));
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(createFlowLog("1"), createFlowLog("2")));

        assertEquals("1", underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN).getFlowId());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    void cancelTooOldTerminationFlowForResourceTest() throws TransactionExecutionException {
        Set<FlowLogIdWithTypeAndTimestamp> flowLogs = new LinkedHashSet<>();
        Long currentTime = 123456789L;
        doReturn(currentTime).when(clock).getCurrentTimeMillis();
        FlowLogIdWithTypeAndTimestamp flowLog2 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog2.getFlowType()).thenReturn(of(Class.class));
        flowLogs.add(flowLog2);
        FlowLogIdWithTypeAndTimestamp flowLog1 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog1.getFlowType()).thenReturn(of(TerminationFlowConfig.class));
        when(flowLog1.getCreated()).thenReturn(9000L);
        when(flowLog1.getFlowId()).thenReturn("flow1");
        flowLogs.add(flowLog1);
        when(flowLogRepository.findAllRunningFlowLogByResourceId(eq(1L))).thenReturn(flowLogs);
        FlowLog realFlowLog1 = mock(FlowLog.class);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(eq("flow1"))).thenReturn(Optional.of(realFlowLog1));
        when(applicationFlowInformation.getTerminationFlow()).thenReturn(Collections.singletonList(TerminationFlowConfig.class));
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> ((Supplier) invocation.getArguments()[0]).get());
        when(nodeConfig.getId()).thenReturn(NODE_ID);
        underTest.cancelTooOldTerminationFlowForResource(1L, 10000L);
        verify(flowLogRepository).save(eq(realFlowLog1));
        verify(flowLogRepository).findAllByFlowIdOrderByCreatedDesc("flow1");
        verify(flowLogRepository, never()).findAllByFlowIdOrderByCreatedDesc("flow2");
        verify(flowLogRepository).saveAll(anyList());
    }

    @Test
    void doNotCancelTooYoungTerminationFlowForResourceTest() {
        Set<FlowLogIdWithTypeAndTimestamp> flowLogs = new HashSet<>();
        FlowLogIdWithTypeAndTimestamp flowLog1 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog1.getFlowType()).thenReturn(of(TerminationFlowConfig.class));
        when(flowLog1.getCreated()).thenReturn(11000L);
        flowLogs.add(flowLog1);
        FlowLogIdWithTypeAndTimestamp flowLog2 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog2.getFlowType()).thenReturn(of(Class.class));
        flowLogs.add(flowLog2);
        when(flowLogRepository.findAllRunningFlowLogByResourceId(eq(1L))).thenReturn(flowLogs);
        when(applicationFlowInformation.getTerminationFlow()).thenReturn(Collections.singletonList(TerminationFlowConfig.class));
        underTest.cancelTooOldTerminationFlowForResource(1L, 10000L);
        verify(flowLogRepository, times(0)).save(any());
        verify(flowLogRepository, never()).findAllByFlowIdOrderByCreatedDesc("flow1");
        verify(flowLogRepository, never()).findAllByFlowIdOrderByCreatedDesc("flow2");
        verify(flowLogRepository, never()).saveAll(anyList());
    }

    @Test
    void testNoPendingFlowEvent() {
        Boolean actual = underTest.hasPendingFlowEvent(Lists.newArrayList(createFlowLog(false, "1"), createFlowLog(false, "2")));
        assertEquals(Boolean.FALSE, actual);
    }

    @Test
    void testHasPendingFlowEvent() {
        Boolean actual = underTest.hasPendingFlowEvent(Lists.newArrayList(createFlowLog(true, "1"), createFlowLog(false, "2")));
        assertEquals(Boolean.TRUE, actual);
    }

    @Test
    void testTerminate() throws TransactionExecutionException {
        prepareFinalization();
        FlowLog flowLog = underTest.terminate(ID, FLOW_ID, REASON);

        verifyFinalization();
        verify(applicationFlowInformation).handleFlowFail(flowLog);
        FlowLog savedFlowLog = savedFlowLogCaptor.getValue();
        assertThat(savedFlowLog.getResourceId()).isEqualTo(ID);
        assertThat(savedFlowLog.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(savedFlowLog.getCurrentState()).isEqualTo(TERMINATED_STATE);
        assertThat(savedFlowLog.getFinalized()).isTrue();
        assertThat(savedFlowLog.getStateStatus()).isEqualTo(StateStatus.SUCCESSFUL);
        assertThat(savedFlowLog.getOperationType()).isEqualTo(OperationType.DIAGNOSTICS);
        assertThat(savedFlowLog.getCloudbreakNodeId()).isEqualTo(NODE_ID);
        assertThat(savedFlowLog.getVariablesJackson()).isNull();
        assertTrue(savedFlowLog.getCreated() > 0L);
        assertEquals(savedFlowLog.getCreated(), savedFlowLog.getEndTime());
    }

    @Test
    void testFinish() throws TransactionExecutionException {
        prepareFinalization();
        Map<Object, Object> params = Map.of("param1", StateStatus.SUCCESSFUL, "param2", 234L, "param3", "true");
        underTest.finish(new FlowEventContext(FLOW_ID, null, null, null, null, null,
                new BaseFlowEvent(null, ID, null)), params, false, REASON);

        verifyFinalization();
        FlowLog savedFlowLog = savedFlowLogCaptor.getValue();
        assertThat(savedFlowLog.getResourceId()).isEqualTo(ID);
        assertThat(savedFlowLog.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(savedFlowLog.getCurrentState()).isEqualTo(FINISHED_STATE);
        assertThat(savedFlowLog.getFinalized()).isTrue();
        assertThat(savedFlowLog.getStateStatus()).isEqualTo(StateStatus.SUCCESSFUL);
        assertThat(savedFlowLog.getOperationType()).isEqualTo(OperationType.DIAGNOSTICS);
        assertThat(savedFlowLog.getCloudbreakNodeId()).isEqualTo(NODE_ID);
        assertThat(savedFlowLog.getVariablesJackson()).isEqualTo(TypedJsonUtil.writeValueAsStringSilent(params));
        assertTrue(savedFlowLog.getCreated() > 0L);
        assertEquals(savedFlowLog.getCreated(), savedFlowLog.getEndTime());
    }

    @Test
    void testCancel() throws TransactionExecutionException {
        prepareFinalization();
        underTest.cancel(ID, FLOW_ID);

        verify(flowLogRepository, times(2)).save(savedFlowLogCaptor.capture());
        assertThat(savedFlowLogCaptor.getAllValues()).hasSize(2);
        FlowLog savedFlowLog = savedFlowLogCaptor.getAllValues().get(1);
        assertThat(savedFlowLog.getResourceId()).isEqualTo(ID);
        assertThat(savedFlowLog.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(savedFlowLog.getCurrentState()).isEqualTo(CANCELLED_STATE);
        assertThat(savedFlowLog.getFinalized()).isTrue();
        assertThat(savedFlowLog.getStateStatus()).isEqualTo(StateStatus.SUCCESSFUL);
        assertThat(savedFlowLog.getOperationType()).isEqualTo(OperationType.DIAGNOSTICS);
        assertThat(savedFlowLog.getCloudbreakNodeId()).isEqualTo(NODE_ID);
        assertThat(savedFlowLog.getVariablesJackson()).isNull();
        assertTrue(savedFlowLog.getCreated() > 0L);
        assertEquals(savedFlowLog.getCreated(), savedFlowLog.getEndTime());
        verify(flowLogRepository).findAllByFlowIdOrderByCreatedDesc(FLOW_ID);
        verify(flowLogRepository).saveAll(anyList());
    }

    private void prepareFinalization() throws TransactionExecutionException {
        Long currentTime = 123456789L;
        doReturn(currentTime).when(clock).getCurrentTimeMillis();
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> ((Supplier) invocation.getArguments()[0]).get());
        FlowLog lastFlowLog = new FlowLog(DATABASE_ID, FLOW_ID, "currentState", false, StateStatus.SUCCESSFUL, OperationType.DIAGNOSTICS);
        lastFlowLog.setId(DATABASE_ID);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(lastFlowLog));
        when(nodeConfig.getId()).thenReturn(NODE_ID);
        FlowLog initialFlowLog = new FlowLog(DATABASE_ID, FLOW_ID, "initState", false, StateStatus.SUCCESSFUL, OperationType.DIAGNOSTICS);
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(List.of(lastFlowLog, initialFlowLog));
    }

    private void verifyFinalization() {
        verify(flowLogRepository, times(2)).save(savedFlowLogCaptor.capture());
        assertThat(savedFlowLogCaptor.getAllValues()).hasSize(2);
        FlowLog lastFlowLog = savedFlowLogCaptor.getAllValues().get(0);
        assertEquals(DATABASE_ID, lastFlowLog.getId());
        assertEquals(REASON, lastFlowLog.getReason());
        verify(flowLogRepository).findAllByFlowIdOrderByCreatedDesc(FLOW_ID);
        ArgumentCaptor<List<FlowLog>> flowLogsCaptor = ArgumentCaptor.forClass(List.class);
        verify(flowLogRepository).saveAll(flowLogsCaptor.capture());
        List<FlowLog> savedFlowLogs = flowLogsCaptor.getValue();
        assertThat(savedFlowLogs).hasSize(2);
        assertThat(savedFlowLogs).allMatch(flowLog -> Boolean.TRUE.equals(flowLog.getFinalized()));
        assertThat(savedFlowLogs).allMatch(flowLog -> !StateStatus.PENDING.equals(flowLog.getStateStatus()));
    }

    private FlowLog createFlowLog(boolean pending, String flowId) {
        FlowLog flowLog = createFlowLog(flowId);
        flowLog.setFinalized(!pending);
        flowLog.setStateStatus(pending ? StateStatus.PENDING : StateStatus.SUCCESSFUL);
        return flowLog;
    }

    private FlowLog createFlowLog(String flowId) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId(flowId);
        flowLog.setFlowChainId(flowId + "chain");
        return flowLog;
    }

    @Test
    void testIsTerminatedFlowAlreadyRunningWhenHasActiveTerminationFlow() throws ClassNotFoundException {
        FlowLog flowLog = mock(FlowLog.class);
        com.sequenceiq.flow.domain.ClassValue terminationFlowConfig = of(TerminationFlowConfig.class.getName());
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(1L)).thenReturn(Optional.of(flowLog));
        when(flowLog.getFlowType()).thenReturn(terminationFlowConfig);
        when(flowLog.getStateStatus()).thenReturn(StateStatus.PENDING);
        boolean actual = underTest.isFlowConfigAlreadyRunning(1L, TerminationFlowConfig.class);
        assertTrue(actual);
    }

    @Test
    void testIsTerminatedFlowAlreadyRunningWhenLastFlowTermButNotPending() throws ClassNotFoundException {
        FlowLog flowLog = mock(FlowLog.class);
        com.sequenceiq.flow.domain.ClassValue terminationFlowConfig = of(TerminationFlowConfig.class.getName());
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(1L)).thenReturn(Optional.of(flowLog));
        when(flowLog.getFlowType()).thenReturn(terminationFlowConfig);
        when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        boolean actual = underTest.isFlowConfigAlreadyRunning(1L, TerminationFlowConfig.class);
        assertFalse(actual);
    }

    @Test
    void testIsTerminatedFlowAlreadyRunningWhenLastFlowIsNotTermination() throws ClassNotFoundException {
        FlowLog flowLog = mock(FlowLog.class);
        com.sequenceiq.flow.domain.ClassValue terminationFlowConfig = of(TerminationFlowConfig.class.getName());
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(1L)).thenReturn(Optional.of(flowLog));
        when(flowLog.getFlowType()).thenReturn(terminationFlowConfig);
        boolean actual = underTest.isFlowConfigAlreadyRunning(1L, TerminationFlowConfig.class);
        assertFalse(actual);
    }

    @Test
    void testTermFlowAlreadyRunningReturnFalseWhenTheFlowTypeIsNotPresent() {
        FlowLog flowLog = mock(FlowLog.class);
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(1L)).thenReturn(Optional.of(flowLog));
        when(flowLog.getFlowType()).thenReturn(null);
        boolean actual = underTest.isFlowConfigAlreadyRunning(1L, TerminationFlowConfig.class);
        assertFalse(actual);
    }

    @Test
    void testIsTerminatedFlowAlreadyRunningWhenNoLastFlow() {
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(1L)).thenReturn(Optional.empty());
        boolean actual = underTest.isFlowConfigAlreadyRunning(1L, TerminationFlowConfig.class);
        assertFalse(actual);
    }

    @Test
    void testFindAllFlowByFlowChainId() {
        FlowLog log1 = new FlowLog(1289L, "0de1361e-5538-4052-a02b-c79a3bb9c5bd",
                "DATAHUB_CLUSTERS_DELETE_STARTED_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        log1.setCreated(1669058834862L);
        log1.setEndTime(1669058834878L);
        FlowLog log2 = new FlowLog(1291L, "0de1361e-5538-4052-a02b-c79a3bb9c5bd",
                "DATALAKE_CLUSTERS_DELETE_STARTED_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        log2.setCreated(1669058834613L);
        log2.setEndTime(1669058834858L);
        FlowLog log3 = new FlowLog(1290L, "0de1361e-5538-4052-a02b-c79a3bb9c5bd",
                "EXPERIENCE_DELETE_STARTED_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        log3.setCreated(1669058834492L);
        log3.setEndTime(1669058834611L);
        FlowLog log4 = new FlowLog(1295L, "741b0bd1-f94a-4184-82c9-c6240c22faf4",
                "ENV_DELETE_FAILED_STATE", true, StateStatus.FAILED, OperationType.UNKNOWN);
        log4.setCreated(1669058928253L);
        FlowLog log5 = new FlowLog(1294L, "741b0bd1-f94a-4184-82c9-c6240c22faf4",
                "FREEIPA_DELETE_STARTED_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        log5.setCreated(1669058928119L);
        log5.setEndTime(1669058928246L);
        FlowLog log6 = new FlowLog(1293L, "741b0bd1-f94a-4184-82c9-c6240c22faf4",
                "INIT_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        log6.setCreated(1669058834958L);
        log6.setEndTime(1669058928114L);
        when(flowLogRepository.findAllByFlowChainIdOrderByCreatedDesc(Set.of("a"))).thenReturn(List.of(log1, log2, log3, log4, log5, log6));
        List<FlowLog> actual = underTest.findAllFlowByFlowChainId(Set.of("a"));
        assertEquals(2, actual.size());
        assertEquals(log1, actual.stream().filter(s -> Objects.equals(s.getFlowId(), "0de1361e-5538-4052-a02b-c79a3bb9c5bd")).findFirst().get());
        assertNull(actual.stream().filter(s -> Objects.equals(s.getFlowId(), "741b0bd1-f94a-4184-82c9-c6240c22faf4")).findFirst().get().getEndTime());
    }

    @Test
    void testFindAllFlowByFlowChainIdEmptyResult() {
        when(flowLogRepository.findAllByFlowChainIdOrderByCreatedDesc(Set.of("a"))).thenReturn(new ArrayList<>());
        List<FlowLog> actual = underTest.findAllFlowByFlowChainId(Set.of("a"));
        assertEquals(0, actual.size());
    }

    @Test
    void testgetFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc() {
        FlowLogWithoutPayload log1 = mock(FlowLogWithoutPayload.class);
        when(flowLogRepository.findAllWithoutPayloadByChainIdsCreatedDesc(Set.of("a"))).thenReturn(List.of(log1));
        List<FlowLogWithoutPayload> actual = underTest.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of("a"));
        assertEquals(1, actual.size());
    }

    @Test
    void testFlowLogsWithoutPayloadByFlowChainIdsCreatedDescNullChainId() {
        List<FlowLogWithoutPayload> actual = underTest.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(null);
        assertEquals(0, actual.size());
    }

    @Test
    void testFlowLogsWithoutPayloadByFlowChainIdsCreatedDescEmptyChainId() {
        List<FlowLogWithoutPayload> actual = underTest.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(new HashSet<>());
        assertEquals(0, actual.size());
    }

    @Test
    void testCloseFlowOnErrorWhenErrorFound() {
        FlowLog flowLog = new FlowLog(ID, FLOW_ID, "currentState", false, StateStatus.PENDING, OperationType.PROVISION);
        flowLog.setId(FLOW_LOG_ID);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));
        underTest.closeFlowOnError(FLOW_ID, null);
        verify(flowLogRepository, times(1)).findFirstByFlowIdOrderByCreatedDesc(eq(FLOW_ID));
        verify(applicationFlowInformation, times(1)).handleFlowFail(eq(flowLog));
        verify(flowLogRepository, times(1)).save(eq(flowLog));
        verify(flowLogRepository).findAllByFlowIdOrderByCreatedDesc(eq(FLOW_ID));
        verify(flowLogRepository).saveAll(anyList());
    }

    @Test
    void testCloseFlowOnErrorWhenHandlingFails() {
        FlowLog flowLog = new FlowLog(ID, FLOW_ID, "currentState", false, StateStatus.PENDING, OperationType.PROVISION);
        flowLog.setId(FLOW_LOG_ID);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));
        doThrow(new RuntimeException("Boom")).when(applicationFlowInformation).handleFlowFail(any());
        underTest.closeFlowOnError(FLOW_ID, null);
        verify(flowLogRepository, times(1)).findFirstByFlowIdOrderByCreatedDesc(eq(FLOW_ID));
        verify(applicationFlowInformation, times(1)).handleFlowFail(eq(flowLog));
        verify(flowLogRepository, times(1)).save(eq(flowLog));
        verify(flowLogRepository).findAllByFlowIdOrderByCreatedDesc(eq(FLOW_ID));
        verify(flowLogRepository).saveAll(anyList());
    }

    @Test
    void testCloseFlowOnErrorWhenFlowNotFound() {
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.empty());
        underTest.closeFlowOnError(FLOW_ID, null);
        verify(flowLogRepository, times(1)).findFirstByFlowIdOrderByCreatedDesc(eq(FLOW_ID));
        verify(applicationFlowInformation, never()).handleFlowFail(any());
        verify(flowLogRepository, never()).save(any());
        verify(flowLogRepository, never()).findAllByFlowIdOrderByCreatedDesc(anyString());
        verify(flowLogRepository, never()).saveAll(anyList());
    }

    public static class TerminationFlowConfig extends AbstractFlowConfiguration<MockFlowState, MockFlowEvent> {
        protected TerminationFlowConfig(Class<MockFlowState> stateType, Class<MockFlowEvent> eventType) {
            super(stateType, eventType);
        }

        @Override
        protected List<Transition<MockFlowState, MockFlowEvent>> getTransitions() {
            return null;
        }

        @Override
        public FlowEdgeConfig<MockFlowState, MockFlowEvent> getEdgeConfig() {
            return null;
        }

        @Override
        public MockFlowEvent[] getEvents() {
            return new MockFlowEvent[0];
        }

        @Override
        public MockFlowEvent[] getInitEvents() {
            return new MockFlowEvent[0];
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }

    public static class MockFlowState implements FlowState {

        @Override
        public String name() {
            return null;
        }

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }
    }

    public static class MockFlowEvent implements FlowEvent {
        @Override
        public String name() {
            return null;
        }

        @Override
        public String event() {
            return null;
        }
    }

    private static class TestSelectable implements Selectable {

        @Override
        public String selector() {
            return "selector";
        }

        @Override
        public Long getResourceId() {
            return ID;
        }
    }
}
