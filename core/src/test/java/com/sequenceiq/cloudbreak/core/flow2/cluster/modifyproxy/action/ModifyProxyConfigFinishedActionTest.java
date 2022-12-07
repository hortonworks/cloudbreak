package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import static org.mockito.Mockito.verify;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigFinishedActionTest extends ModifyProxyConfigActionTest<StackEvent> {

    @InjectMocks
    private ModifyProxyConfigFinishedAction underTest;

    @Mock
    private StackEvent event;

    @Override
    ModifyProxyConfigAction<StackEvent> getAction() {
        return underTest;
    }

    @Override
    StackEvent getEvent() {
        return event;
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, event, Map.of());

        verify(modifyProxyConfigStatusService).success(STACK_ID);
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FINISHED_EVENT.selector(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);
    }

}
