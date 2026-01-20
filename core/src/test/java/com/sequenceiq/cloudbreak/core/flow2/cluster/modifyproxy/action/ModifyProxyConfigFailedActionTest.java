package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

class ModifyProxyConfigFailedActionTest extends ModifyProxyConfigActionTest<StackFailureEvent> {

    private static final Exception CAUSE = new Exception("cause");

    @InjectMocks
    private ModifyProxyConfigFailedAction underTest;

    @Mock
    private StackFailureEvent event;

    @Override
    ModifyProxyConfigFailedAction getAction() {
        return underTest;
    }

    @Override
    StackFailureEvent getEvent() {
        return event;
    }

    @BeforeEach
    void setUp() {
        super.setUp();
        lenient().when(event.getException()).thenReturn(CAUSE);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, event, Map.of());

        verify(modifyProxyConfigStatusService).failed(STACK_ID, CAUSE);
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT.selector(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);
    }
}
