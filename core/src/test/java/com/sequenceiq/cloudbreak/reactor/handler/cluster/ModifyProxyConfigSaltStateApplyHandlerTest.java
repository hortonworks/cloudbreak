package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSaltStateApplyRequest;
import com.sequenceiq.cloudbreak.service.proxy.ModifyProxyConfigService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigSaltStateApplyHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String PREVIOUS_PROXY_CRN = "proxy-crn";

    @Mock
    private ModifyProxyConfigService modifyProxyConfigService;

    @InjectMocks
    private ModifyProxyConfigSaltStateApplyHandler underTest;

    @Mock
    private Event<ModifyProxyConfigSaltStateApplyRequest> event;

    @Mock
    private ModifyProxyConfigSaltStateApplyRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(event.getData()).thenReturn(request);
        lenient().when(request.getResourceId()).thenReturn(STACK_ID);
        lenient().when(request.getPreviousProxyConfigCrn()).thenReturn(PREVIOUS_PROXY_CRN);
    }

    @Test
    void defaultFailureEvent() {
        Exception cause = new Exception("cause");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, cause, event);

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigFailureResponse.class)
                .extracting(ModifyProxyConfigFailureResponse.class::cast)
                .returns(STACK_ID, ModifyProxyConfigFailureResponse::getResourceId)
                .returns(cause, ModifyProxyConfigFailureResponse::getException);
    }

    @Test
    void doAcceptSuccess() throws Exception {
        Selectable result = underTest.doAccept(new HandlerEvent<>(event));

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigRequest.class)
                .extracting(ModifyProxyConfigRequest.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_ON_CM.event(), ModifyProxyConfigRequest::selector)
                .returns(STACK_ID, ModifyProxyConfigRequest::getResourceId)
                .returns(PREVIOUS_PROXY_CRN, ModifyProxyConfigRequest::getPreviousProxyConfigCrn);
        verify(modifyProxyConfigService).applyModifyProxyState(STACK_ID);
    }

    @Test
    void doAcceptFailure() throws Exception {
        RuntimeException cause = new RuntimeException("cause");
        doThrow(cause).when(modifyProxyConfigService).applyModifyProxyState(STACK_ID);

        Selectable result = underTest.doAccept(new HandlerEvent<>(event));

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigFailureResponse.class)
                .extracting(ModifyProxyConfigFailureResponse.class::cast)
                .returns(STACK_ID, ModifyProxyConfigFailureResponse::getResourceId)
                .returns(cause, ModifyProxyConfigFailureResponse::getException);
        verify(modifyProxyConfigService).applyModifyProxyState(STACK_ID);
    }

}
