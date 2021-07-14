package com.sequenceiq.freeipa.service.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.freeipa.flow.chain.ProvisionFlowEventChainFactory;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionFlowConfig;

@ExtendWith(MockitoExtension.class)
public class FlowOperationServiceTest {

    private static final String TEST_ENV_CRN = "crn:cdp:environments:us-west-1:autoscale:cluster:ffff";

    private static final List<Class<?>> EXPECTED_TYPE_LIST = List.of(StackProvisionFlowConfig.class, FreeIpaProvisionFlowConfig.class);

    @InjectMocks
    private FlowOperationService underTest;

    @Mock
    private FlowService flowService;

    @Mock
    private OperationDetailsPopulator operationDetailsPopulator;

    @Mock
    private OperationFlowsView operationFlowsView;

    @Test
    public void testGetOperationProgressByResourceCrn() {
        // GIVEN
        ProvisionFlowEventChainFactory provisionFlowEventChainFactory = new ProvisionFlowEventChainFactory();
        FlowTriggerEventQueue eventQueue = provisionFlowEventChainFactory.createFlowTriggerEventQueue(new StackEvent(null, null));
        given(flowService.getLastFlowOperationByResourceCrn(anyString()))
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
        assertEquals(EXPECTED_TYPE_LIST.size(), eventQueue.getQueue().size());
    }
}
