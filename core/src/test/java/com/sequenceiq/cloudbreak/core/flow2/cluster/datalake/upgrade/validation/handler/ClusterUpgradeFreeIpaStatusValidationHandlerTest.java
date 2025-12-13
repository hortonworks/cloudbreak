package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeFreeIpaStatusValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String ENV_CRN = "ENV_CRN";

    private static final String STACK_NAME = "STACK_NAME";

    @InjectMocks
    private ClusterUpgradeFreeIpaStatusValidationHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private FreeipaService freeipaService;

    @Mock
    private StackView stackView;

    @BeforeEach
    public void setup() {
        when(stackService.getViewByIdWithoutAuth(STACK_ID)).thenReturn(stackView);
        when(stackView.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stackView.getName()).thenReturn(STACK_NAME);
    }

    @Test
    void testFreeIpaValidationReturnsNotAvailableThenThrowError() {

        when(freeipaService.checkFreeipaRunning(ENV_CRN, STACK_NAME)).thenReturn(false);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(freeipaService).checkFreeipaRunning(ENV_CRN, STACK_NAME);
        verify(stackService).getViewByIdWithoutAuth(STACK_ID);
    }

    @Test
    void testFreeIpaValidationReturnsAvailableThenPass() {

        when(freeipaService.checkFreeipaRunning(ENV_CRN, STACK_NAME)).thenReturn(true);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(freeipaService).checkFreeipaRunning(ENV_CRN, STACK_NAME);
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