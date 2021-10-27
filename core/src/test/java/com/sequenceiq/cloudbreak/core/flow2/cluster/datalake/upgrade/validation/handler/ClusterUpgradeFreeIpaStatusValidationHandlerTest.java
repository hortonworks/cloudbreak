package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUpgradeFreeIpaStatusValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String ENV_CRN = "ENV_CRN";

    @InjectMocks
    private ClusterUpgradeFreeIpaStatusValidationHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private FreeipaService freeipaService;

    @Mock
    private StackView stackView;

    @Before
    public void setup() {
        when(stackService.getViewByIdWithoutAuth(STACK_ID)).thenReturn(stackView);
        when(stackView.getEnvironmentCrn()).thenReturn(ENV_CRN);
    }

    @Test
    public void testFreeIpaValidationReturnsNotAvailableThenThrowError() {

        when(freeipaService.freeipaStatusInDesiredState(ENV_CRN, Set.of(Status.AVAILABLE))).thenReturn(false);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(freeipaService).freeipaStatusInDesiredState(ENV_CRN, Set.of(Status.AVAILABLE));
        verify(stackService).getViewByIdWithoutAuth(STACK_ID);
    }

    @Test
    public void testFreeIpaValidationReturnsAvailableThenPass() {

        when(freeipaService.freeipaStatusInDesiredState(ENV_CRN, Set.of(Status.AVAILABLE))).thenReturn(true);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(freeipaService).freeipaStatusInDesiredState(ENV_CRN, Set.of(Status.AVAILABLE));
        verify(stackService).getViewByIdWithoutAuth(STACK_ID);
    }

    private HandlerEvent<ClusterUpgradeFreeIpaStatusValidationEvent> getHandlerEvent() {
        ClusterUpgradeFreeIpaStatusValidationEvent clusterUpgradeImageValidationEvent =
                new ClusterUpgradeFreeIpaStatusValidationEvent(1L);
        HandlerEvent<ClusterUpgradeFreeIpaStatusValidationEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(clusterUpgradeImageValidationEvent);
        return handlerEvent;
    }
}