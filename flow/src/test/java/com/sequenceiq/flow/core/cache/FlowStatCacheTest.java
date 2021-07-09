package com.sequenceiq.flow.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;

@ExtendWith(MockitoExtension.class)
public class FlowStatCacheTest {

    private static final String SAMPLE_RESOURCE_CRN = "crn:cdp:environments:us-west-1:12345-6789:environment:12345-6789";

    private static final String SAMPLE_FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:12345-6789:environment:12345-6789";

    private static final Long SAMPLE_RESOURCE_ID = 1L;

    private static final Integer SAMPLE_PROGRESS = 66;

    private static final String SAMPLE_CLOUD_PLATFORM = "AWS";

    private static final String SAMPLE_FLOW_ID = "flow-id";

    private static final String SAMPLE_FLOW_CHAIN_ID = "flow-chain-id";

    private static final Date OLD_DATE = new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime();

    @InjectMocks
    private FlowStatCache underTest;

    @Mock
    private PayloadContextProvider payloadContextProvider;

    @Mock
    private FlowOperationStatisticsService flowOperationStatisticsService;

    @BeforeEach
    public void setUp() {
        underTest = new FlowStatCache(flowOperationStatisticsService, payloadContextProvider);
    }

    @Test
    public void testPutFlow() {
        // GIVEN
        PayloadContext payloadContext = PayloadContext.create(SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM);
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(payloadContext);
        // WHEN
        underTest.put(SAMPLE_FLOW_ID, null, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), null, false);
        FlowStat result = underTest.getFlowStatByResourceCrn(SAMPLE_RESOURCE_CRN);
        FlowStat resultByFlowId = underTest.getFlowStatByFlowId(SAMPLE_FLOW_ID);
        // THEN
        assertEquals(SAMPLE_FLOW_ID, result.getFlowId());
        assertEquals(OperationType.PROVISION, result.getOperationType());
        assertNotNull(resultByFlowId);
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
    }

    @Test
    public void testPutFlowWithEnvironmentCrn() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_FREEIPA_CRN, SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        // WHEN
        underTest.put(SAMPLE_FLOW_ID, null, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), null, false);
        FlowStat result = underTest.getFlowStatByResourceCrn(SAMPLE_FREEIPA_CRN);
        FlowStat resultWithEnvCrn = underTest.getFlowStatByResourceCrn(SAMPLE_RESOURCE_CRN);
        // THEN
        assertEquals(SAMPLE_FLOW_ID, result.getFlowId());
        assertEquals(OperationType.PROVISION, result.getOperationType());
        assertEquals(SAMPLE_FLOW_ID, resultWithEnvCrn.getFlowId());
        assertEquals(OperationType.PROVISION, resultWithEnvCrn.getOperationType());
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
    }

    @Test
    public void testPutFlowChain() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_FREEIPA_CRN, SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        // WHEN
        underTest.putByFlowChainId(SAMPLE_FLOW_CHAIN_ID, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), false);
        FlowStat result = underTest.getFlowChainStatByResourceCrn(SAMPLE_FREEIPA_CRN);
        FlowStat resultWithEnvCrn = underTest.getFlowChainStatByResourceCrn(SAMPLE_RESOURCE_CRN);
        FlowStat resultByFlowChainId = underTest.getFlowStatByFlowChainId(SAMPLE_FLOW_CHAIN_ID);
        // THEN
        assertEquals(SAMPLE_FLOW_CHAIN_ID, result.getFlowChainId());
        assertEquals(OperationType.PROVISION, result.getOperationType());
        assertEquals(SAMPLE_FLOW_CHAIN_ID, resultWithEnvCrn.getFlowChainId());
        assertEquals(OperationType.PROVISION, resultWithEnvCrn.getOperationType());
        assertNotNull(resultByFlowChainId);
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
    }

    @Test
    public void testRemoveFlow() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_FREEIPA_CRN, SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        // WHEN
        underTest.put(SAMPLE_FLOW_ID, null, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), null, false);
        underTest.remove(SAMPLE_FLOW_ID, false);
        FlowStat result = underTest.getFlowStatByResourceCrn(SAMPLE_FREEIPA_CRN);
        FlowStat resultWithEnvCrn = underTest.getFlowStatByResourceCrn(SAMPLE_RESOURCE_CRN);
        FlowStat resultByFlowId = underTest.getFlowStatByFlowId(SAMPLE_FLOW_ID);
        // THEN
        assertNull(result);
        assertNull(resultWithEnvCrn);
        assertNull(resultByFlowId);
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
    }

    @Test
    public void testRemoveFlowChain() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_FREEIPA_CRN, SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        // WHEN
        underTest.putByFlowChainId(SAMPLE_FLOW_CHAIN_ID, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), false);
        underTest.removeByFlowChainId(SAMPLE_FLOW_CHAIN_ID, false);
        FlowStat result = underTest.getFlowStatByResourceCrn(SAMPLE_FREEIPA_CRN);
        FlowStat resultWithEnvCrn = underTest.getFlowStatByResourceCrn(SAMPLE_RESOURCE_CRN);
        FlowStat resultByFlowChainId = underTest.getFlowStatByFlowId(SAMPLE_FLOW_CHAIN_ID);
        // THEN
        assertNull(result);
        assertNull(resultWithEnvCrn);
        assertNull(resultByFlowChainId);
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
    }

    @Test
    public void testGetOperationFlowByResourceCrn() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        given(flowOperationStatisticsService.getProgressFromHistory(any())).willReturn(SAMPLE_PROGRESS);
        // WHEN
        underTest.put(SAMPLE_FLOW_ID, null, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), null, false);
        Optional<OperationFlowsView> resultOpt = underTest.getOperationFlowByResourceCrn(SAMPLE_RESOURCE_CRN);
        // THEN
        assertEquals(SAMPLE_FLOW_ID, resultOpt.get().getOperationId());
        assertEquals(OperationType.PROVISION, resultOpt.get().getOperationType());
        assertTrue(resultOpt.get().isInMemory());
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
        verify(flowOperationStatisticsService, times(1)).getProgressFromHistory(any());
    }

    @Test
    public void testGetOperationFlowChainByResourceCrn() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        given(flowOperationStatisticsService.getProgressFromHistory(any())).willReturn(SAMPLE_PROGRESS);
        // WHEN
        underTest.putByFlowChainId(SAMPLE_FLOW_CHAIN_ID, SAMPLE_RESOURCE_ID, OperationType.PROVISION.name(), false);
        Optional<OperationFlowsView> resultOpt = underTest.getOperationFlowByResourceCrn(SAMPLE_RESOURCE_CRN);
        // THEN
        assertEquals(SAMPLE_FLOW_CHAIN_ID, resultOpt.get().getOperationId());
        assertEquals(OperationType.PROVISION, resultOpt.get().getOperationType());
        assertEquals(SAMPLE_PROGRESS, resultOpt.get().getProgressFromHistory());
        assertTrue(resultOpt.get().isInMemory());
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
        verify(flowOperationStatisticsService, times(1)).getProgressFromHistory(any());
    }

    @Test
    public void testGetOperationFlowByResourceCrnWithUnknownOperation() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(SAMPLE_RESOURCE_ID)).willReturn(
                PayloadContext.create(SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM));
        // WHEN
        underTest.put(SAMPLE_FLOW_ID, null, SAMPLE_RESOURCE_ID, OperationType.UNKNOWN.name(), null, false);
        Optional<OperationFlowsView> resultOpt = underTest.getOperationFlowByResourceCrn(SAMPLE_RESOURCE_CRN);
        // THEN
        assertTrue(resultOpt.isEmpty());
        verify(payloadContextProvider, times(1)).getPayloadContext(anyLong());
        verify(flowOperationStatisticsService, times(0)).getProgressFromHistory(any());
    }

    @Test
    public void testCleanOldCacheEntries() {
        // GIVEN
        underTest.getFlowIdStatCache().put("flowid1", createFlowStat("flowid1", null, SAMPLE_RESOURCE_CRN, true));
        // WHEN
        underTest.cleanOldCacheEntries(Set.of("flowid2"));
        // THEN
        assertTrue(underTest.getFlowIdStatCache().isEmpty());
    }

    @Test
    public void testCleanOldCacheEntriesAgainstFlowChainIdCache() {
        // GIVEN
        underTest.getFlowChainIdStatCache().put("flowchainid1", createFlowStat("flowid1", "flowchainid1", SAMPLE_RESOURCE_CRN, true));
        // WHEN
        underTest.cleanOldCacheEntries(Set.of("flowid1"));
        // THEN
        assertTrue(underTest.getFlowChainIdStatCache().isEmpty());
    }

    @Test
    public void testCleanOldCacheEntriesWithFlowIdsStillRunning() {
        // GIVEN
        underTest.getFlowIdStatCache().put("flowid1", createFlowStat("flowid1", null, SAMPLE_RESOURCE_CRN, true));
        underTest.getFlowIdStatCache().put("flowid2", createFlowStat("flowid2", null, SAMPLE_RESOURCE_CRN + "1", false));
        // WHEN
        underTest.cleanOldCacheEntries(Set.of("flowid1", "flowid2"));
        // THEN
        assertEquals(2, underTest.getFlowIdStatCache().size());
    }

    @Test
    public void testCleanOldCacheEntriesWithResourceCrn() {
        // GIVEN
        underTest.getResourceCrnFlowStatCache().put(SAMPLE_RESOURCE_CRN, createFlowStat("flowid1", null, SAMPLE_RESOURCE_CRN, true));
        // WHEN
        underTest.cleanOldCacheEntries(Set.of());
        // THEN
        assertTrue(underTest.getResourceCrnFlowStatCache().isEmpty());
    }

    @Test
    public void testCleanOldCacheEntriesWithResourceCrnAndFlowChain() {
        // GIVEN
        underTest.getResourceCrnFlowChainStatCache().put(SAMPLE_RESOURCE_CRN, createFlowStat("flowid1", "flowchainid1",
                SAMPLE_RESOURCE_CRN, true));
        underTest.getResourceCrnFlowChainStatCache().put(SAMPLE_RESOURCE_CRN + "1", createFlowStat("flowid2", "flowchainid2",
                SAMPLE_RESOURCE_CRN + "1", false));
        // WHEN
        underTest.cleanOldCacheEntries(Set.of());
        // THEN
        assertEquals(1, underTest.getResourceCrnFlowChainStatCache().size());
        assertTrue(underTest.getResourceCrnFlowChainStatCache().containsKey(SAMPLE_RESOURCE_CRN + "1"));
    }

    private FlowStat createFlowStat(String flowId, String flowChainId, String crn, boolean old) {
        FlowStat flowStat = new FlowStat();
        flowStat.setFlowId(flowId);
        flowStat.setFlowChainId(flowChainId);
        PayloadContext payloadContext = PayloadContext.create(crn, SAMPLE_CLOUD_PLATFORM);
        flowStat.setPayloadContext(payloadContext);
        if (old) {
            flowStat.setStartTime(OLD_DATE.getTime());
        } else {
            flowStat.setStartTime(new Date().getTime());
        }
        return flowStat;
    }
}
