package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerSuccess;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;

@ExtendWith(MockitoExtension.class)
class FreeIpaTrustSetupPrepareServerHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private FreeIpaTrustSetupPrepareServerHandler handler;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private TrustProvider trustProvider;

    @Mock
    private Event event;

    @BeforeEach
    void setUp() {
        FreeIpaTrustSetupPrepareServerRequest request = new FreeIpaTrustSetupPrepareServerRequest(STACK_ID);
        when(event.getData()).thenReturn(request);
    }

    @Test
    void testDoAcceptSuccess() throws Exception {
        HandlerEvent<FreeIpaTrustSetupPrepareServerRequest> handlerEvent = new HandlerEvent<>(event);

        when(crossRealmTrustService.getTrustProvider(STACK_ID)).thenReturn(trustProvider);

        Selectable result = handler.doAccept(handlerEvent);

        assertTrue(result instanceof FreeIpaTrustSetupPrepareServerSuccess);
        verify(trustProvider).prepare(STACK_ID);
    }

    @Test
    void testDoAcceptFailure() {
        HandlerEvent<FreeIpaTrustSetupPrepareServerRequest> handlerEvent = new HandlerEvent<>(event);

        when(crossRealmTrustService.getTrustProvider(STACK_ID)).thenThrow(new RuntimeException("error"));

        Selectable result = handler.doAccept(handlerEvent);

        assertTrue(result instanceof FreeIpaTrustSetupPrepareServerFailed);
    }
}