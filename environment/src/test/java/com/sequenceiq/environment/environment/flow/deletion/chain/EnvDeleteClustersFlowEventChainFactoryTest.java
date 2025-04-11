package com.sequenceiq.environment.environment.flow.deletion.chain;

import static com.sequenceiq.environment.environment.flow.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class EnvDeleteClustersFlowEventChainFactoryTest {

    private EnvDeleteClustersFlowEventChainFactory underTest;

    @Test
    void testFlowChainEventQueueBuildingAndGenerateGraph() {
        EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);
        underTest = new EnvDeleteClustersFlowEventChainFactory(environmentService);
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(any(), anyLong())).thenReturn(List.of());
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(0L)
                .withResourceCrn("resourceCrn")
                .build();
        String actorCrn = CrnTestUtil
                .getUserCrnBuilder()
                .setAccountId("test")
                .setResource("testUser")
                .build()
                .toString();

        FlowTriggerEventQueue flowTriggerEventQueue = ThreadBasedUserCrnProvider.doAs(actorCrn, () -> underTest.createFlowTriggerEventQueue(envDeleteEvent));

        Assertions.assertEquals(2, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }

}