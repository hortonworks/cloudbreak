package com.sequenceiq.cloudbreak.core.flow2;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.mockito.ArgumentCaptor;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;

/**
 * Shared helpers for the core flow integration tests that boot a real state machine and drive a single flow.
 * Subclasses keep their own {@code @ActiveProfiles}, {@code @ExtendWith}, and nested {@code @TestConfiguration};
 * Spring autowires the inherited {@link #flowRegister} / {@link #flowLogRepository} fields.
 */
public abstract class AbstractFlowIntegrationTest {

    @Inject
    protected FlowRegister flowRegister;

    @Inject
    protected FlowLogRepository flowLogRepository;

    /**
     * Blocks until the flow leaves the running register, failing loudly with a {@code ConditionTimeoutException}
     * if it does not finalize within the timeout (a deadlocked flow), rather than silently proceeding.
     */
    protected void letItFlow(FlowIdentifier flowIdentifier) {
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> flowRegister.get(flowIdentifier.getPollableId()) == null);
    }

    protected void flowFinishedSuccessfully() {
        assertFlowFinalized();
    }

    /**
     * Asserts the flow finalized (reached a terminal state and left the register) without deadlocking. Named
     * neutrally for failure-path tests, which additionally verify the flow's own failure signal - its
     * failed-state action firing the FAILED event - to prove the failure path, not the happy path, was taken.
     */
    protected void flowFinalized() {
        assertFlowFinalized();
    }

    private void assertFlowFinalized() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, atLeastOnce()).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }
}
