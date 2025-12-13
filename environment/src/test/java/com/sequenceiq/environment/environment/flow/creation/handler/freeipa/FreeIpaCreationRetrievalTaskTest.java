package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
class FreeIpaCreationRetrievalTaskTest {

    private static final long ENV_ID = 243_937_713L;

    private static final String ENV_CRN = "envCrn";

    private static final String FREE_IPA_NAME = "freeIpaName";

    private static final String FREE_IPA_CRN = "freeIpaCrn";

    private final FreeIpaService freeIpaService = mock(FreeIpaService.class);

    private final FreeIpaCreationRetrievalTask underTest = new FreeIpaCreationRetrievalTask(freeIpaService);

    @AfterEach
    void cleanup() {
        EnvironmentInMemoryStateStore.delete(ENV_ID);
    }

    @Test
    void testExitPollingWhenFreeIpaClusterIsInCreateInProgressState() {
        EnvironmentInMemoryStateStore.put(ENV_ID, PollGroup.CANCELLED);
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN);

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }

    @Test
    void testExitPollingWhenFreeIpaClusterIsInDeleteRelatedState() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN);

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }

    @Test
    void testCheckStatusWithMissingFreeIpa() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.empty());

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testCheckStatusWithDeleteInProgressState() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN);
        DescribeFreeIpaResponse freeIpa = new DescribeFreeIpaResponse();
        freeIpa.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        freeIpa.setStatus(DELETE_IN_PROGRESS);
        freeIpa.setName(FREE_IPA_NAME);
        freeIpa.setCrn(FREE_IPA_CRN);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeIpa));

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testCheckStatusWithFailedState() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN);
        DescribeFreeIpaResponse freeIpa = new DescribeFreeIpaResponse();
        freeIpa.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        freeIpa.setStatus(DELETE_IN_PROGRESS);
        freeIpa.setName(FREE_IPA_NAME);
        freeIpa.setCrn(FREE_IPA_CRN);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeIpa));

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }
}
