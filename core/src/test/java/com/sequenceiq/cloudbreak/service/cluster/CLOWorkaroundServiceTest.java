package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class CLOWorkaroundServiceTest {

    private static final long STACK_ID = 1L;

    private static final String BLUEPRINT_TEXT = "blueprint-text";

    @Mock
    private CmTemplateService cmTemplateService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private CLOWorkaroundService underTest;

    @Test
    void testGetClusterUpgradeTriggerEventWhenCLOIsPresentAnd732() {
        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(
                "selector", STACK_ID, null, null, false, false, "AWS", false, "7.3.2");
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setExtendedBlueprintText(BLUEPRINT_TEXT);
        stack.setCluster(cluster);

        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(cmTemplateService.isServiceTypePresent("LAKEHOUSE_OPTIMIZER", BLUEPRINT_TEXT)).thenReturn(true);

        List<Selectable> result = underTest.getClusterUpgradeTriggerEvent(event);

        assertEquals(1, result.size());
        assertEquals(SALT_UPDATE_EVENT.event(), result.get(0).getSelector());
        assertEquals(STACK_ID, ((StackEvent) result.get(0)).getResourceId());
    }

    @Test
    void testGetClusterUpgradeTriggerEventWhenCLOIsPresentAndNot732() {
        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(
                "selector", STACK_ID, null, null, false, false, "AWS", false, "7.3.1");
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setExtendedBlueprintText(BLUEPRINT_TEXT);
        stack.setCluster(cluster);

        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(cmTemplateService.isServiceTypePresent("LAKEHOUSE_OPTIMIZER", BLUEPRINT_TEXT)).thenReturn(true);

        List<Selectable> result = underTest.getClusterUpgradeTriggerEvent(event);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetClusterUpgradeTriggerEventWhenCLOIsNotPresent() {
        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(
                "selector", STACK_ID, null, null, false, false, "AWS", false, "7.3.2");
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setExtendedBlueprintText(BLUEPRINT_TEXT);
        stack.setCluster(cluster);

        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(cmTemplateService.isServiceTypePresent("LAKEHOUSE_OPTIMIZER", BLUEPRINT_TEXT)).thenReturn(false);

        List<Selectable> result = underTest.getClusterUpgradeTriggerEvent(event);

        assertTrue(result.isEmpty());
    }
}
