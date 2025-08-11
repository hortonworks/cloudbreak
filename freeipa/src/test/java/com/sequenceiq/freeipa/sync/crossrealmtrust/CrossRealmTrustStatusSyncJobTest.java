package com.sequenceiq.freeipa.sync.crossrealmtrust;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.provider.ProviderSyncJobService;

@ExtendWith(MockitoExtension.class)
class CrossRealmTrustStatusSyncJobTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private CrossRealmTrustStatusSyncService crossRealmTrustStatusSyncService;

    @Mock
    private ProviderSyncJobService jobService;

    @Mock
    private CrossRealmTrustStatusSyncConfig config;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @InjectMocks
    private CrossRealmTrustStatusSyncJob underTest;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private Stack stack;

    private CrossRealmTrust crossRealmTrust;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, "", DetailedStackStatus.AVAILABLE));
        lenient().when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(false);
        crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustStatus(TrustStatus.TRUST_ACTIVE);
        lenient().when(crossRealmTrustService.getByStackIdIfExists(STACK_ID)).thenReturn(Optional.of(crossRealmTrust));
        lenient().when(jobExecutionContext.getJobDetail()).thenReturn(mock());
        underTest.setLocalId(String.valueOf(STACK_ID));
    }

    @Test
    void configDisabled() {
        when(config.isEnabled()).thenReturn(false);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(jobService, crossRealmTrustStatusSyncService);
    }

    @Test
    void flowRunning() {
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(true);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(jobService, crossRealmTrustStatusSyncService);
    }

    @Test
    void statusUnschedulable() {
        stack.getStackStatus().setStatus(Status.DELETE_IN_PROGRESS);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(crossRealmTrustStatusSyncService);
        verify(jobService).unschedule(any());
    }

    @Test
    void noCrossRealmTrust() {
        lenient().when(crossRealmTrustService.getByStackIdIfExists(STACK_ID)).thenReturn(Optional.empty());

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(crossRealmTrustStatusSyncService);
        verify(jobService).unschedule(any());
    }

    @Test
    void crossRealmStatusIsNotSyncable() {
        crossRealmTrust.setTrustStatus(TrustStatus.TRUST_SETUP_IN_PROGRESS);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(jobService, crossRealmTrustStatusSyncService);
    }

    @Test
    void sync() {
        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(jobService);
        verify(crossRealmTrustStatusSyncService).syncCrossRealmTrustStatus(stack, crossRealmTrust);
    }

}
