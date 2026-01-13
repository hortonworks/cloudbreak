package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
class FreeIpaDeletionRetrievalTaskTest {

    private static final long ENV_ID = 1;

    private static final Long RESOURCE_ID = 123L;

    private static final String FLOW_ID = "9fee088f-f0d1-4309-9786-9ce373ca9dbb";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private final FreeIpaService freeIpaService = mock(FreeIpaService.class);

    private final FlowLogService flowLogService = mock(FlowLogService.class);

    private final FreeIpaDeletionRetrievalTask underTest = new FreeIpaDeletionRetrievalTask(freeIpaService, flowLogService);

    @AfterEach
    void cleanup() {
        EnvironmentInMemoryStateStore.delete(ENV_ID);
    }

    @Test
    void testWhenFreeIpaReturnsDeleteFailedShouldThrowException() {
        FreeIpaPollerObject freeIpaPollerObject =
                new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID), RESOURCE_ID);
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getStatus()).thenReturn(DELETE_FAILED);

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testWhenFreeIpaReturnsDeleteCompletedShouldReturnTrue() {
        FreeIpaPollerObject freeIpaPollerObject =
                new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID), RESOURCE_ID);
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getStatus()).thenReturn(DELETE_COMPLETED);

        assertTrue(underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testWhenFreeIpaReturnsNonFinalStatusAndFlowIsRunningShouldReturnFalse() {
        FreeIpaPollerObject freeIpaPollerObject =
                new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID), RESOURCE_ID);
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);
        FlowLog flowLog = mock(FlowLog.class);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getStatus()).thenReturn(DELETE_IN_PROGRESS);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(RESOURCE_ID)).thenReturn(List.of(flowLog));
        when(flowLog.getFlowId()).thenReturn(FLOW_ID);

        assertFalse(underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testWhenFreeIpaReturnsNotFinalStatusAndFlowIsFinishedShouldThrowException() {
        FreeIpaPollerObject freeIpaPollerObject =
                new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID), RESOURCE_ID);
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);
        FlowLog flowLog = mock(FlowLog.class);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getStatus()).thenReturn(DELETE_IN_PROGRESS);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(RESOURCE_ID)).thenReturn(List.of());

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testCheckStatusWithMissingFreeIpaShouldReturnTrue() {
        FreeIpaPollerObject freeIpaPollerObject =
                new FreeIpaPollerObject(ENV_ID, ENV_CRN, new FlowIdentifier(FlowType.FLOW, FLOW_ID), RESOURCE_ID);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.empty());

        assertTrue(underTest.checkStatus(freeIpaPollerObject));
    }

    @Test
    void testWhenFreeIpaReturnsNotFinalStatusAndFlowIdIsNullShouldThrowException() {
        FreeIpaPollerObject freeIpaPollerObject = new FreeIpaPollerObject(ENV_ID, ENV_CRN, null, RESOURCE_ID);
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getStatus()).thenReturn(DELETE_IN_PROGRESS);

        assertThrows(FreeIpaOperationFailedException.class, () -> underTest.checkStatus(freeIpaPollerObject));
    }
}
