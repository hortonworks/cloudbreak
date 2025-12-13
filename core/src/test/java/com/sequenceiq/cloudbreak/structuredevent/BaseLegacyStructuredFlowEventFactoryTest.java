package com.sequenceiq.cloudbreak.structuredevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
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
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@ExtendWith(MockitoExtension.class)
class BaseLegacyStructuredFlowEventFactoryTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private Clock clock;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private ClusterToClusterDetailsConverter clusterToClusterDetailsConverter;

    @Mock
    private BlueprintToBlueprintDetailsConverter blueprintToBlueprintDetailsConverter;

    @Mock
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private BaseLegacyStructuredFlowEventFactory baseLegacyStructuredFlowEventFactory;

    private String bpName = "testBpName";

    @Test
    void createStructuredFlowEvent() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        Blueprint blueprint = TestUtil.blueprint();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setBlueprintName(bpName);
        when(stackDtoService.getBlueprint(any())).thenReturn(blueprint);
        when(stackDtoService.getClusterViewByStackId(any())).thenReturn(cluster);
        when(stackToStackDetailsConverter.convert(any(), any(), any())).thenReturn(null);
        when(blueprintToBlueprintDetailsConverter.convert(blueprint)).thenReturn(blueprintDetails);
        when(stackDtoService.getStackViewById(1L)).thenReturn(stack);
        String userCrn = "exampleCrn";
        StructuredFlowEvent result = ThreadBasedUserCrnProvider.doAs(userCrn,
                () -> baseLegacyStructuredFlowEventFactory.createStucturedFlowEvent(1L, new FlowDetails(), true));
        assertNull(result.getException());
        assertEquals(bpName, result.getBlueprintDetails().getBlueprintName());
        assertEquals(userCrn, result.getOperation().getUserCrn());
        verifyNoInteractions(regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    void createStructuredFlowEventWhenNoUserCrnOnTheThreadLocal() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        Blueprint blueprint = TestUtil.blueprint();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setBlueprintName(bpName);
        when(stackDtoService.getBlueprint(any())).thenReturn(blueprint);
        when(stackDtoService.getClusterViewByStackId(any())).thenReturn(cluster);
        when(stackToStackDetailsConverter.convert(any(), any(), any())).thenReturn(null);
        when(blueprintToBlueprintDetailsConverter.convert(blueprint)).thenReturn(blueprintDetails);
        when(stackDtoService.getStackViewById(1L)).thenReturn(stack);
        String internalActorCrn = "internalActorCrn";
        when(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()).thenReturn(internalActorCrn);
        StructuredFlowEvent result = baseLegacyStructuredFlowEventFactory.createStucturedFlowEvent(1L, new FlowDetails(), true);
        assertNull(result.getException());
        assertEquals(bpName, result.getBlueprintDetails().getBlueprintName());
        assertEquals(internalActorCrn, result.getOperation().getUserCrn());
    }

    @Test
    void createStructuredNotificationEventWhenUserCrnIsInThreadLocal() {
        String userCrn = "exampleCrn";
        StructuredNotificationEvent result = ThreadBasedUserCrnProvider.doAs(userCrn,
                () -> baseLegacyStructuredFlowEventFactory.createStructuredNotificationEvent(TestUtil.stack(), "", "", ""));
        assertEquals(userCrn, result.getOperation().getUserCrn());
        verifyNoInteractions(regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void createStructuredNotificationEventWhenNoUserCrnOnTheThreadLocal() {
        String creatorCrn = "creatorCrn";
        Stack stack = TestUtil.stack();
        stack.getCreator().setUserCrn(creatorCrn);
        StructuredNotificationEvent result = baseLegacyStructuredFlowEventFactory.createStructuredNotificationEvent(stack, "", "", "");
        assertEquals(creatorCrn, result.getOperation().getUserCrn());
    }
}