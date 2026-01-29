package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication.RemoveReplicationAgreementsRequest;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaTopologyService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RemoveReplicationAgreementsHandlerTest {

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
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaTopologyService freeIpaTopologyService;

    @InjectMocks
    private RemoveReplicationAgreementsHandler underTest;

    @Test
    void testRemoveReplicationAgreementsSuccess() throws Exception {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveReplicationAgreementsRequest request = new RemoveReplicationAgreementsRequest(cleanupEvent);
        Stack stack = mock(Stack.class);
        when(stackService.getStackById(any())).thenReturn(stack);
        underTest.accept(new Event<>(request));
        verify(freeIpaTopologyService).updateReplicationTopologyWithRetry(any(), any());
        verify(eventBus).notify(eq("REMOVEREPLICATIONAGREEMENTSRESPONSE"), ArgumentMatchers.<Event>any());
    }

    @Test
    void testRemoveReplicationAgreementsFailure() {
        CleanupEvent cleanupEvent =
                new CleanupEvent(STACK_ID, USERS, HOSTS, ROLES, IPS, STATES_TO_SKIP, ACCOUNT_ID, OPERATION_ID, CLUSTER_NAME, ENVIRONMENT_CRN);
        RemoveReplicationAgreementsRequest request = new RemoveReplicationAgreementsRequest(cleanupEvent);
        when(stackService.getStackById(any())).thenThrow(new RuntimeException("expected exception"));
        underTest.accept(new Event<>(request));
        verify(eventBus).notify(eq("REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT"), ArgumentMatchers.<Event>any());
    }
}