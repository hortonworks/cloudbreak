package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.maintenancewindow.DataHubMaintenanceWindowScheduleService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class StackTerminationServiceTest {

    private static final long STACK_ID = 1L;

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:acc:cluster:dh-1";

    @Mock
    private TerminationService terminationService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakMetricService metricService;

    @Mock
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Mock
    private ObjectProvider<DataHubMaintenanceWindowScheduleService> dataHubMaintenanceWindowScheduleService;

    @Mock
    private DataHubMaintenanceWindowScheduleService scheduleServiceBean;

    @Mock
    private StackView stack;

    @InjectMocks
    private StackTerminationService underTest;

    @BeforeEach
    void setUp() {
        when(stack.getId()).thenReturn(STACK_ID);
    }

    private void runScheduleConsumerOnIfAvailable() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Consumer.class).accept(scheduleServiceBean);
            return null;
        }).when(dataHubMaintenanceWindowScheduleService).ifAvailable(any());
    }

    @Test
    void finishStackTerminationClearsDataHubScheduleBeforeFinalize() {
        when(stack.isDatahub()).thenReturn(true);
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        runScheduleConsumerOnIfAvailable();

        underTest.finishStackTermination(stack, false, null);

        InOrder inOrder = inOrder(scheduleServiceBean, terminationService);
        inOrder.verify(scheduleServiceBean).clearScheduleQuietly(RESOURCE_CRN);
        inOrder.verify(terminationService).finalizeTermination(STACK_ID, false);
    }

    @Test
    void finishStackTerminationDoesNotClearScheduleForNonDataHub() {
        when(stack.isDatahub()).thenReturn(false);

        underTest.finishStackTermination(stack, false, null);

        verify(terminationService).finalizeTermination(STACK_ID, false);
        verifyNoInteractions(dataHubMaintenanceWindowScheduleService, scheduleServiceBean);
    }

    @Test
    void forcedTerminationErrorClearsDataHubScheduleBeforeFinalize() {
        when(stack.isDatahub()).thenReturn(true);
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        runScheduleConsumerOnIfAvailable();

        underTest.handleStackTerminationError(stack, new RuntimeException("boom"), true);

        InOrder inOrder = inOrder(scheduleServiceBean, terminationService);
        inOrder.verify(scheduleServiceBean).clearScheduleQuietly(RESOURCE_CRN);
        inOrder.verify(terminationService).finalizeTermination(STACK_ID, true);
    }

    @Test
    void nonForcedTerminationErrorDoesNotClearScheduleOrFinalize() {
        underTest.handleStackTerminationError(stack, new RuntimeException("boom"), false);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DELETE_FAILED), any());
        verify(terminationService, never()).finalizeTermination(any(), anyBoolean());
        verifyNoInteractions(dataHubMaintenanceWindowScheduleService, scheduleServiceBean);
    }
}
