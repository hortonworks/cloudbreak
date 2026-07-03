package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.replicationcleanup.VerifyReplicationCleanupRequest;
import com.sequenceiq.freeipa.service.freeipa.cleanup.VerifyReplicationCleanupService;

@ExtendWith(MockitoExtension.class)
class VerifyReplicationCleanupHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private VerifyReplicationCleanupService verifyReplicationCleanupService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private VerifyReplicationCleanupHandler underTest;

    private VerifyReplicationCleanupRequest createRequest() {
        CleanupEvent cleanupEvent = new CleanupEvent(STACK_ID, Set.of(), Set.of("host1.example.com"), Set.of(), Set.of(), Set.of(),
                "account1", "op1", "", "env-crn");
        return new VerifyReplicationCleanupRequest(cleanupEvent);
    }

    @Test
    void testDoAcceptWhenSuccessThenNotifiesSuccessEvent() throws Exception {
        VerifyReplicationCleanupRequest request = createRequest();

        underTest.accept(new Event<>(request));

        verify(verifyReplicationCleanupService).verifyOnSurvivingMasters(STACK_ID, Set.of("host1.example.com"));
        verify(eventBus).notify(eq("VERIFYREPLICATIONCLEANUPRESPONSE"), any(Event.class));
    }

    @Test
    void testDoAcceptWhenOrchestratorExceptionThenNotifiesFailureEvent() throws Exception {
        VerifyReplicationCleanupRequest request = createRequest();

        doThrow(new CloudbreakOrchestratorFailedException("salt failed"))
                .when(verifyReplicationCleanupService).verifyOnSurvivingMasters(any(), any());

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("VERIFY_REPLICATION_CLEANUP_FAILED_EVENT"), eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        DownscaleFailureEvent failureEvent = (DownscaleFailureEvent) capturedEvent.getData();
        assertThat(failureEvent.getFailedPhase()).isEqualTo("Downscale Verify Replication Cleanup");
    }
}
