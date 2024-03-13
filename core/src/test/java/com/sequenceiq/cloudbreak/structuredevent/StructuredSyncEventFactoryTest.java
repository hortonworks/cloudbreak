package com.sequenceiq.cloudbreak.structuredevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.converter.BlueprintToBlueprintDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.ClusterToClusterDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.StackToStackDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@ExtendWith(MockitoExtension.class)
public class StructuredSyncEventFactoryTest {

    @Mock
    private Clock clock;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private ClusterToClusterDetailsConverter clusterToClusterDetailsConverter;

    @Mock
    private BlueprintToBlueprintDetailsConverter blueprintToBlueprintDetailsConverter;

    @Mock
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @InjectMocks
    private StructuredSyncEventFactory underTest;

    @Test
    public void createCDPStructuredSyncEvent() {
        Stack stack = TestUtil.stack();
        stack.setResourceCrn("crn");
        Cluster cluster = TestUtil.cluster();
        Blueprint blueprint = TestUtil.blueprint();

        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setBlueprintName("testBpName");

        when(stackToStackDetailsConverter.convert(any(), any(), any())).thenReturn(null);
        when(blueprintToBlueprintDetailsConverter.convert(blueprint)).thenReturn(blueprintDetails);

        when(stackDtoService.getStackViewById(1L)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(1L)).thenReturn(cluster);
        when(stackDtoService.getBlueprint(cluster.getId())).thenReturn(blueprint);
        when(nodeConfig.getId()).thenReturn("cbid");

        StructuredSyncEvent result = underTest.createStructuredSyncEvent(1L);

        assertNull(result.getStack());
        assertEquals(CloudbreakEventService.DATAHUB_RESOURCE_TYPE, result.getOperation().getResourceType());
        assertEquals("crn", result.getOperation().getResourceCrn());
        assertEquals("cbid", result.getOperation().getCloudbreakId());
        assertEquals("testBpName", result.getBlueprintDetails().getBlueprintName());
    }
}
