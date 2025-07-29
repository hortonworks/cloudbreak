package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientCallable;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreSuccess;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientRetryService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCleanupAfterRestoreHandlerTest {
    private static final Long RESOURCE_ID = 1L;

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private CleanupService cleanupService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaClientRetryService retryService;

    @InjectMocks
    private FreeIpaCleanupAfterRestoreHandler underTest;

    @BeforeEach
    void init() throws FreeIpaClientException {
        lenient().doAnswer(invocation -> invocation.getArgument(0, FreeIpaClientCallable.class).run())
                .when(retryService).retryWhenRetryableWithValue(any(FreeIpaClientCallable.class));
    }

    @Test
    void testDoAcceptSuccess() throws FreeIpaClientException {
        FreeIpaCleanupAfterRestoreRequest request = new FreeIpaCleanupAfterRestoreRequest(RESOURCE_ID);
        HandlerEvent<FreeIpaCleanupAfterRestoreRequest> event = new HandlerEvent<>(new Event<>(request));

        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(RESOURCE_ID);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        FreeIpa freeIpa = mock(FreeIpa.class);
        InstanceMetaData pgw = mock(InstanceMetaData.class);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        IpaServer ipaServer1 = mock(IpaServer.class);
        IpaServer ipaServer2 = mock(IpaServer.class);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(freeIpaService.findByStackId(RESOURCE_ID)).thenReturn(freeIpa);
        when(stack.getPrimaryGatewayAndThrowExceptionIfEmpty()).thenReturn(pgw);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(pgw.getDiscoveryFQDN()).thenReturn("primary.gateway");
        when(ipaServer1.getFqdn()).thenReturn("server1");
        when(ipaServer2.getFqdn()).thenReturn("primary.gateway");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServer1, ipaServer2));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(FreeIpaCleanupAfterRestoreSuccess.class, result);
        verify(cleanupService).removeServers(RESOURCE_ID, Set.of("server1"));
        verify(cleanupService).removeDnsEntries(RESOURCE_ID, Set.of("server1"), Set.of(), freeIpa.getDomain(), ENVIRONMENT_CRN);
    }

    @Test
    void testDoAcceptFailure() throws FreeIpaClientException {
        FreeIpaCleanupAfterRestoreRequest request = new FreeIpaCleanupAfterRestoreRequest(RESOURCE_ID);
        HandlerEvent<FreeIpaCleanupAfterRestoreRequest> event = new HandlerEvent<>(new Event<>(request));

        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        InstanceMetaData pgw = mock(InstanceMetaData.class);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(freeIpaService.findByStackId(RESOURCE_ID)).thenReturn(freeIpa);
        when(stack.getPrimaryGatewayAndThrowExceptionIfEmpty()).thenReturn(pgw);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllServers()).thenThrow(new FreeIpaClientException("Error"));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(FreeIpaCleanupAfterRestoreFailed.class, result);
        verify(cleanupService, never()).removeServers(anyLong(), anySet());
        verify(cleanupService, never()).removeDnsEntries(anyLong(), anySet(), anySet(), anyString(), anyString());
    }

    @Test
    void testSelector() {
        String selector = underTest.selector();

        assertEquals(EventSelectorUtil.selector(FreeIpaCleanupAfterRestoreRequest.class), selector);
    }

    @Test
    void testFailurePayload() {
        Exception e = new Exception("fds");

        FreeIpaCleanupAfterRestoreFailed result = (FreeIpaCleanupAfterRestoreFailed) underTest.defaultFailureEvent(RESOURCE_ID, e,
                new Event<>(new FreeIpaCleanupAfterRestoreRequest(RESOURCE_ID)));

        assertEquals(RESOURCE_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
