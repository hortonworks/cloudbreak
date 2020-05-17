package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersRequest;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class ServerRemoveHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final Set<String> USERS = Set.of();

    private static final Set<String> HOSTS = Set.of("example1.com", "example2.com");

    private static final Set<String> ROLES = Set.of();

    private static final Set<String> IPS = Set.of();

    private static final Set<String> STATES_TO_SKIP = Set.of();

    private static final String ACCOUNT_ID = "1";

    private static final String OPERATION_ID = "1";

    private static final String CLUSTER_NAME = "";

    private static final String ENVIRONMENT_CRN = "env-crn";

    @Mock
    private CleanupService cleanupService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ServerRemoveHandler underTest;

    @Test
    void testSendsSuccessMessageWhenAllServersSucceed() throws FreeIpaClientException {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveServersRequest request = new RemoveServersRequest(cleanupEvent);
        Set<String> successList = HOSTS;
        Map<String, String> errorMap = Map.of();
        when(cleanupService.removeServers(any(), any())).thenReturn(Pair.of(successList, errorMap));
        underTest.accept(new Event<>(request));
        verify(cleanupService, times(1)).removeServers(eq(STACK_ID), eq(HOSTS));
        verify(eventBus).notify(eq("REMOVESERVERSRESPONSE"), ArgumentMatchers.<Event>any());
    }

    @Test
    void testSendsFailureMessageWhenSomeServersFail() throws FreeIpaClientException {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveServersRequest request = new RemoveServersRequest(cleanupEvent);
        Set<String> successList = Set.of("example1.com");
        Map<String, String> errorMap = Map.of("example2.com", "expected exception");
        when(cleanupService.removeServers(any(), any())).thenReturn(Pair.of(successList, errorMap));
        underTest.accept(new Event<>(request));
        verify(cleanupService, times(1)).removeServers(eq(STACK_ID), eq(HOSTS));
        verify(eventBus).notify(eq("REMOVESERVERSRESPONSE_ERROR"), ArgumentMatchers.<Event>any());
    }

    @Test
    void testSendsFailureMessageWhenExceptionIsThrown() throws FreeIpaClientException {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveServersRequest request = new RemoveServersRequest(cleanupEvent);
        when(cleanupService.removeServers(any(), any())).thenThrow(new FreeIpaClientException("expected exception"));
        underTest.accept(new Event<>(request));
        verify(cleanupService, times(1)).removeServers(eq(STACK_ID), eq(HOSTS));
        verify(eventBus).notify(eq("REMOVESERVERSRESPONSE_ERROR"), ArgumentMatchers.<Event>any());
    }
}