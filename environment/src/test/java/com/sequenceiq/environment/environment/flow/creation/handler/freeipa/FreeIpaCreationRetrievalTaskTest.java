package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
@ExtendWith(MockitoExtension.class)
class FreeIpaCreationRetrievalTaskTest {

    private static final String ENV_CRN = "envCrn";

    private static final long ENV_ID = 243_937_713L;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FreeIpaPollerObject freeIpaPollerObject;

    @InjectMocks
    private FreeIpaCreationRetrievalTask underTest;

    @BeforeEach
    void setup() {
        when(freeIpaPollerObject.getEnvironmentId()).thenReturn(ENV_ID);
    }

    @AfterEach
    void cleanup() {
        EnvironmentInMemoryStateStore.delete(ENV_ID);
    }

    @Test
    void testExitPollingWhenFreeIpaClusterIsInCreateInProgressState() {
        EnvironmentInMemoryStateStore.put(ENV_ID, PollGroup.CANCELLED);
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setName("aFreeIpaName");
        describeFreeIpaResponse.setCrn("aFreeIpaCRN");
        describeFreeIpaResponse.setEnvironmentCrn(ENV_CRN);
        describeFreeIpaResponse.setStatus(Status.CREATE_IN_PROGRESS);

        when(freeIpaPollerObject.getFreeIpaV1Endpoint().describe(anyString()))
                .thenReturn(describeFreeIpaResponse);

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"DELETE_IN_PROGRESS", "DELETE_COMPLETED"})
    void testExitPollingWhenFreeIpaClusterIsInDeleteRelatedState(Status deletedStatus) {
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setName("aFreeIpaName");
        describeFreeIpaResponse.setCrn("aFreeIpaCRN");
        describeFreeIpaResponse.setStatus(deletedStatus);

        when(freeIpaPollerObject.getFreeIpaV1Endpoint().describe(anyString()))
                .thenReturn(describeFreeIpaResponse);

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"UPDATE_FAILED", "CREATE_FAILED", "ENABLE_SECURITY_FAILED", "DELETE_FAILED", "START_FAILED", "STOP_FAILED"})
    void testExitPollingWhenFreeIpaClusterIsInFailedState(Status failedStatus) {
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setName("aFreeIpaName");
        describeFreeIpaResponse.setCrn("aFreeIpaCRN");
        describeFreeIpaResponse.setStatus(failedStatus);

        when(freeIpaPollerObject.getFreeIpaV1Endpoint().describe(anyString()))
                .thenReturn(describeFreeIpaResponse);

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }
}
