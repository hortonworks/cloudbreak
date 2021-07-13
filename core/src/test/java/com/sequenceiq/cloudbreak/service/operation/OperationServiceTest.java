package com.sequenceiq.cloudbreak.service.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.core.flow2.chain.ProvisionFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(MockitoExtension.class)
public class OperationServiceTest {

    private static final String TEST_CLUSTER_CRN = "crn:cdp:datahub:us-west-1:autoscale:cluster:ffff";

    private static final String TEST_DATALAKE_CRN = "crn:cdp:datalake:us-west-1:autoscale:cluster:ffff";

    private static final List<Class<?>> EXPECTED_TYPE_LIST =  List.of(
            CloudConfigValidationFlowConfig .class,
            KerberosConfigValidationFlowConfig .class,
            ExternalDatabaseCreationFlowConfig .class,
            StackCreationFlowConfig .class,
            ClusterCreationFlowConfig .class
    );

    private OperationService underTest;

    @Mock
    private FlowService flowService;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private StackOperations stackOperations;

    @Mock
    private OperationDetailsPopulator operationDetailsPopulator;

    @Mock
    private Stack stack;

    @Mock
    private OperationFlowsView operationFlowsView;

    @Mock
    private OperationView operationView;

    @Mock
    private OperationView remoteDatabaseOperationView;

    @BeforeEach
    public void setUp() {
        underTest = new OperationService(flowService, databaseService, stackOperations, operationDetailsPopulator);
    }

    @Test
    public void testGetOperationProgressByResourceCrnWithDatalake() {
        // GIVEN
        ProvisionFlowEventChainFactory provisionFlowEventChainFactory = new ProvisionFlowEventChainFactory();
        FlowTriggerEventQueue eventQueue = provisionFlowEventChainFactory.createFlowTriggerEventQueue(new StackEvent(null, null));
        given(flowService.getLastFlowOperationByResourceCrn(anyString()))
                .willReturn(Optional.of(operationFlowsView));
        given(operationFlowsView.getOperationType()).willReturn(OperationType.PROVISION);
        given(operationDetailsPopulator.createOperationView(operationFlowsView, OperationResource.DATALAKE, EXPECTED_TYPE_LIST))
                .willReturn(operationView);
        // WHEN
        OperationView result = underTest.getOperationProgressByResourceCrn(TEST_DATALAKE_CRN, true);
        // THEN
        assertEquals(operationView, result);
        verify(operationDetailsPopulator, times(1))
                .createOperationView(operationFlowsView, OperationResource.DATALAKE, EXPECTED_TYPE_LIST);
        // Checks that the number of provision flows are the same as in the operation check
        assertEquals(eventQueue.getQueue().size(), EXPECTED_TYPE_LIST.size());
    }

    @Test
    public void testGetOperationProgressByResourceCrnWithDatahub() {
        // GIVEN
        given(flowService.getLastFlowOperationByResourceCrn(anyString()))
                .willReturn(Optional.of(operationFlowsView));
        given(operationFlowsView.getOperationType()).willReturn(OperationType.PROVISION);
        given(operationDetailsPopulator.createOperationView(operationFlowsView, OperationResource.DATAHUB, EXPECTED_TYPE_LIST))
                .willReturn(new OperationView());
        given(stackOperations.getStackByCrn(anyString())).willReturn(stack);
        given(stack.getExternalDatabaseCreationType()).willReturn(DatabaseAvailabilityType.NON_HA);
        given(databaseService.getRemoteDatabaseOperationProgress(any(), anyBoolean())).willReturn(Optional.of(remoteDatabaseOperationView));
        // WHEN
        OperationView result = underTest.getOperationProgressByResourceCrn(TEST_CLUSTER_CRN, true);
        // THEN
        assertEquals(remoteDatabaseOperationView, result.getSubOperations().get(OperationResource.REMOTEDB));
        verify(databaseService, times(1)).getRemoteDatabaseOperationProgress(any(), anyBoolean());
        verify(operationDetailsPopulator, times(1)).createOperationView(operationFlowsView,
                OperationResource.DATAHUB, EXPECTED_TYPE_LIST);
    }

    @Test
    public void testGetOperationProgressByResourceCrnWithFailedRemoteDbResponse() {
        // GIVEN
        given(flowService.getLastFlowOperationByResourceCrn(anyString()))
                .willReturn(Optional.of(operationFlowsView));
        given(operationFlowsView.getOperationType()).willReturn(OperationType.PROVISION);
        given(operationDetailsPopulator.createOperationView(operationFlowsView, OperationResource.DATAHUB, EXPECTED_TYPE_LIST))
                .willReturn(new OperationView());
        given(stackOperations.getStackByCrn(anyString())).willReturn(stack);
        given(stack.getExternalDatabaseCreationType()).willReturn(DatabaseAvailabilityType.NON_HA);
        given(databaseService.getRemoteDatabaseOperationProgress(any(), anyBoolean())).willThrow(new RuntimeException("my ex"));
        // WHEN
        OperationView result = underTest.getOperationProgressByResourceCrn(TEST_CLUSTER_CRN, true);
        // THEN
        assertNull(result.getSubOperations().get(OperationResource.REMOTEDB));
    }
}
