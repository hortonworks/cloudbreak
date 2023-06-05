package com.sequenceiq.flow.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.converter.ClassValueConverter;
import com.sequenceiq.flow.converter.FlowLogConverter;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
public class FlowServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 50);

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String FLOW_CHAIN_ID = "FLOW_CHAIN_ID";

    private static final String FLOW_ID = "FLOW_ID";

    private static final String NEXT_EVENT = "NEXT_EVENT";

    private static final String NO_NEXT_EVENT = null;

    private static final String FAIL_HANDLED_NEXT_EVENT = "FAIL_HANDLED_NEXT_EVENT";

    private static final String INTERMEDIATE_STATE = "INTERMEDIATE_STATE";

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private Set<String> failHandledEvents;

    @Mock
    private FlowProgressResponseConverter flowProgressResponseConverter;

    @Mock
    private FlowLogConverter flowLogConverter;

    @InjectMocks
    private FlowService underTest;

    @BeforeEach
    void setup() {
        lenient().when(flowLogConverter.convert(any())).thenReturn(new FlowLogResponse());
        lenient().when(failHandledEvents.contains(FAIL_HANDLED_NEXT_EVENT)).thenReturn(true);
    }

    @Test
    void testGetLastFlowById() {
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Optional.of(mock(FlowLog.class)));

        underTest.getLastFlowById(FLOW_ID);

        verify(flowLogDBService).findFirstByFlowIdOrderByCreatedDesc(anyString());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testGetLastFlowByIdException() {
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.getLastFlowById(FLOW_ID));

        Assertions.assertEquals(e.getMessage(), "Not found flow for this flow id!");
        verify(flowLogDBService).findFirstByFlowIdOrderByCreatedDesc(anyString());
    }

    @Test
    void testGetFlowLogsByFlowIdEmpty() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(List.of());

        Assertions.assertEquals(0, underTest.getFlowLogsByFlowId(FLOW_ID).size());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
    }

    @Test
    void testGetFlowLogsByFlowId() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(List.of(new FlowLog()));

        Assertions.assertEquals(1, underTest.getFlowLogsByFlowId(FLOW_ID).size());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testGetLastFlowByResourceNameInvalidInput() {
        assertThrows(IllegalStateException.class, () -> underTest.getLastFlowByResourceName(Crn.fromString(STACK_CRN).toString()));
    }

    @Test
    public void testGetLastFlowByResourceName() {
        when(flowLogDBService.getLastFlowLogByResourceCrnOrName(anyString())).thenReturn(new FlowLog());

        underTest.getLastFlowByResourceName("myLittleSdx");

        verify(flowLogDBService).getLastFlowLogByResourceCrnOrName(anyString());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testGetFlowLogsByResourceNameInvalidInput() {
        assertThrows(IllegalStateException.class, () -> underTest.getFlowLogsByResourceName(Crn.fromString(STACK_CRN).toString()));
    }

    @Test
    void testGetFlowLogsByResourceName() {
        when(flowLogDBService.getFlowLogsByResourceCrnOrName(anyString())).thenReturn(List.of(new FlowLog()));

        underTest.getFlowLogsByResourceName("myLittleSdx");

        verify(flowLogDBService).getFlowLogsByResourceCrnOrName(anyString());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testGetLastFlowByResourceCrnInvalidInput() {
        assertThrows(IllegalStateException.class, () -> underTest.getLastFlowByResourceCrn("myLittleSdx"));
    }

    @Test
    void testGetLastFlowByResourceCrn() {
        when(flowLogDBService.getLastFlowLogByResourceCrnOrName(anyString())).thenReturn(new FlowLog());

        underTest.getLastFlowByResourceCrn(Crn.fromString(STACK_CRN).toString());

        verify(flowLogDBService).getLastFlowLogByResourceCrnOrName(anyString());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testGetFlowLogsByResourceCrnInvalidInput() {
        assertThrows(IllegalStateException.class, () -> underTest.getFlowLogsByResourceCrn("myLittleSdx"));
    }

    @Test
    void testGetFlowLogsByResourceCrn() {
        when(flowLogDBService.getFlowLogsByResourceCrnOrName(anyString())).thenReturn(List.of(new FlowLog()));

        underTest.getFlowLogsByResourceCrn(Crn.fromString(STACK_CRN).toString());

        verify(flowLogDBService).getFlowLogsByResourceCrnOrName(anyString());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testHasFlowRunningByFlowId() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        Assertions.assertTrue(flowCheckResponse.getHasActiveFlow());
        Assertions.assertNotNull(flowCheckResponse.getFlowId());

        verify(flowLogDBService).findAllWithoutPayloadByFlowIdOrderByCreatedDesc(anyString());
        verifyNoMoreInteractions(flowChainLogService);
    }

    @Test
    void testNoFlowRunningByFlowId() {
        setUpFlow(FLOW_ID, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        Assertions.assertFalse(flowCheckResponse.getHasActiveFlow());
        Assertions.assertNotNull(flowCheckResponse.getFlowId());

        verify(flowLogDBService).findAllWithoutPayloadByFlowIdOrderByCreatedDesc(anyString());
        verify(flowChainLogService).hasEventInFlowChainQueue(List.of());
    }

    @Test
    void testFlowRunningByWrongFlowId() {
        setUpFlow(FLOW_ID, Collections.emptyList());

        NotFoundException e = assertThrows(NotFoundException.class, () -> underTest.getFlowState(FLOW_ID));
        Assertions.assertEquals("Flow 'FLOW_ID' not found.", e.getMessage());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithAllFieldsFilledByFlowChain() {
        FlowLogWithoutPayload latestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(latestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_FIRST");
        lenient().when(latestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_FIRST");
        ClassValueConverter classValueConverter = new ClassValueConverter();
        ClassValue classValue = classValueConverter.convertToEntityAttribute("FLOW_TYPE_FIRST");
        lenient().when(latestFlowLog.getFlowType()).thenReturn(classValue);

        FlowLogWithoutPayload secondToLatestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(secondToLatestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_SECOND");
        lenient().when(secondToLatestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_SECOND");
        ClassValueConverter classValueConverterSecond = new ClassValueConverter();
        ClassValue classValueSecond = classValueConverterSecond.convertToEntityAttribute("FLOW_TYPE_SECOND");
        lenient().when(secondToLatestFlowLog.getFlowType()).thenReturn(classValueSecond);

        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(latestFlowLog, secondToLatestFlowLog));
        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertEquals("CURRENT_STATE_FIRST", flowCheckResponse.getCurrentState());
        Assertions.assertEquals("NEXT_EVENT_FIRST", flowCheckResponse.getNextEvent());
        Assertions.assertEquals("FLOW_TYPE_FIRST", flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithAllFieldsFilledByFlow() {
        FlowLogWithoutPayload latestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(latestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_FIRST");
        lenient().when(latestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_FIRST");
        ClassValueConverter classValueConverter = new ClassValueConverter();
        ClassValue classValue = classValueConverter.convertToEntityAttribute("FLOW_TYPE_FIRST");
        lenient().when(latestFlowLog.getFlowType()).thenReturn(classValue);

        FlowLogWithoutPayload secondToLatestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(secondToLatestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_SECOND");
        lenient().when(secondToLatestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_SECOND");
        ClassValueConverter classValueConverterSecond = new ClassValueConverter();
        ClassValue classValueSecond = classValueConverterSecond.convertToEntityAttribute("FLOW_TYPE_SECOND");
        lenient().when(secondToLatestFlowLog.getFlowType()).thenReturn(classValueSecond);

        setUpFlow(FLOW_ID, List.of(latestFlowLog, secondToLatestFlowLog));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);

        Assertions.assertEquals("CURRENT_STATE_FIRST", flowCheckResponse.getCurrentState());
        Assertions.assertEquals("NEXT_EVENT_FIRST", flowCheckResponse.getNextEvent());
        Assertions.assertEquals("FLOW_TYPE_FIRST", flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithFlowTypeNullByFlow() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        Assertions.assertEquals(INTERMEDIATE_STATE, flowCheckResponse.getCurrentState());
        Assertions.assertEquals(NEXT_EVENT, flowCheckResponse.getNextEvent());
        Assertions.assertNull(flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNextEventAndFlowTypeNullByFlow() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NO_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        Assertions.assertEquals(INTERMEDIATE_STATE, flowCheckResponse.getCurrentState());
        Assertions.assertEquals(NO_NEXT_EVENT, flowCheckResponse.getNextEvent());
        Assertions.assertNull(flowCheckResponse.getNextEvent());
        Assertions.assertNull(flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNoFlowRunningByFlow() {
        setUpFlow(FLOW_ID, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        Assertions.assertNull(flowCheckResponse.getCurrentState());
        Assertions.assertNull(flowCheckResponse.getNextEvent());
        Assertions.assertNull(flowCheckResponse.getFlowType());
    }

    @Test
    void testCompletedFlowChain() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        Assertions.assertFalse(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testWhenFlowChainNotFound() {
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDescIdDesc(anyString())).thenReturn(List.of());

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertFalse(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        verify(flowChainLogService).findByFlowChainIdOrderByCreatedDescIdDesc(anyString());
        verifyNoMoreInteractions(flowLogDBService);
    }

    @Test
    void testRunningFlowChainWhenHasEventInQueue() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), true, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertTrue(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testRunningFlowChainWithoutFinishedState() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertTrue(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testFailedFlowChain() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));
        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertFalse(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testFailedAndRestartedFlowChain() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertTrue(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testCompletedAfterTwoRetryFlowChain() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 8, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 7, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 6, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 5, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertFalse(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testRunningFlowChainWithPendingFlowLog() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        Assertions.assertTrue(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNextEventValueWByFlowChain() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        Assertions.assertEquals(INTERMEDIATE_STATE, flowCheckResponse.getCurrentState());
        Assertions.assertEquals(FAIL_HANDLED_NEXT_EVENT, flowCheckResponse.getNextEvent());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNextEventNullByFlowChain() {
        setUpFlowChainForGetFlowChainState(flowChainLog(), false, List.of(
                pendingFlowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        Assertions.assertEquals(FlowConstants.FINISHED_STATE, flowCheckResponse.getCurrentState());
        Assertions.assertEquals(NO_NEXT_EVENT, flowCheckResponse.getNextEvent());
        Assertions.assertNull(flowCheckResponse.getNextEvent());
    }

    private void setUpFlow(String flowId, List<FlowLogWithoutPayload> flowLogs) {
        when(flowLogDBService.findAllWithoutPayloadByFlowIdOrderByCreatedDesc(flowId)).thenReturn(flowLogs);
        lenient().when(flowChainLogService.hasEventInFlowChainQueue(List.of())).thenReturn(false);
    }

    private void setUpFlowChain(FlowChainLog flowChainLog, boolean hasEventInQueue, List<FlowLogWithoutPayload> flowLogs) {
        List<FlowChainLog> chainLogs = List.of(flowChainLog);
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDesc(FLOW_CHAIN_ID)).thenReturn(chainLogs);
        lenient().when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(hasEventInQueue);
        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID))).thenReturn(flowLogs);
        when(flowChainLogService.getRelatedFlowChainLogs(chainLogs)).thenReturn(chainLogs);
    }

    private void setUpFlowChainForGetFlowChainState(FlowChainLog flowChainLog, boolean hasEventInQueue,
            List<FlowLogWithoutPayload> flowLogs) {
        List<FlowChainLog> chainLogs = List.of(flowChainLog);
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDescIdDesc(FLOW_CHAIN_ID)).thenReturn(chainLogs);
        lenient().when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(hasEventInQueue);
        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID))).thenReturn(flowLogs);
        when(flowChainLogService.getRelatedFlowChainLogs(chainLogs)).thenReturn(chainLogs);
    }

    private FlowChainLog flowChainLog() {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(FLOW_CHAIN_ID);
        return flowChainLog;
    }

    private FlowLogWithoutPayload flowLog(String currentState, String nextEvent, long created, String flowId) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        lenient().when(flowLog.getFinalized()).thenReturn(true);
        lenient().when(flowLog.getFlowId()).thenReturn(flowId);
        lenient().when(flowLog.getFlowChainId()).thenReturn(FLOW_CHAIN_ID);
        return flowLog;
    }

    private FlowLogWithoutPayload flowLog(String currentState, String nextEvent, long created, String flowId, Long endTime) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        lenient().when(flowLog.getFinalized()).thenReturn(true);
        lenient().when(flowLog.getFlowId()).thenReturn(flowId);
        lenient().when(flowLog.getEndTime()).thenReturn(endTime);
        return flowLog;
    }

    private FlowLogWithoutPayload flowLog(String currentState, String nextEvent, long created, String flowId,
            Long endTime, String chainId) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        lenient().when(flowLog.getFinalized()).thenReturn(true);
        lenient().when(flowLog.getFlowId()).thenReturn(flowId);
        lenient().when(flowLog.getFlowChainId()).thenReturn(chainId);
        return flowLog;
    }

    private FlowLogWithoutPayload pendingFlowLog(String currentState, String nextEvent, long created, String flowId) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.PENDING);
        lenient().when(flowLog.getFinalized()).thenReturn(false);
        lenient().when(flowLog.getFlowId()).thenReturn(flowId);
        lenient().when(flowLog.getFlowChainId()).thenReturn(FLOW_CHAIN_ID);
        return flowLog;
    }

    @Test
    void testCompletedFlowChainEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123", 3L),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123", 2L));
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);
        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        Assertions.assertEquals(3L, flowCheckResponse.getEndTime().longValue());
    }

    @Test
    void testWhenFlowChainNotFoundEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of();
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        Assertions.assertNull(flowCheckResponse.getEndTime());
    }

    @Test
    void testActiveFlowChainEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123"));
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(true);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        Assertions.assertTrue(flowCheckResponse.getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testCompletedFlowChainWithNullEndTimeForFlowsEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, "123", null),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123", null));
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        Assertions.assertNull(flowCheckResponse.getEndTime());
    }

    @Test
    void testCompletedFlowChainWithNullFlowsEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of();
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        Assertions.assertNull(flowCheckResponse.getEndTime());
    }

    @Test
    void testGetFlowLogsByIdsEmpty() {
        when(flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(anySet(), any())).thenReturn(new PageImpl<>(List.of()));

        Assertions.assertEquals(0, underTest.getFlowLogsByIds(List.of(FLOW_ID), PAGEABLE).getContent().size());

        verify(flowLogDBService).getFlowLogsByFlowIdsCreatedDesc(anySet(), any());
    }

    @Test
    void testGetFlowLogsByIds() {
        when(flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(anySet(), any())).thenReturn(new PageImpl<>(List.of(new FlowLog())));

        Assertions.assertEquals(1, underTest.getFlowLogsByIds(List.of(FLOW_ID), PAGEABLE).getContent().size());

        verify(flowLogDBService).getFlowLogsByFlowIdsCreatedDesc(anySet(), any());
        verify(flowLogConverter).convert(any());
    }

    private void setUpFlowChains(FlowChainLog flowChainLog, boolean hasEventInQueue, List<FlowLogWithoutPayload> flowLogs) {
        List<FlowChainLog> chainLogs = List.of(flowChainLog);
        when(flowChainLogService.findAllByFlowChainIdInOrderByCreatedDesc(Set.of(FLOW_CHAIN_ID), PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(chainLogs));
        lenient().when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(hasEventInQueue);
        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID))).thenReturn(flowLogs);
    }

    @Test
    void testCompletedFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);
        Assertions.assertFalse(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testWhenFlowChainsNotFound() {
        when(flowChainLogService.findAllByFlowChainIdInOrderByCreatedDesc(anySet(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertEquals(0, flowCheckResponse.getContent().size());
        verify(flowChainLogService).findAllByFlowChainIdInOrderByCreatedDesc(anySet(), any());
        verify(flowLogDBService).getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(new HashSet<>());
    }

    @Test
    void testRunningFlowChainsWhenHasEventInQueue() {
        setUpFlowChains(flowChainLog(), true, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testRunningFlowChainsWithoutFinishedState() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testFailedFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));
        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertFalse(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testFailedAndRestartedFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testCompletedAfterTwoRetryFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 8, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 7, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 6, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 5, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertFalse(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testRunningFlowChainsWithPendingFlowLog() {
        setUpFlowChains(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4, "123"),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, "123"),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testRunningFlowChainsWithChildChains() {
        setUpFlowChainsWithChildren(flowChainLog(), flowChainLogChild(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123", null, FLOW_CHAIN_ID + "_CHILD"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123", 1L, FLOW_CHAIN_ID + "_CHILD")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
        Assertions.assertNull(flowCheckResponse.getContent().get(0).getEndTime());
    }

    private void setUpFlowChainsWithChildren(FlowChainLog flowChainLog, FlowChainLog flowChainLogChild,
            boolean hasEventInQueue, List<FlowLogWithoutPayload> flowLogs) {
        List<FlowChainLog> chainLogs = List.of(flowChainLog);
        List<FlowChainLog> childChainLogs = new ArrayList<>();
        childChainLogs.add(flowChainLogChild);
        when(flowChainLogService.findAllByFlowChainIdInOrderByCreatedDesc(Set.of(FLOW_CHAIN_ID),
                PageRequest.of(0, 50))).thenReturn(new PageImpl<>(chainLogs));
        when(flowChainLogService.getRelatedFlowChainLogs(chainLogs)).thenReturn(childChainLogs);
        lenient().when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(hasEventInQueue);
        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID, FLOW_CHAIN_ID + "_CHILD"))).thenReturn(flowLogs);
    }

    private FlowChainLog flowChainLogChild() {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(FLOW_CHAIN_ID + "_CHILD");
        flowChainLog.setParentFlowChainId(FLOW_CHAIN_ID);
        return flowChainLog;
    }

    @Test
    void testRunningFlowChainsWithMultipleChildChains() {
        List<FlowChainLog> chainLogs = List.of(flowChainLog());

        FlowChainLog childFlowChainLog1 = new FlowChainLog();
        childFlowChainLog1.setFlowChainId(FLOW_CHAIN_ID + "_CHILD_1");
        childFlowChainLog1.setParentFlowChainId(FLOW_CHAIN_ID);

        FlowChainLog childFlowChainLog2 = new FlowChainLog();
        childFlowChainLog2.setFlowChainId(FLOW_CHAIN_ID + "_CHILD_2");
        childFlowChainLog2.setParentFlowChainId(FLOW_CHAIN_ID);

        List<FlowChainLog> childChainLogs = List.of(childFlowChainLog1, childFlowChainLog2);

        when(flowChainLogService.findAllByFlowChainIdInOrderByCreatedDesc(Set.of(FLOW_CHAIN_ID),
                PageRequest.of(0, 50))).thenReturn(new PageImpl<>(chainLogs));

        when(flowChainLogService.getRelatedFlowChainLogs(chainLogs)).thenReturn(childChainLogs);

        lenient().when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(false);

        List<FlowLogWithoutPayload> flowLogs = List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123", null, FLOW_CHAIN_ID + "_CHILD_1"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123", 1L, FLOW_CHAIN_ID + "_CHILD_1"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, "123", null, FLOW_CHAIN_ID + "_CHILD_2"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, "123", 1L, FLOW_CHAIN_ID + "_CHILD_2"));

        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID,
                FLOW_CHAIN_ID + "_CHILD_1", FLOW_CHAIN_ID + "_CHILD_2"))).thenReturn(flowLogs);

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        Assertions.assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        Assertions.assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
        Assertions.assertNull(flowCheckResponse.getContent().get(0).getEndTime());
    }
}
