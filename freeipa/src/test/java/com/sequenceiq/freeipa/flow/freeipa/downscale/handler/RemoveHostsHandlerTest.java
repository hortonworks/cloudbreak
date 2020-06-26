package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationRequest;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class RemoveHostsHandlerTest {

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
    private StackService stackService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private EventBus eventBus;

    @Mock
    private BootstrapService bootstrapService;

    @InjectMocks
    private RemoveHostsHandler underTest;

    @Test
    void testSendsSuccessMessageWhenAllServersSucceed() throws Exception {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveHostsFromOrchestrationRequest request = new RemoveHostsFromOrchestrationRequest(cleanupEvent);
        Stack stack = new Stack();
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(stack);
        underTest.accept(new Event<>(request));
        verify(eventBus, times(1)).notify(eq("REMOVEHOSTSFROMORCHESTRATIONSUCCESS"), ArgumentMatchers.<Event>any());
        verify(bootstrapService).bootstrap(any(), any());
        verify(hostOrchestrator).tearDown(any(), any(), any(), any());
    }

    @Test
    void testSendsFailureMessageWhenExceptionIsThrown() {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveHostsFromOrchestrationRequest request = new RemoveHostsFromOrchestrationRequest(cleanupEvent);
        when(stackService.getByIdWithListsInTransaction(any())).thenThrow(new RuntimeException("expected exception"));
        underTest.accept(new Event<>(request));
        verify(eventBus, times(1)).notify(eq("DOWNSCALEFAILUREEVENT"), ArgumentMatchers.<Event>any());
    }

}