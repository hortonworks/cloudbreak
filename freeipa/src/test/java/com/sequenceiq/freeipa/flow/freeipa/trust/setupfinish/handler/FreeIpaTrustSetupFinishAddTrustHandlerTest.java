package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.handler;

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
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddTrustSuccess;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustSetupSteps;

@ExtendWith(MockitoExtension.class)
class FreeIpaTrustSetupFinishAddTrustHandlerTest {
    private static final Long STACK_ID = 1L;

    @InjectMocks
    private FreeIpaTrustSetupFinishAddTrustHandler handler;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private TrustSetupSteps trustSetupSteps;

    @Mock
    private Event event;

    @BeforeEach
    void setUp() {
        FreeIpaTrustSetupFinishAddRequest request = new FreeIpaTrustSetupFinishAddRequest(STACK_ID);
        when(event.getData()).thenReturn(request);
    }

    @Test
    void testDoAcceptSuccess() throws Exception {
        HandlerEvent<FreeIpaTrustSetupFinishAddRequest> handlerEvent = new HandlerEvent<>(event);

        when(crossRealmTrustService.getTrustSetupSteps(STACK_ID)).thenReturn(trustSetupSteps);

        Selectable result = handler.doAccept(handlerEvent);

        assertTrue(result instanceof FreeIpaTrustSetupFinishAddTrustSuccess);
        verify(trustSetupSteps).addTrust(STACK_ID);
        verify(trustSetupSteps).validateTrust(STACK_ID);
    }

    @Test
    void testDoAcceptFailure() {
        HandlerEvent<FreeIpaTrustSetupFinishAddRequest> handlerEvent = new HandlerEvent<>(event);

        when(crossRealmTrustService.getTrustSetupSteps(STACK_ID)).thenThrow(new RuntimeException("error"));

        Selectable result = handler.doAccept(handlerEvent);

        assertTrue(result instanceof FreeIpaTrustSetupFinishAddTrustFailed);
    }
}