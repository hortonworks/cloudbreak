package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams.JvmConfigRecordV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams.ResetJvmParamsV4Response;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigApplicability;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigRecord;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.ResetJvmParamsDiff;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class ClusterReallocateMemoryServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String STACK_CRN = "crn:cdp:cloudbreak:us-west-1:someone:stack:12345";

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "flowId");

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterApi clusterApi;

    @InjectMocks
    private ClusterReallocateMemoryService underTest;

    @Test
    public void testResetJvmParamsWhenDryRunReturnsDiffWithoutTriggeringFlow() {
        ResetJvmParamsDiff diff = new ResetJvmParamsDiff(
                List.of(new JvmConfigRecord("heap_size", "1024", "rcg", "cluster", "yarn", JvmConfigApplicability.RECONFIGURABLE)),
                List.of(new JvmConfigRecord("heap_size", "2048", "rcg", "cluster", "yarn", JvmConfigApplicability.RECONFIGURABLE)));
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.reallocateMemoryDiff()).thenReturn(diff);

        ResetJvmParamsV4Response result = underTest.resetJvmParams(stack, true);

        verify(flowManager, never()).triggerResetJvmParams(any());
        assertEquals(FlowIdentifier.notTriggered(), result.getFlowIdentifier());
        assertTrue(result.getMessage().contains(STACK_CRN));
        assertNotNull(result.getResetJvmParamsDiff());
        assertEquals(1, result.getResetJvmParamsDiff().getConfigsBefore().size());
        assertEquals(1, result.getResetJvmParamsDiff().getConfigsAfter().size());
        JvmConfigRecordV4Response before = result.getResetJvmParamsDiff().getConfigsBefore().getFirst();
        assertEquals("heap_size", before.getName());
        assertEquals("1024", before.getValue());
        assertEquals("rcg", before.getRoleConfigGroupName());
        assertEquals("cluster", before.getClusterName());
        assertEquals("yarn", before.getServiceName());
        assertEquals(JvmConfigApplicability.RECONFIGURABLE.name(), before.getApplicability());
        JvmConfigRecordV4Response after = result.getResetJvmParamsDiff().getConfigsAfter().getFirst();
        assertEquals("heap_size", after.getName());
        assertEquals("2048", after.getValue());
        assertEquals(JvmConfigApplicability.RECONFIGURABLE.name(), after.getApplicability());
    }

    @Test
    public void testResetJvmParamsWhenNotDryRunTriggersFlowAndReturnsDiff() {
        ResetJvmParamsDiff diff = new ResetJvmParamsDiff(
                List.of(),
                List.of(new JvmConfigRecord("heap_size", "2048", null, "cluster", "yarn", JvmConfigApplicability.RECONFIGURABLE)));
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.reallocateMemoryDiff()).thenReturn(diff);
        when(flowManager.triggerResetJvmParams(STACK_ID)).thenReturn(FLOW_IDENTIFIER);

        ResetJvmParamsV4Response result = underTest.resetJvmParams(stack, false);

        verify(flowManager).triggerResetJvmParams(STACK_ID);
        assertEquals(FLOW_IDENTIFIER, result.getFlowIdentifier());
        assertTrue(result.getMessage().contains(STACK_CRN));
        assertNotNull(result.getResetJvmParamsDiff());
        assertTrue(result.getResetJvmParamsDiff().getConfigsBefore().isEmpty());
        assertEquals(1, result.getResetJvmParamsDiff().getConfigsAfter().size());
        assertEquals("2048", result.getResetJvmParamsDiff().getConfigsAfter().getFirst().getValue());
        assertNull(result.getResetJvmParamsDiff().getConfigsAfter().getFirst().getRoleConfigGroupName());
    }

    @Test
    public void testResetJvmParamsWhenDiffIsNullReturnsDiffWithEmptyLists() {
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.reallocateMemoryDiff()).thenReturn(null);

        ResetJvmParamsV4Response result = underTest.resetJvmParams(stack, true);

        verify(flowManager, never()).triggerResetJvmParams(any());
        assertNotNull(result.getResetJvmParamsDiff());
        assertTrue(result.getResetJvmParamsDiff().getConfigsBefore().isEmpty());
        assertTrue(result.getResetJvmParamsDiff().getConfigsAfter().isEmpty());
    }

    @Test
    public void testResetJvmParamsWhenRecordApplicabilityIsNullMapsToNull() {
        ResetJvmParamsDiff diff = new ResetJvmParamsDiff(
                List.of(),
                List.of(new JvmConfigRecord("heap_size", "2048", null, null, null, null)));
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.reallocateMemoryDiff()).thenReturn(diff);
        when(flowManager.triggerResetJvmParams(STACK_ID)).thenReturn(FLOW_IDENTIFIER);

        ResetJvmParamsV4Response result = underTest.resetJvmParams(stack, false);

        JvmConfigRecordV4Response record = result.getResetJvmParamsDiff().getConfigsAfter().getFirst();
        assertNull(record.getApplicability());
        assertNull(record.getRoleConfigGroupName());
        assertNull(record.getClusterName());
        assertNull(record.getServiceName());
    }
}
