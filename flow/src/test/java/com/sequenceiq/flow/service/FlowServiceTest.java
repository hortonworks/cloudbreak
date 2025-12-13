package com.sequenceiq.flow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
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

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.converter.ClassValueConverter;
import com.sequenceiq.flow.converter.FlowLogConverter;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.config.FlowConfiguration;
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

    private static final Long STACK_ID = 1L;

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:account1:cluster:cluster1";

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

    @Mock
    private ResourceIdProvider resourceIdProvider;

    @Spy
    private List<FlowConfiguration<?>> flowConfigurations = List.of();

    @Spy
    private List<FlowEventChainFactory<?>> flowEventChainFactories = List.of();

    @InjectMocks
    @Spy
    private FlowService underTest;

    @Mock
    private FlowCheckResponse flowCheckResponse;

    @BeforeEach
    void setup() {
        lenient().when(flowLogConverter.convert(any())).thenReturn(new FlowLogResponse());
        lenient().when(failHandledEvents.contains(FAIL_HANDLED_NEXT_EVENT)).thenReturn(true);
        lenient().when(resourceIdProvider.getResourceIdByResourceCrn(RESOURCE_CRN)).thenReturn(STACK_ID);
    }

    @Test
    void testPreviousFlowFailedFailed() {
        when(underTest.getFlowChainStateSafe(List.of(STACK_ID), FLOW_CHAIN_ID)).thenReturn(flowCheckResponse);
        when(flowCheckResponse.getHasActiveFlow()).thenReturn(false);
        when(flowCheckResponse.getLatestFlowFinalizedAndFailed()).thenReturn(true);

        boolean result = underTest.isPreviousFlowFailed(STACK_ID, FLOW_CHAIN_ID);

        assertTrue(result);
    }

    @Test
    void testPreviousFlowFailedSuccess() {
        when(underTest.getFlowChainStateSafe(List.of(STACK_ID), FLOW_CHAIN_ID)).thenReturn(flowCheckResponse);
        when(flowCheckResponse.getHasActiveFlow()).thenReturn(false);
        when(flowCheckResponse.getLatestFlowFinalizedAndFailed()).thenReturn(false);

        boolean result = underTest.isPreviousFlowFailed(STACK_ID, FLOW_CHAIN_ID);

        assertFalse(result);
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

        assertEquals(e.getMessage(), "Not found flow for this flow id!");
        verify(flowLogDBService).findFirstByFlowIdOrderByCreatedDesc(anyString());
    }

    @Test
    void testGetFlowLogsByFlowIdEmpty() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(List.of());

        assertEquals(0, underTest.getFlowLogsByFlowId(FLOW_ID).size());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
    }

    @Test
    void testGetFlowLogsByFlowId() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(List.of(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByFlowId(FLOW_ID).size());

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
    void testGetAllFlowLogsByResourceCrnAndFlowTypes() {
        when(flowLogDBService.getAllFlowLogsByResourceCrnOrNameAndFlowTypes(anyString(), anyList())).thenReturn(List.of(new FlowLog()));

        underTest.getAllFlowLogsByResourceCrnAndFlowTypes(Crn.fromString(STACK_CRN).toString(), Collections.emptyList());

        verify(flowLogDBService).getAllFlowLogsByResourceCrnOrNameAndFlowTypes(anyString(), anyList());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testHasFlowRunningByFlowId() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertNotNull(flowCheckResponse.getFlowId());

        verify(flowLogDBService).findAllWithoutPayloadByFlowIdOrderByCreatedDesc(anyString());
        verifyNoMoreInteractions(flowChainLogService);
    }

    @Test
    void testNoFlowRunningByFlowId() {
        setUpFlow(FLOW_ID, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertNotNull(flowCheckResponse.getFlowId());

        verify(flowLogDBService).findAllWithoutPayloadByFlowIdOrderByCreatedDesc(anyString());
        verify(flowChainLogService).hasEventInFlowChainQueue(List.of());
    }

    @Test
    void testFlowRunningByWrongFlowId() {
        setUpFlow(FLOW_ID, Collections.emptyList());

        NotFoundException e = assertThrows(NotFoundException.class, () -> underTest.getFlowState(FLOW_ID));
        assertEquals("Flow 'FLOW_ID' not found.", e.getMessage());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithAllFieldsFilledByFlowChain() {
        FlowLogWithoutPayload latestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(latestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_FIRST");
        lenient().when(latestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_FIRST");
        lenient().when(latestFlowLog.getFlowId()).thenReturn("FLOW_ID");
        ClassValueConverter classValueConverter = new ClassValueConverter();
        ClassValue classValue = classValueConverter.convertToEntityAttribute("FLOW_TYPE_FIRST");
        lenient().when(latestFlowLog.getFlowType()).thenReturn(classValue);

        FlowLogWithoutPayload secondToLatestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(secondToLatestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_SECOND");
        lenient().when(secondToLatestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_SECOND");
        lenient().when(secondToLatestFlowLog.getFlowId()).thenReturn("FLOW_ID");
        ClassValueConverter classValueConverterSecond = new ClassValueConverter();
        ClassValue classValueSecond = classValueConverterSecond.convertToEntityAttribute("FLOW_TYPE_SECOND");
        lenient().when(secondToLatestFlowLog.getFlowType()).thenReturn(classValueSecond);

        setUpFlowChain(flowChainLog(), false, List.of(latestFlowLog, secondToLatestFlowLog));
        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertEquals("CURRENT_STATE_FIRST", flowCheckResponse.getCurrentState());
        assertEquals("NEXT_EVENT_FIRST", flowCheckResponse.getNextEvent());
        assertEquals("FLOW_TYPE_FIRST", flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithAllFieldsFilledByFlow() {
        FlowLogWithoutPayload latestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(latestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_FIRST");
        lenient().when(latestFlowLog.getFlowId()).thenReturn("FLOW_ID");
        lenient().when(latestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_FIRST");
        ClassValueConverter classValueConverter = new ClassValueConverter();
        ClassValue classValue = classValueConverter.convertToEntityAttribute("FLOW_TYPE_FIRST");
        lenient().when(latestFlowLog.getFlowType()).thenReturn(classValue);

        FlowLogWithoutPayload secondToLatestFlowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(secondToLatestFlowLog.getCurrentState()).thenReturn("CURRENT_STATE_SECOND");
        lenient().when(secondToLatestFlowLog.getNextEvent()).thenReturn("NEXT_EVENT_SECOND");
        lenient().when(secondToLatestFlowLog.getFlowId()).thenReturn("FLOW_ID");
        ClassValueConverter classValueConverterSecond = new ClassValueConverter();
        ClassValue classValueSecond = classValueConverterSecond.convertToEntityAttribute("FLOW_TYPE_SECOND");
        lenient().when(secondToLatestFlowLog.getFlowType()).thenReturn(classValueSecond);

        setUpFlow(FLOW_ID, List.of(latestFlowLog, secondToLatestFlowLog));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);

        assertEquals("CURRENT_STATE_FIRST", flowCheckResponse.getCurrentState());
        assertEquals("NEXT_EVENT_FIRST", flowCheckResponse.getNextEvent());
        assertEquals("FLOW_TYPE_FIRST", flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithFlowTypeNullByFlow() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertEquals(INTERMEDIATE_STATE, flowCheckResponse.getCurrentState());
        assertEquals(NEXT_EVENT, flowCheckResponse.getNextEvent());
        assertNull(flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNextEventAndFlowTypeNullByFlow() {
        setUpFlow(FLOW_ID, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertEquals(INTERMEDIATE_STATE, flowCheckResponse.getCurrentState());
        assertEquals(NO_NEXT_EVENT, flowCheckResponse.getNextEvent());
        assertNull(flowCheckResponse.getNextEvent());
        assertNull(flowCheckResponse.getFlowType());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNoFlowRunningByFlow() {
        setUpFlow(FLOW_ID, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)
        ));

        FlowCheckResponse flowCheckResponse = underTest.getFlowState(FLOW_ID);
        assertNull(flowCheckResponse.getCurrentState());
        assertNull(flowCheckResponse.getNextEvent());
        assertNull(flowCheckResponse.getFlowType());
    }

    @Test
    void testCompletedFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testWhenFlowChainNotFound() {
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDesc(anyString())).thenReturn(List.of());

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        verify(flowChainLogService).findByFlowChainIdOrderByCreatedDesc(anyString());
        verifyNoMoreInteractions(flowLogDBService);
    }

    @Test
    void testRunningFlowChainWhenHasEventInQueue() {
        setUpFlowChain(flowChainLog(), true, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testRunningFlowChainWithoutFinishedState() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testFailedFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));
        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertFalse(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testFailedAndRestartedFlowChain() {
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
    void testCompletedAfterTwoRetryFlowChain() {
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
    void testRunningFlowChainWithPendingFlowLog() {
        setUpFlowChain(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNextEventValueWByFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        assertEquals(INTERMEDIATE_STATE, flowCheckResponse.getCurrentState());
        assertEquals(FAIL_HANDLED_NEXT_EVENT, flowCheckResponse.getNextEvent());
    }

    @Test
    void testGetLatestKnownFlowCheckResponseWithNextEventNullByFlowChain() {
        setUpFlowChain(flowChainLog(), false, List.of(
                pendingFlowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        FlowCheckResponse flowCheckResponse = underTest.getFlowChainState(FLOW_CHAIN_ID);
        assertEquals(FlowConstants.FINISHED_STATE, flowCheckResponse.getCurrentState());
        assertEquals(NO_NEXT_EVENT, flowCheckResponse.getNextEvent());
        assertNull(flowCheckResponse.getNextEvent());
    }

    @Test
    void testCompletedFlowChainEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, 3L),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, 2L));
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);
        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        assertEquals(3L, flowCheckResponse.getEndTime().longValue());
    }

    @Test
    void testWhenFlowChainNotFoundEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of();
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        assertNull(flowCheckResponse.getEndTime());
    }

    @Test
    void testActiveFlowChainEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1));
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(true);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        assertTrue(flowCheckResponse.getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
    }

    @Test
    void testCompletedFlowChainWithNullEndTimeForFlowsEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2, null),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, null));
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        assertNull(flowCheckResponse.getEndTime());
    }

    @Test
    void testCompletedFlowChainWithNullFlowsEndTime() {
        List<FlowLogWithoutPayload> relatedFlowLogs = List.of();
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(false);

        underTest.setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);

        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getFlowChainId());
        assertNull(flowCheckResponse.getEndTime());
    }

    @Test
    void testGetFlowLogsByIdsEmpty() {
        when(flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(anySet(), any())).thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, underTest.getFlowLogsByIds(List.of(FLOW_ID), PAGEABLE).getContent().size());

        verify(flowLogDBService).getFlowLogsByFlowIdsCreatedDesc(anySet(), any());
    }

    @Test
    void testGetFlowLogsByIds() {
        when(flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(anySet(), any())).thenReturn(new PageImpl<>(List.of(new FlowLog())));

        assertEquals(1, underTest.getFlowLogsByIds(List.of(FLOW_ID), PAGEABLE).getContent().size());

        verify(flowLogDBService).getFlowLogsByFlowIdsCreatedDesc(anySet(), any());
        verify(flowLogConverter).convert(any());
    }

    @Test
    void testCompletedFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);
        assertFalse(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testWhenFlowChainsNotFound() {
        when(flowChainLogService.findAllByFlowChainIdInOrderByCreatedDesc(anySet(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertEquals(0, flowCheckResponse.getContent().size());
        verify(flowChainLogService).findAllByFlowChainIdInOrderByCreatedDesc(anySet(), any());
        verify(flowLogDBService).getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(new HashSet<>());
    }

    @Test
    void testRunningFlowChainsWhenHasEventInQueue() {
        setUpFlowChains(flowChainLog(), true, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testRunningFlowChainsWithoutFinishedState() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testFailedFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));
        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertFalse(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testFailedAndRestartedFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testCompletedAfterTwoRetryFlowChains() {
        setUpFlowChains(flowChainLog(), false, List.of(
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 8),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 7),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 6),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 5),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertFalse(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testRunningFlowChainsWithPendingFlowLog() {
        setUpFlowChains(flowChainLog(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 4),
                flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3),
                flowLog(INTERMEDIATE_STATE, FAIL_HANDLED_NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1)));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
    }

    @Test
    void testRunningFlowChainsWithChildChains() {
        setUpFlowChainsWithChildren(flowChainLog(), flowChainLogChild(), false, List.of(
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, null, FLOW_CHAIN_ID + "_CHILD"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, 1L, FLOW_CHAIN_ID + "_CHILD")));

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
        assertNull(flowCheckResponse.getContent().get(0).getEndTime());
    }

    @Test
    public void testGetFlowOperationWhenOperationIsFlowAndCompleted() {
        setUpCompletedFlow();

        OperationStatusResponse operation = underTest.getOperationStatus(RESOURCE_CRN, FLOW_ID);

        assertEquals(FLOW_ID, operation.getOperationId());
        assertEquals("CreateDataHub", operation.getOperationName());
        assertEquals(OperationProgressStatus.FINISHED, operation.getOperationStatus());
    }

    @Test
    public void testGetFlowOperationWhenOperationIsFlowAndNotCompleted() {
        setUpRunningFlow();

        OperationStatusResponse operation = underTest.getOperationStatus(RESOURCE_CRN, FLOW_ID);

        assertEquals(FLOW_ID, operation.getOperationId());
        assertEquals("CreateDataHub", operation.getOperationName());
        assertEquals(OperationProgressStatus.RUNNING, operation.getOperationStatus());
    }

    @Test
    public void testGetFlowOperationWhenOperationIsFlowChainAndCompleted() {
        setUpCompletedFlowChain();

        OperationStatusResponse operation = underTest.getOperationStatus(RESOURCE_CRN, FLOW_CHAIN_ID);

        assertEquals(FLOW_CHAIN_ID, operation.getOperationId());
        assertEquals("UpgradeDataHub", operation.getOperationName());
        assertEquals(OperationProgressStatus.FINISHED, operation.getOperationStatus());
    }

    @Test
    public void testGetFlowOperationWhenOperationIsFlowChainAndNotCompleted() {
        setUpRunningFlowChain();

        OperationStatusResponse operation = underTest.getOperationStatus(RESOURCE_CRN, FLOW_CHAIN_ID);

        assertEquals(FLOW_CHAIN_ID, operation.getOperationId());
        assertEquals("UpgradeDataHub", operation.getOperationName());
        assertEquals(OperationProgressStatus.RUNNING, operation.getOperationStatus());
    }

    @Test
    public void testGetLastFlowOperationWhenOperationIsFlow() {
        FlowLog lastFlow = new FlowLog();
        lastFlow.setFlowId(FLOW_ID);
        lastFlow.setFlowChainId(null);
        when(flowLogDBService.getLastFlowLog(anyLong())).thenReturn(Optional.of(lastFlow));
        setUpCompletedFlow();

        OperationStatusResponse operation = underTest.getOperationStatus(RESOURCE_CRN, null);

        assertEquals(FLOW_ID, operation.getOperationId());
        assertEquals("CreateDataHub", operation.getOperationName());
        assertEquals(OperationProgressStatus.FINISHED, operation.getOperationStatus());
    }

    @Test
    public void testGetLastFlowOperationWhenOperationIsFlowCHain() {
        FlowLog lastFlow = new FlowLog();
        lastFlow.setFlowId(FLOW_ID);
        lastFlow.setFlowChainId(FLOW_CHAIN_ID);
        when(flowLogDBService.getLastFlowLog(anyLong())).thenReturn(Optional.of(lastFlow));
        setUpCompletedFlowChain();

        OperationStatusResponse operation = underTest.getOperationStatus(RESOURCE_CRN, null);

        assertEquals(FLOW_CHAIN_ID, operation.getOperationId());
        assertEquals("UpgradeDataHub", operation.getOperationName());
        assertEquals(OperationProgressStatus.FINISHED, operation.getOperationStatus());
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
                pendingFlowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, null, FLOW_CHAIN_ID + "_CHILD_1"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, 1L, FLOW_CHAIN_ID + "_CHILD_1"),
                flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, null, FLOW_CHAIN_ID + "_CHILD_2"),
                flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, 1L, FLOW_CHAIN_ID + "_CHILD_2"));

        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID,
                FLOW_CHAIN_ID + "_CHILD_1", FLOW_CHAIN_ID + "_CHILD_2"))).thenReturn(flowLogs);

        Page<FlowCheckResponse> flowCheckResponse = underTest.getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);

        assertTrue(flowCheckResponse.getContent().get(0).getHasActiveFlow());
        assertEquals(FLOW_CHAIN_ID, flowCheckResponse.getContent().get(0).getFlowChainId());
        assertNull(flowCheckResponse.getContent().get(0).getEndTime());
    }

    private void setUpRunningFlow() {
        setUpFlow(false);
    }

    private void setUpCompletedFlow() {
        setUpFlow(true);
    }

    private void setUpFlow(boolean completed) {
        List<FlowLogWithoutPayload> flowLogs = new ArrayList<>();
        if (completed) {
            flowLogs.add(flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, 3L, null));
        }
        flowLogs.add(flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, completed ? 2L : null, null));
        flowLogs.add(flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, 1L, null));
        setUpFlow(FLOW_ID, flowLogs);
    }

    private void setUpRunningFlowChain() {
        setUpFlowChain(false);
    }

    private void setUpCompletedFlowChain() {
        setUpFlowChain(true);
    }

    private void setUpFlowChain(boolean completed) {
        List<FlowLogWithoutPayload> flowLogs = new ArrayList<>();
        if (completed) {
            flowLogs.add(flowLog(FlowConstants.FINISHED_STATE, NO_NEXT_EVENT, 3, 3L, FLOW_CHAIN_ID));
        }
        flowLogs.add(flowLog(INTERMEDIATE_STATE, NEXT_EVENT, 2, completed ? 2L : null, FLOW_CHAIN_ID));
        flowLogs.add(flowLog(FlowConstants.INIT_STATE, NEXT_EVENT, 1, 1L, FLOW_CHAIN_ID));
        setUpFlowChain(flowChainLog(), false, flowLogs);
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

    private void setUpFlowChains(FlowChainLog flowChainLog, boolean hasEventInQueue, List<FlowLogWithoutPayload> flowLogs) {
        List<FlowChainLog> chainLogs = List.of(flowChainLog);
        when(flowChainLogService.findAllByFlowChainIdInOrderByCreatedDesc(Set.of(FLOW_CHAIN_ID), PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(chainLogs));
        lenient().when(flowChainLogService.hasEventInFlowChainQueue(chainLogs)).thenReturn(hasEventInQueue);
        when(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID))).thenReturn(flowLogs);
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

    private FlowChainLog flowChainLog() {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(FLOW_CHAIN_ID);
        flowChainLog.setFlowChainType("UpgradeDataHubFlowEventChainFactory");
        return flowChainLog;
    }

    private FlowLogWithoutPayload flowLog(String currentState, String nextEvent, long created) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        lenient().when(flowLog.getFinalized()).thenReturn(true);
        lenient().when(flowLog.getFlowId()).thenReturn(FLOW_ID);
        lenient().when(flowLog.getFlowChainId()).thenReturn(FLOW_CHAIN_ID);
        lenient().when(flowLog.getResourceId()).thenReturn(STACK_ID);
        return flowLog;
    }

    private FlowLogWithoutPayload flowLog(String currentState, String nextEvent, long created, Long endTime) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        lenient().when(flowLog.getFinalized()).thenReturn(true);
        lenient().when(flowLog.getFlowId()).thenReturn(FLOW_ID);
        lenient().when(flowLog.getEndTime()).thenReturn(endTime);
        return flowLog;
    }

    private FlowLogWithoutPayload flowLog(String currentState, String nextEvent, long created, Long endTime, String chainId) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        lenient().when(flowLog.getFinalized()).thenReturn(true);
        lenient().when(flowLog.getFlowId()).thenReturn(FLOW_ID);
        lenient().when(flowLog.getFlowChainId()).thenReturn(chainId);
        lenient().when(flowLog.getEndTime()).thenReturn(endTime);
        lenient().when(flowLog.getResourceId()).thenReturn(STACK_ID);
        lenient().when(flowLog.getFlowType()).thenReturn(ClassValue.ofUnknown("a.b.c.CreateDataHubFlowConfig"));
        return flowLog;
    }

    private FlowLogWithoutPayload pendingFlowLog(String currentState, String nextEvent, long created) {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        lenient().when(flowLog.getCurrentState()).thenReturn(currentState);
        lenient().when(flowLog.getNextEvent()).thenReturn(nextEvent);
        lenient().when(flowLog.getCreated()).thenReturn(created);
        lenient().when(flowLog.getStateStatus()).thenReturn(StateStatus.PENDING);
        lenient().when(flowLog.getFinalized()).thenReturn(false);
        lenient().when(flowLog.getFlowId()).thenReturn(FLOW_ID);
        lenient().when(flowLog.getFlowChainId()).thenReturn(FLOW_CHAIN_ID);
        return flowLog;
    }
}
