package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSaltStateApplyRequest;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigSaltStateApplyActionTest extends ModifyProxyConfigActionTest<ModifyProxyConfigRequest> {

    @InjectMocks
    private ModifyProxyConfigSaltStateApplyAction underTest;

    @Mock
    private ModifyProxyConfigRequest event;

    @Mock
    private Map<Object, Object> variables;

    @Override
    ModifyProxyConfigAction<ModifyProxyConfigRequest> getAction() {
        return underTest;
    }

    @Override
    ModifyProxyConfigRequest getEvent() {
        return event;
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, event, Map.of());

        verify(modifyProxyConfigStatusService).applyingSaltState(STACK_ID);
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigSaltStateApplyRequest.class)
                .extracting(ModifyProxyConfigSaltStateApplyRequest.class::cast)
                .returns(STACK_ID, StackEvent::getResourceId)
                .returns(PREVIOUS_PROXY_CONFIG_CRN, ModifyProxyConfigSaltStateApplyRequest::getPreviousProxyConfigCrn);
    }

    @Test
    void prepareExecution() {
        when(event.getPreviousProxyConfigCrn()).thenReturn(PREVIOUS_PROXY_CONFIG_CRN);

        underTest.prepareExecution(event, variables);

        verify(variables).put(ModifyProxyConfigAction.PREVIOUS_PROXY_CONFIG, PREVIOUS_PROXY_CONFIG_CRN);
    }

    @Test
    void prepareExecutionWithoutPreviousProxyConfigCrn() {
        when(event.getPreviousProxyConfigCrn()).thenReturn(null);

        underTest.prepareExecution(event, variables);

        verifyNoInteractions(variables);
    }

}
