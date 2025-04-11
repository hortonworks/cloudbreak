package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class UpscaleFlowEventChainFactoryTest {

    @Mock
    private StackService stackService;

    @Mock
    private SkuMigrationService skuMigrationService;

    @InjectMocks
    private UpscaleFlowEventChainFactory underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "zombieAutoCleanupEnabled", Boolean.TRUE);
    }

    @Test
    void testCreateFlowTriggerEventQueueWhenFullUpscaleWithSkuUpgrade() {
        when(stackService.getStackProxyById(any())).thenReturn(mock(StackDto.class));
        when(skuMigrationService.isUpscaleSkuMigrationEnabled()).thenReturn(Boolean.TRUE);
        when(skuMigrationService.isMigrationNecessary(any())).thenReturn(Boolean.TRUE);
        Map<String, Set<String>> hostGroupWithHostNames = Map.of("master", Set.of("master1", "master2"));
        StackAndClusterUpscaleTriggerEvent triggerEvent = new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT,
                0L, Map.of(), Map.of(), hostGroupWithHostNames, ScalingType.UPSCALE_TOGETHER, true, true,
                null, false, true, null, null, "", false);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        Assertions.assertEquals(5, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue, "WITH_SKU_MIGRATION");
    }

    @Test
    void testCreateFlowTriggerEventQueueWhenFullUpscaleWithoutSkuUpgrade() {
        when(stackService.getStackProxyById(any())).thenReturn(mock(StackDto.class));
        when(skuMigrationService.isUpscaleSkuMigrationEnabled()).thenReturn(Boolean.FALSE);
        Map<String, Set<String>> hostGroupWithHostNames = Map.of("master", Set.of("master1", "master2"));
        StackAndClusterUpscaleTriggerEvent triggerEvent = new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT,
                0L, Map.of(), Map.of(), hostGroupWithHostNames, ScalingType.UPSCALE_TOGETHER, true, true,
                null, false, true, null, null, "", false);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        Assertions.assertEquals(4, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue, "WITHOUT_SKU_MIGRATION");
    }
}