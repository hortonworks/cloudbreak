package com.sequenceiq.cloudbreak.structuredevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.converter.ClusterToClusterDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.flow.ha.NodeConfig;

@ExtendWith(MockitoExtension.class)
public class StructuredSyncEventFactoryTest {

    @Mock
    private Clock clock;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackService stackService;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private ClusterToClusterDetailsConverter clusterToClusterDetailsConverter;

    @InjectMocks
    private StructuredSyncEventFactory underTest;

    @Test
    public void createCDPStructuredSyncEvent() {
        Stack stack = TestUtil.stack();
        stack.setResourceCrn("crn");
        Cluster cluster = TestUtil.cluster();
        Blueprint blueprint = TestUtil.blueprint();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);

        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setBlueprintName("testBpName");

        when(conversionService.convert(any(), eq(StackDetails.class))).thenReturn(null);
        when(conversionService.convert(blueprint, BlueprintDetails.class)).thenReturn(blueprintDetails);

        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        when(nodeConfig.getId()).thenReturn("cbid");

        StructuredSyncEvent result = underTest.createStructuredSyncEvent(1L);

        assertNull(result.getStack());
        assertEquals(CloudbreakEventService.DATAHUB_RESOURCE_TYPE, result.getOperation().getResourceType());
        assertEquals("crn", result.getOperation().getResourceCrn());
        assertEquals("cbid", result.getOperation().getCloudbreakId());
        assertEquals("testBpName", result.getBlueprintDetails().getBlueprintName());
    }
}
