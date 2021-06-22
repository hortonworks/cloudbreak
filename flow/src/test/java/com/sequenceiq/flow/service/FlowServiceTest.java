package com.sequenceiq.flow.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@RunWith(MockitoJUnitRunner.class)
public class FlowServiceTest {

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String FLOW_CHAIN_ID = "FLOW_CHAIN_ID";

    private static final String FLOW_ID = "FLOW_ID";

    private static final String NEXT_EVENT = "NEXT_EVENT";

    private static final String NO_NEXT_EVENT = null;

    private static final String FAIL_HANDLED_NEXT_EVENT = "FAIL_HANDLED_NEXT_EVENT";

    private static final String INTERMEDIATE_STATE = "INTERMEDIATE_STATE";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private Set<String> failHandledEvents;

    @InjectMocks
    private FlowService underTest;

    @Before
    public void setup() {
        when(conversionService.convert(any(), eq(FlowLogResponse.class))).thenReturn(new FlowLogResponse());
        lenient().when(failHandledEvents.contains(FAIL_HANDLED_NEXT_EVENT)).thenReturn(true);
    }

    @Test
    public void testGetLastFlowById() {
        when(flowLogDBService.getLastFlowLog(anyString())).thenReturn(Optional.of(new FlowLog()));

        underTest.getLastFlowById(FLOW_ID);

        verify(flowLogDBService).getLastFlowLog(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetLastFlowByIdException() {
        when(flowLogDBService.getLastFlowLog(anyString())).thenReturn(Optional.empty());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Not found flow for this flow id!");

        underTest.getLastFlowById(FLOW_ID);

        verify(flowLogDBService).getLastFlowLog(anyString());
        verifyZeroInteractions(conversionService);
    }

    @Test
    public void testGetFlowLogsByFlowIdEmpty() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(List.of());

        assertEquals(0, underTest.getFlowLogsByFlowId(FLOW_ID).size());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
        verifyZeroInteractions(conversionService);
    }

    @Test
    public void testGetFlowLogsByFlowId() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(List.of(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByFlowId(FLOW_ID).size());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetLastFlowByResourceNameInvalidInput() {
        thrown.expect(IllegalStateException.class);
        underTest.getLastFlowByResourceName(Crn.fromString(STACK_CRN).toString());
    }

    @Test
    public void testGetLastFlowByResourceName() {
        when(flowLogDBService.getLastFlowLogByResourceCrnOrName(anyString())).thenReturn(new FlowLog());

        underTest.getLastFlowByResourceName("myLittleSdx");

        verify(flowLogDBService).getLastFlowLogByResourceCrnOrName(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetFlowLogsByResourceNameInvalidInput() {
        thrown.expect(IllegalStateException.class);
        underTest.getFlowLogsByResourceName(Crn.fromString(STACK_CRN).toString());
    }

    @Test
    public void testGetFlowLogsByResourceName() {
        when(flowLogDBService.getFlowLogsByResourceCrnOrName(anyString())).thenReturn(List.of(new FlowLog()));

        underTest.getFlowLogsByResourceName("myLittleSdx");

        verify(flowLogDBService).getFlowLogsByResourceCrnOrName(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetLastFlowByResourceCrnInvalidInput() {
        thrown.expect(IllegalStateException.class);
        underTest.getLastFlowByResourceCrn("myLittleSdx");
    }

    @Test
    public void testGetLastFlowByResourceCrn() {
        when(flowLogDBService.getLastFlowLogByResourceCrnOrName(anyString())).thenReturn(new FlowLog());

        underTest.getLastFlowByResourceCrn(Crn.fromString(STACK_CRN).toString());

        verify(flowLogDBService).getLastFlowLogByResourceCrnOrName(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetFlowLogsByResourceCrnInvalidInput() {
        thrown.expect(IllegalStateException.class);
        underTest.getFlowLogsByResourceCrn("myLittleSdx");
    }

    @Test
    public void testGetFlowLogsByResourceCrn() {
        when(flowLogDBService.getFlowLogsByResourceCrnOrName(anyString())).thenReturn(List.of(new FlowLog()));

        underTest.getFlowLogsByResourceCrn(Crn.fromString(STACK_CRN).toString());

        verify(flowLogDBService).getFlowLogsByResourceCrnOrName(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testHasFlowRunningByFlowId() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertNotNull(flowCheckResponse.getFlowId());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
        verifyZeroInteractions(flowChainLogService);
    }

    @Test
    public void testNoFlowRunningByFlowId() {
        setUpFlow(FLOW_ID, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertNotNull(flowCheckResponse.getFlowId());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
        verify(flowChainLogService).hasEventInFlowChainQueue(List.of());
    }

    @Test
    public void testCompletedFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    public void testWhenFlowChainNotFound() {
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDesc(anyString())).thenReturn(List.of());

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        verify(flowChainLogService).findByFlowChainIdOrderByCreatedDesc(anyString());
        verifyZeroInteractions(flowLogDBService, conversionService);
    }

    @Test
    public void testRunningFlowChainWhenHasEventInQueue() {
        setUpFlowChain(flowChainLog(), true, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    public void testRunningFlowChainWithoutFinishedState() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    public void testFailedFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    public void testFailedAndRestartedFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    public void testCompletedAfterTwoRetryFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 8),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 7),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 6),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 5),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    public void testRunningFlowChainWithPendingFlowLog() {
        setUpFlowChain(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    private void setUpFlow(String flowId, List<FlowLog> flowLogs) {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId)).thenReturn(flowLogs);
        when(flowChainLogService.hasEventInFlowChainQueue(List.of())).thenReturn(false);
    }

    private void setUpFlowChain(FlowChainLog flowChainLog, boolean hasEventInQueue, List<FlowLog> flowLogs) {
        List<FlowChainLog> chainLogs = List.of(flowChainLog);
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDesc(FLOW_CHAIN_ID))
                .thenReturn(chainLogs);
        when(flowLogDBService.getFlowIdsByChainIds(Set.of(FLOW_CHAIN_ID))).thenReturn(Set.of(FLOW_ID));
        when(flowChainLogService.collectRelatedFlowChains(flowChainLog)).thenReturn(chainLogs);
        when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(hasEventInQueue);
        when(flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(Set.of(FLOW_ID)))
                .thenReturn(flowLogs);
    }

    private FlowChainLog flowChainLog() {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(FLOW_CHAIN_ID);
        return flowChainLog;
    }

    private FlowLog flowLog(String currentState, String nextEvent, long created) {
        FlowLog flowLog = new FlowLog();
        flowLog.setCurrentState(currentState);
        flowLog.setNextEvent(nextEvent);
        flowLog.setCreated(created);
        flowLog.setStateStatus(StateStatus.SUCCESSFUL);
        flowLog.setFinalized(true);
        return flowLog;
    }

    private FlowLog pendingFlowLog(String currentState, String nextEvent, long created) {
        FlowLog flowLog = new FlowLog();
        flowLog.setCurrentState(currentState);
        flowLog.setNextEvent(nextEvent);
        flowLog.setCreated(created);
        flowLog.setStateStatus(StateStatus.PENDING);
        flowLog.setFinalized(false);
        return flowLog;
    }
}
