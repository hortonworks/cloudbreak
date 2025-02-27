package com.sequenceiq.freeipa.service.operation;

import static com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType.INTERNAL_NLB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.freeipa.flow.chain.ProvisionFlowEventChainFactory;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.ProvisionTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionFlowConfig;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerProvisionCondition;

@ExtendWith(MockitoExtension.class)
public class FlowOperationServiceTest {

    private static final long STACK_ID = 1L;

    private static final String TEST_ENV_CRN = "crn:cdp:environments:us-west-1:autoscale:cluster:ffff";

    private static final List<Class<?>> EXPECTED_TYPE_LIST = List.of(StackProvisionFlowConfig.class, FreeIpaLoadBalancerProvisionFlowConfig.class,
            FreeIpaProvisionFlowConfig.class);

    @InjectMocks
    private FlowOperationService underTest;

    @InjectMocks
    private ProvisionFlowEventChainFactory provisionFlowEventChainFactory;

    @Mock
    private FlowOperationStatisticsService flowOperationStatisticsService;

    @Mock
    private OperationDetailsPopulator operationDetailsPopulator;

    @Mock
    private OperationFlowsView operationFlowsView;

    @Mock
    private FreeIpaLoadBalancerProvisionCondition freeIpaLoadBalancerProvisionCondition;

    @ParameterizedTest
    @CsvSource({
            "true,INTERNAL_NLB,3",
            "true,NONE,2",
            "false,INTERNAL_NLB,2",
            "false,NONE,2"
    })
    public void testGetOperationProgressByResourceCrnWithLoadBalancerFlow(boolean loadBalancerEntitlementEnabled, FreeIpaLoadBalancerType loadBalancerInRequest,
            int expectedEventQueueSize) {
        // GIVEN
        lenient().when(freeIpaLoadBalancerProvisionCondition.loadBalancerProvisionEnabled(STACK_ID, loadBalancerInRequest))
                .thenReturn(loadBalancerEntitlementEnabled && loadBalancerInRequest == INTERNAL_NLB);
        FlowTriggerEventQueue eventQueue =
                provisionFlowEventChainFactory.createFlowTriggerEventQueue(new ProvisionTriggerEvent(null, STACK_ID, loadBalancerInRequest));
        given(flowOperationStatisticsService.getLastFlowOperationByResourceCrn(anyString()))
                .willReturn(Optional.of(operationFlowsView));
        given(operationFlowsView.getOperationType()).willReturn(OperationType.PROVISION);
        given(operationDetailsPopulator
                .createOperationView(operationFlowsView, OperationResource.FREEIPA, EXPECTED_TYPE_LIST)).willReturn(new OperationView());
        // WHEN
        underTest.getOperationProgressByEnvironmentCrn(TEST_ENV_CRN, false);
        // THEN
        verify(operationDetailsPopulator, times(1))
                .createOperationView(operationFlowsView, OperationResource.FREEIPA, EXPECTED_TYPE_LIST);
        // Checks that the number of provision flows are the same as in the operation check
        assertEquals(expectedEventQueueSize, eventQueue.getQueue().size());
    }

}
