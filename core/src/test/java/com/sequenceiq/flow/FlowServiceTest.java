package com.sequenceiq.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@RunWith(MockitoJUnitRunner.class)
public class FlowServiceTest {

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private FlowService underTest;

    @Before
    public void setup() {
        when(conversionService.convert(any(), eq(FlowLogResponse.class))).thenReturn(new FlowLogResponse());
    }

    @Test
    public void testGetLastFlowById() {
        when(flowLogDBService.getLastFlowLog(anyString())).thenReturn(Optional.of(new FlowLog()));

        underTest.getLastFlowById("1");

        verify(flowLogDBService).getLastFlowLog(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetLastFlowByIdException() {
        when(flowLogDBService.getLastFlowLog(anyString())).thenReturn(Optional.empty());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Not found flow for this flow id!");

        underTest.getLastFlowById("1");

        verify(flowLogDBService).getLastFlowLog(anyString());
        verifyZeroInteractions(conversionService);
    }

    @Test
    public void testGetFlowLogsByFlowIdEmpty() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList());

        assertEquals(0, underTest.getFlowLogsByFlowId("1").size());

        verify(flowLogDBService).findAllByFlowIdOrderByCreatedDesc(anyString());
        verifyZeroInteractions(conversionService);
    }

    @Test
    public void testGetFlowLogsByFlowId() {
        when(flowLogDBService.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByFlowId("1").size());

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
        when(flowLogDBService.getFlowLogsByResourceCrnOrName(anyString())).thenReturn(Lists.newArrayList(new FlowLog()));

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
        when(flowLogDBService.getFlowLogsByResourceCrnOrName(anyString())).thenReturn(Lists.newArrayList(new FlowLog()));

        underTest.getFlowLogsByResourceCrn(Crn.fromString(STACK_CRN).toString());

        verify(flowLogDBService).getFlowLogsByResourceCrnOrName(anyString());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testGetFlowLogsByResourceNameAndChainIdIfNotFound() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("FlowChain not found by this flowChainId!");

        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(anyString())).thenReturn(Optional.empty());

        underTest.getFlowLogsByResourceNameAndChainId("myLittleSdx", "1");

        verify(flowChainLogService).findFirstByFlowChainIdOrderByCreatedDesc(anyString());
        verifyZeroInteractions(flowLogDBService, conversionService);
    }

    @Test
    public void testHasFlowRunningIfNotFound() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("FlowChain not found by this flowChainId!");

        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(anyString())).thenReturn(Optional.empty());

        underTest.hasFlowRunning("myLittleSdx", "1");

        verify(flowChainLogService).findFirstByFlowChainIdOrderByCreatedDesc(anyString());
        verifyZeroInteractions(flowLogDBService, conversionService);
    }

    @Test
    public void testGetFlowLogsByResourceNameAndChainId() {
        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(anyString())).thenReturn(Optional.of(new FlowChainLog()));
        when(flowChainLogService.collectRelatedFlowChains(any(), any())).thenReturn(Lists.newArrayList(new FlowChainLog()));
        when(flowLogDBService.getFlowLogsByResourceAndChainId(anyString(), any())).thenReturn(Lists.newArrayList(new FlowLog()));

        underTest.getFlowLogsByResourceNameAndChainId("myLittleSdx", "1");

        verify(flowChainLogService).findFirstByFlowChainIdOrderByCreatedDesc(anyString());
        verify(flowChainLogService).collectRelatedFlowChains(any(), any());
        verify(flowLogDBService).getFlowLogsByResourceAndChainId(anyString(), any());
        verify(conversionService).convert(any(), eq(FlowLogResponse.class));
    }

    @Test
    public void testHasFlowRunningBasedOnFlowChains() {
        mockHasFlowRunningCalls(Boolean.TRUE, Boolean.FALSE);

        assertTrue(underTest.hasFlowRunning("myLittleSdx", "1").getHasActiveFlow());

        verifyHasFlowRunningCalls();
        verify(flowLogDBService, times(0)).hasPendingFlowEvent(any());
    }

    @Test
    public void testHasFlowRunningBasedOnFlowEvents() {
        mockHasFlowRunningCalls(Boolean.FALSE, Boolean.TRUE);

        assertTrue(underTest.hasFlowRunning("myLittleSdx", "1").getHasActiveFlow());

        verifyHasFlowRunningCalls();
        verify(flowLogDBService).hasPendingFlowEvent(any());
    }

    @Test
    public void testNoFlowRunning() {
        mockHasFlowRunningCalls(Boolean.FALSE, Boolean.FALSE);

        assertFalse(underTest.hasFlowRunning("myLittleSdx", "1").getHasActiveFlow());

        verifyHasFlowRunningCalls();
        verify(flowLogDBService).hasPendingFlowEvent(any());
    }

    private void verifyHasFlowRunningCalls() {
        verify(flowChainLogService).findFirstByFlowChainIdOrderByCreatedDesc(anyString());
        verify(flowChainLogService).collectRelatedFlowChains(any(), any());
        verify(flowLogDBService).getFlowLogsByResourceAndChainId(anyString(), any());
        verify(flowChainLogService).checkIfAnyFlowChainHasEventInQueue(any());
    }

    private void mockHasFlowRunningCalls(Boolean hasAnyFlowEventInChain, Boolean hasPengingFlowEvent) {
        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(anyString())).thenReturn(Optional.of(new FlowChainLog()));
        when(flowChainLogService.collectRelatedFlowChains(any(), any())).thenReturn(Lists.newArrayList(new FlowChainLog()));
        when(flowLogDBService.getFlowLogsByResourceAndChainId(anyString(), any())).thenReturn(Lists.newArrayList(new FlowLog()));
        when(flowChainLogService.checkIfAnyFlowChainHasEventInQueue(any())).thenReturn(hasAnyFlowEventInChain);
        when(flowLogDBService.hasPendingFlowEvent(any())).thenReturn(hasPengingFlowEvent);
    }
}
