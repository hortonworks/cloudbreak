package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_SERVICE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDatalakeReachabilityValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeNotReachableException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeDatalakeReachabilityValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private ClusterUpgradeDatalakeReachabilityValidationHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DataLakeStatusCheckerService dataLakeStatusCheckerService;

    @Mock
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Mock
    private StackView stackView;

    @BeforeEach
    void setup() {
        when(clusterUpgradePropertiesResolver.resolve(any(ClusterUpgradeValidationEvent.class))).thenAnswer(invocation ->
                ((ClusterUpgradeValidationEvent) invocation.getArgument(0)).getClusterUpgradeProperties());
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
    }

    @Test
    void testDatalakeReachableThenPass() {
        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(START_CLUSTER_UPGRADE_SERVICE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(dataLakeStatusCheckerService).validateDatalakeReachable(stackView);
    }

    @Test
    void testDatalakeNotReachableThenFail() {
        doThrow(new DatalakeNotReachableException("Data Lake is not reachable", new RuntimeException("cause")))
                .when(dataLakeStatusCheckerService).validateDatalakeReachable(stackView);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        Exception exception = ((ClusterUpgradeValidationFailureEvent) nextFlowStepSelector).getException();
        assertInstanceOf(UpgradeValidationFailedException.class, exception);
        assertEquals("Data Lake is not reachable", exception.getMessage());
    }

    private HandlerEvent<ClusterUpgradeDatalakeReachabilityValidationEvent> getHandlerEvent() {
        ClusterUpgradeProperties clusterUpgradeProperties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ClusterUpgradeDatalakeReachabilityValidationEvent event =
                new ClusterUpgradeDatalakeReachabilityValidationEvent(STACK_ID, clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties);
        HandlerEvent<ClusterUpgradeDatalakeReachabilityValidationEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(event);
        return handlerEvent;
    }
}
