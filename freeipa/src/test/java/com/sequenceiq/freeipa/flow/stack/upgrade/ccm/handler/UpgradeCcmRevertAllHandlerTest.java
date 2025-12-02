package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.common.api.type.Tunnel.CCM;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmRevertAllHandlerTest {
    private static final long STACK_ID = 234L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private UpgradeCcmRevertAllHandler underTest;

    @Captor
    private ArgumentCaptor<Event<UpgradeCcmFailureEvent>> eventCaptor;

    private Event<UpgradeCcmFailureEvent> event;

    @BeforeEach
    void setUp() {
        UpgradeCcmFailureEvent upgradeCcmEvent = new UpgradeCcmFailureEvent(
                "selector",
                STACK_ID,
                CCM,
                UpgradeCcmCheckPrerequisitesHandler.class,
                new ArrayIndexOutOfBoundsException(""),
                LocalDateTime.now(),
                "reason",
                ERROR
        );
        event = new Event<>(upgradeCcmEvent);
    }

    @Test
    void testRevertAllHandlerAccept() throws CloudbreakOrchestratorException {
        underTest.accept(event);
        verify(upgradeCcmService).changeTunnel(STACK_ID, CCM);
        verify(upgradeCcmService).registerClusterProxyAndCheckHealth(STACK_ID);
        verify(upgradeCcmService).pushSaltStates(STACK_ID);
        verify(eventBus).notify(eq(UPGRADE_CCM_FAILED_EVENT.event()), eventCaptor.capture());
        Event<UpgradeCcmFailureEvent> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().selector()).isEqualTo(UPGRADE_CCM_FAILED_EVENT.event());
    }
}