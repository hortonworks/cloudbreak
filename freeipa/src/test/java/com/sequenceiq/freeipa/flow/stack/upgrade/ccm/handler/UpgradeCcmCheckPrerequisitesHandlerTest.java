package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmCheckPrerequisitesHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<UpgradeCcmEvent>> eventCaptor;

    @InjectMocks
    private UpgradeCcmCheckPrerequisitesHandler underTest;

    private Event<UpgradeCcmEvent> event;

    @BeforeEach
    void setUp() {
        UpgradeCcmEvent upgradeCcmEvent = new UpgradeCcmEvent("selector", STACK_ID, Tunnel.CCM, null);
            event = new Event<>(upgradeCcmEvent);
    }

    @Test
    void checkPrerequisitesInAnyCase() {
        underTest.accept(event);
        verify(upgradeCcmService).checkPrerequsities(STACK_ID, Tunnel.CCM);
        verify(eventBus).notify(eq(UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT.event()), eventCaptor.capture());
        Event<UpgradeCcmEvent> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().getOldTunnel()).isEqualTo(Tunnel.CCM);
        assertThat(eventResult.getData().selector()).isEqualTo(UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT.event());
        assertThat(eventResult.getData().getResourceId()).isEqualTo(STACK_ID);
    }
}
