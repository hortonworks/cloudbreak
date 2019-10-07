package com.sequenceiq.cloudbreak.structuredevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.converter.ClusterToClusterDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.flow.ha.NodeConfig;

@ExtendWith(MockitoExtension.class)
public class StructuredFlowEventFactoryTest {

    @Mock
    private StackService stackService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private Clock clock;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private ClusterToClusterDetailsConverter clusterToClusterDetailsConverter;

    @InjectMocks
    private StructuredFlowEventFactory structuredFlowEventFactory;

    private String bpName = "testBpName";

    @Test
    public void createStucturedFlowEvent() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        Blueprint blueprint = TestUtil.blueprint();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setBlueprintName(bpName);
        when(conversionService.convert(any(), eq(StackDetails.class))).thenReturn(null);
        when(conversionService.convert(blueprint, BlueprintDetails.class)).thenReturn(blueprintDetails);
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        StructuredFlowEvent result = structuredFlowEventFactory.createStucturedFlowEvent(1L, new FlowDetails(), true);
        assertNull(result.getException());
        assertEquals(bpName, result.getBlueprintDetails().getBlueprintName());
    }

    @Test
    public void createStucturedConverterError() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCloudPlatform("AWS");
        when(conversionService.convert(stack, StackDetails.class)).thenReturn(stackDetails);
        when(clusterToClusterDetailsConverter.convert(stack.getCluster()))
                .thenThrow(new BadRequestException("Failed to get Kerberos config from FreeIpa service due to"));
        StructuredFlowEvent result = structuredFlowEventFactory.createStucturedFlowEvent(1L, new FlowDetails(), true);
        assertEquals("AWS", result.getStack().getCloudPlatform());
        assertTrue(result.getException().contains("BadRequestException: Failed to get Kerberos config from FreeIpa"));
    }
}