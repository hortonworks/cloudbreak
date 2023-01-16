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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigOnCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigOnCmActionTest extends ModifyProxyConfigActionTest<ModifyProxyConfigRequest> {

    @InjectMocks
    private ModifyProxyConfigOnCmAction underTest;

    @Mock
    private ModifyProxyConfigRequest event;

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

        verify(modifyProxyConfigStatusService).updateClusterManager(STACK_ID);
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(ModifyProxyConfigOnCmRequest.class)
                .extracting(ModifyProxyConfigOnCmRequest.class::cast)
                .returns(STACK_ID, ModifyProxyConfigOnCmRequest::getResourceId)
                .returns(PREVIOUS_PROXY_CONFIG_CRN, ModifyProxyConfigOnCmRequest::getPreviousProxyConfigCrn);
    }
}
