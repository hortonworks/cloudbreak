package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.api.v1.operation.model.OperationType.BIND_USER_CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.PollBindUserCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.ValidateKerberosConfigEvent;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationCheckerTask;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationPollerObject;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class PollBindUserCreationHandlerTest {

    @Mock
    private OperationV1Endpoint operationV1Endpoint;

    @Mock
    private PollingService<FreeIpaOperationPollerObject> freeIpaOperationChecker;

    @InjectMocks
    private PollBindUserCreationHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(PollBindUserCreationEvent.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        Event<PollBindUserCreationEvent> event = new Event<>(new PollBindUserCreationEvent(1L, "opId"));
        Exception e = new Exception();

        StackFailureEvent result = (StackFailureEvent) underTest.defaultFailureEvent(1L, e, event);

        assertEquals(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    public void testPollingSuccessful() {
        Event<PollBindUserCreationEvent> event = new Event<>(new PollBindUserCreationEvent(1L, "opId"));
        ArgumentCaptor<FreeIpaOperationPollerObject> captor = ArgumentCaptor.forClass(FreeIpaOperationPollerObject.class);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(FreeIpaOperationCheckerTask.class), captor.capture(), anyLong(), anyLong(), anyInt()))
        .thenReturn(Pair.of(PollingResult.SUCCESS, null));

        ValidateKerberosConfigEvent result = (ValidateKerberosConfigEvent) underTest.doAccept(new HandlerEvent<>(event));

        assertEquals(VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertTrue(result.doesFreeipaExistsForEnv());
        FreeIpaOperationPollerObject pollerObject = captor.getValue();
        assertEquals(BIND_USER_CREATE.name(), pollerObject.getOperationType());
        assertEquals("opId", pollerObject.getOperationId());
        assertEquals(operationV1Endpoint, pollerObject.getOperationV1Endpoint());
    }

    @Test
    public void testPollingFailed() {
        Event<PollBindUserCreationEvent> event = new Event<>(new PollBindUserCreationEvent(1L, "opId"));
        ArgumentCaptor<FreeIpaOperationPollerObject> captor = ArgumentCaptor.forClass(FreeIpaOperationPollerObject.class);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(FreeIpaOperationCheckerTask.class), captor.capture(), anyLong(), anyLong(), anyInt()))
        .thenReturn(Pair.of(PollingResult.FAILURE, new Exception("error")));

        StackFailureEvent result = (StackFailureEvent) underTest.doAccept(new HandlerEvent<>(event));

        assertEquals(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertEquals("Bind user creation failed with: error", result.getException().getMessage());
        FreeIpaOperationPollerObject pollerObject = captor.getValue();
        assertEquals(BIND_USER_CREATE.name(), pollerObject.getOperationType());
        assertEquals("opId", pollerObject.getOperationId());
        assertEquals(operationV1Endpoint, pollerObject.getOperationV1Endpoint());
    }

}