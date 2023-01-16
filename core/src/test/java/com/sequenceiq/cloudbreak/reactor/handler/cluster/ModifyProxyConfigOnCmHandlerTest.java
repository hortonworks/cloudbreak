package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigOnCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.cloudbreak.service.proxy.ModifyProxyConfigService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigOnCmHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String PREVIOUS_PROXY_CONFIG_CRN = "crn";

    private static final RuntimeException CAUSE = new RuntimeException("cause");

    @InjectMocks
    private ModifyProxyConfigOnCmHandler underTest;

    @Mock
    private ModifyProxyConfigService modifyProxyConfigService;

    private ModifyProxyConfigOnCmRequest request;

    @BeforeEach
    void setUp() {
        request = new ModifyProxyConfigOnCmRequest(STACK_ID, PREVIOUS_PROXY_CONFIG_CRN);
    }

    @Test
    void defaultFailureEvent() {
        Selectable result = underTest.defaultFailureEvent(STACK_ID, CAUSE, Event.wrap(request));

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigFailureResponse.class)
                .extracting(ModifyProxyConfigFailureResponse.class::cast)
                .returns(STACK_ID, ModifyProxyConfigFailureResponse::getResourceId)
                .returns(CAUSE, ModifyProxyConfigFailureResponse::getException);
    }

    @Test
    void doAcceptSuccess() {
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        verify(modifyProxyConfigService).updateClusterManager(STACK_ID);
        assertThat(result)
                .isInstanceOf(ModifyProxyConfigSuccessResponse.class)
                .extracting(ModifyProxyConfigSuccessResponse.class::cast)
                .returns(STACK_ID, ModifyProxyConfigSuccessResponse::getResourceId);
    }

    @Test
    void doAcceptFailure() {
        doThrow(CAUSE).when(modifyProxyConfigService).updateClusterManager(STACK_ID);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        verify(modifyProxyConfigService).updateClusterManager(STACK_ID);
        assertThat(result)
                .isInstanceOf(ModifyProxyConfigFailureResponse.class)
                .extracting(ModifyProxyConfigFailureResponse.class::cast)
                .returns(STACK_ID, ModifyProxyConfigFailureResponse::getResourceId)
                .returns(CAUSE, ModifyProxyConfigFailureResponse::getException);
    }

}
