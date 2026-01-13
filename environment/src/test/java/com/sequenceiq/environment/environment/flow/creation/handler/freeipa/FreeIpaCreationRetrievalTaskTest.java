package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
class FreeIpaCreationRetrievalTaskTest {

    private static final long ENV_ID = 243_937_713L;

    private static final String ENV_CRN = "envCrn";

    private static final String FREE_IPA_NAME = "freeIpaName";

    private static final String FLOW_ID = "1";

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
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID));

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }

    @Test
    void testExitPollingWhenFreeIpaClusterIsInDeleteRelatedState() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID));

        boolean result = underTest.exitPolling(freeIpaPollerObject);

        assertTrue(result);
    }

    @Test
    void testCheckStatusWithMissingFreeIpa() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID));
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.empty());

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testCheckStatusWithDeleteInProgressState() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID));
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
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID));
        DescribeFreeIpaResponse freeIpa = new DescribeFreeIpaResponse();
        freeIpa.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        freeIpa.setStatus(DELETE_IN_PROGRESS);
        freeIpa.setName(FREE_IPA_NAME);
        freeIpa.setCrn(FREE_IPA_CRN);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeIpa));

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testCheckStatusWithFlowIdentifierSuccess() {
        String flowId = "flow-123";
        DescribeFreeIpaResponse response = new DescribeFreeIpaResponse();
        response.setStatus(Status.CREATE_IN_PROGRESS);
        FreeIpaPollerObject pollerObject = mock(FreeIpaPollerObject.class);

        when(pollerObject.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(pollerObject.getFlowIdentifier()).thenReturn(new FlowIdentifier(FlowType.FLOW, flowId));
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(response));

        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowId("12345");
        flowCheckResponse.setFlowType(FlowType.FLOW.name());
        flowCheckResponse.setHasActiveFlow(false);
        flowCheckResponse.setLatestFlowFinalizedAndFailed(false);
        when(freeIpaService.checkFlow(any())).thenReturn(flowCheckResponse);

        boolean result = ThreadBasedUserCrnProvider.doAs("appletree", () -> underTest.checkStatus(pollerObject));

        assertTrue(result);
    }
}
