package com.sequenceiq.freeipa.flow.freeipa.binduser.create.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateLdapBindUserEvent;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Service;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class LdapBindUserCreationHandlerTest {
    @Mock
    private StackService stackService;

    @Mock
    private LdapConfigV1Service ldapConfigV1Service;

    @Mock
    private LdapConfigService ldapConfigService;

    @InjectMocks
    private LdapBindUserCreationHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(CreateLdapBindUserEvent.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEventCreation() {
        CreateBindUserEvent createBindUserEvent = new CreateBindUserEvent("selector", 1L, "acc", "opid", "suffix", "envcrn");
        CreateLdapBindUserEvent createLdapBindUserEvent = new CreateLdapBindUserEvent(createBindUserEvent);
        Exception exception = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(1L, exception, new Event<>(createLdapBindUserEvent));

        assertTrue(result instanceof CreateBindUserFailureEvent);
        CreateBindUserFailureEvent failureEvent = (CreateBindUserFailureEvent) result;
        assertEquals(CreateBindUserFlowEvent.CREATE_BIND_USER_FAILED_EVENT.event(), failureEvent.selector());
        assertEquals("LDAP bind user creation failed for suffix with test", failureEvent.getFailureMessage());
        assertEquals(exception, failureEvent.getException());
    }

    @Test
    public void testEventSentIfConfigAlreadyExists() {
        CreateBindUserEvent createBindUserEvent = new CreateBindUserEvent("selector", 1L, "acc", "opid", "suffix", "envcrn");
        CreateLdapBindUserEvent createLdapBindUserEvent = new CreateLdapBindUserEvent(createBindUserEvent);
        HandlerEvent<CreateLdapBindUserEvent> handlerEvent = new HandlerEvent<>(new Event<>(createLdapBindUserEvent));
        when(ldapConfigService.find(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId(), createBindUserEvent.getSuffix()))
                .thenReturn(Optional.of(new LdapConfig()));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertTrue(selectable instanceof CreateBindUserEvent);
        CreateBindUserEvent event = (CreateBindUserEvent) selectable;
        assertEquals(CreateBindUserFlowEvent.CREATE_LDAP_BIND_USER_FINISHED_EVENT.event(), event.selector());
        assertEquals(createBindUserEvent.getOperationId(), event.getOperationId());
        assertEquals(createBindUserEvent.getSuffix(), event.getSuffix());
        assertEquals(createBindUserEvent.getAccountId(), event.getAccountId());
        assertEquals(createBindUserEvent.getResourceId(), event.getResourceId());
        verifyNoInteractions(stackService);
        verifyNoInteractions(ldapConfigV1Service);
    }

    @Test
    public void testCreated() throws FreeIpaClientException {
        CreateBindUserEvent createBindUserEvent = new CreateBindUserEvent("selector", 1L, "acc", "opid", "suffix", "envcrn");
        CreateLdapBindUserEvent createLdapBindUserEvent = new CreateLdapBindUserEvent(createBindUserEvent);
        HandlerEvent<CreateLdapBindUserEvent> handlerEvent = new HandlerEvent<>(new Event<>(createLdapBindUserEvent));
        when(ldapConfigService.find(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId(), createBindUserEvent.getSuffix()))
                .thenReturn(Optional.empty());
        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountId(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId())).thenReturn(stack);

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertTrue(selectable instanceof CreateBindUserEvent);
        CreateBindUserEvent event = (CreateBindUserEvent) selectable;
        assertEquals(CreateBindUserFlowEvent.CREATE_LDAP_BIND_USER_FINISHED_EVENT.event(), event.selector());
        assertEquals(createBindUserEvent.getOperationId(), event.getOperationId());
        assertEquals(createBindUserEvent.getSuffix(), event.getSuffix());
        assertEquals(createBindUserEvent.getAccountId(), event.getAccountId());
        assertEquals(createBindUserEvent.getResourceId(), event.getResourceId());
        verify(stackService).getByEnvironmentCrnAndAccountId(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId());
        verify(ldapConfigV1Service).createNewLdapConfig(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getSuffix(), stack, true);
    }

    @Test
    public void testCreateFailed() throws FreeIpaClientException {
        CreateBindUserEvent createBindUserEvent = new CreateBindUserEvent("selector", 1L, "acc", "opid", "suffix", "envcrn");
        CreateLdapBindUserEvent createLdapBindUserEvent = new CreateLdapBindUserEvent(createBindUserEvent);
        HandlerEvent<CreateLdapBindUserEvent> handlerEvent = new HandlerEvent<>(new Event<>(createLdapBindUserEvent));
        when(ldapConfigService.find(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId(), createBindUserEvent.getSuffix()))
                .thenReturn(Optional.empty());
        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountId(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId())).thenReturn(stack);
        when(ldapConfigV1Service.createNewLdapConfig(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getSuffix(), stack, true))
                .thenThrow(new FreeIpaClientException("test"));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertTrue(selectable instanceof CreateBindUserEvent);
        CreateBindUserEvent event = (CreateBindUserEvent) selectable;
        assertEquals(CreateBindUserFlowEvent.CREATE_BIND_USER_FAILED_EVENT.event(), event.selector());
        assertEquals(createBindUserEvent.getOperationId(), event.getOperationId());
        assertEquals(createBindUserEvent.getSuffix(), event.getSuffix());
        assertEquals(createBindUserEvent.getAccountId(), event.getAccountId());
        assertEquals(createBindUserEvent.getResourceId(), event.getResourceId());
        verify(stackService).getByEnvironmentCrnAndAccountId(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getAccountId());
        verify(ldapConfigV1Service).createNewLdapConfig(createBindUserEvent.getEnvironmentCrn(), createBindUserEvent.getSuffix(), stack, true);
    }
}