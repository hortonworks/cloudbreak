package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINALIZE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINALIZING_FINISHED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmFinalizingHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private UpgradeCcmFinalizingHandler underTest;

    @Captor
    private ArgumentCaptor<Event<UpgradeCcmEvent>> eventCaptor;

    private Event<UpgradeCcmEvent> event;

    @BeforeEach
    void setUp() {
        UpgradeCcmEvent upgradeCcmEvent = new UpgradeCcmEvent("selector", STACK_ID, Tunnel.CCM,
                LocalDateTime.now());
        event = new Event<>(upgradeCcmEvent);
    }

    @Test
    void testAcceptance() throws CloudbreakOrchestratorException {
        underTest.accept(event);
        verify(upgradeCcmService).changeTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        verify(upgradeCcmService).finalizeConfiguration(STACK_ID);
        verify(eventBus).notify(eq(UPGRADE_CCM_FINALIZING_FINISHED_EVENT.event()), eventCaptor.capture());
        Event<UpgradeCcmEvent> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(UPGRADE_CCM_FINALIZING_FINISHED_EVENT.event());
    }

    @Test
    void testAcceptanceFinalizeFailed() throws CloudbreakOrchestratorException {
        doThrow(new CloudbreakOrchestratorCancelledException("")).when(upgradeCcmService).finalizeConfiguration(STACK_ID);
        underTest.accept(event);
        verify(upgradeCcmService).changeTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        verify(upgradeCcmService).finalizeConfiguration(STACK_ID);
        ArgumentCaptor<Event<UpgradeCcmFailureEvent>> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(UPGRADE_CCM_FINALIZE_FAILED_EVENT.event()), captor.capture());
        Event<UpgradeCcmFailureEvent> eventResult = captor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(UPGRADE_CCM_FINALIZE_FAILED_EVENT.event());
    }
}