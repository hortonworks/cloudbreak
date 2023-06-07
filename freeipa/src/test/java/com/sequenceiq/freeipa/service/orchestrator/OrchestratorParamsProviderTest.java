package com.sequenceiq.freeipa.service.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class OrchestratorParamsProviderTest {

    private static final long STACK_ID = 123L;

    private static final String STATE = "state";

    @InjectMocks
    private OrchestratorParamsProvider underTest;

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private Stack stack;

    @Mock
    private GatewayConfig gatewayConfig;

    private Node node1;

    private Node node2;

    @BeforeEach
    void setUp() {
        node1 = new Node("privateIP1", "publicIP1", "instance1", "instanceType1", "fqdn1", "hostgroup");
        node2 = new Node("privateIP2", "publicIP2", "instance2", "instanceType2", "fqdn2", "hostgroup");
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        lenient().when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        lenient().when(freeIpaNodeUtilService.mapInstancesToNodes(any())).thenReturn(Set.of(node1, node2));
    }

    @Test
    void createStateParams() {
        OrchestratorStateParams params = underTest.createStateParams(STACK_ID, STATE);

        assertThat(params.getState()).isEqualTo(STATE);
        assertThat(params.getPrimaryGatewayConfig()).isEqualTo(gatewayConfig);
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1", "fqdn2"));
        assertThat(params.getExitCriteriaModel()).isInstanceOf(StackBasedExitCriteriaModel.class);
        assertThat(((StackBasedExitCriteriaModel) params.getExitCriteriaModel()).getStackId().get()).isEqualTo(STACK_ID);
    }

    @Test
    void createStateParamsForSingleTarget() {
        OrchestratorStateParams params = underTest.createStateParamsForSingleTarget(stack, "fqdn1", STATE);

        assertThat(params.getState()).isEqualTo(STATE);
        assertThat(params.getPrimaryGatewayConfig()).isEqualTo(gatewayConfig);
        assertThat(params.getTargetHostNames()).containsOnly("fqdn1");
        assertThat(params.getExitCriteriaModel()).isInstanceOf(StackBasedExitCriteriaModel.class);
        assertThat(((StackBasedExitCriteriaModel) params.getExitCriteriaModel()).getStackId().get()).isEqualTo(STACK_ID);
    }

}
