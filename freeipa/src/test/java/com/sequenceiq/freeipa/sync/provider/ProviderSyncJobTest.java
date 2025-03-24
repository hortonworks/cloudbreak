package com.sequenceiq.freeipa.sync.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.impl.JobDetailImpl;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ProviderSyncJobTest {

    private static final Long LOCAL_ID = 1L;

    private static final String ACCOUNT_ID = "account-id";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String MODIFIED_INTERNAL_CRN = "crn:cdp:iam:us-west-1:account-id:user:__internal__actor__";

    @Mock
    private StackService stackDtoService;

    @Mock
    private ProviderSyncService providerSyncService;

    @Mock
    private ProviderSyncJobService providerSyncJobService;

    @Mock
    private ProviderSyncConfig providerSyncConfig;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private Stack stack;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private InternalCrnModifier internalCrnModifier;

    @InjectMocks
    private ProviderSyncJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
        when(stackDtoService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        StackStatus stackStatus = mock(StackStatus.class);
        when(stack.getStackStatus()).thenReturn(stackStatus);
    }

    @Test
    void testExecuteWhenProviderSyncDisabled() {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(false);

        underTest.executeTracedJob(jobExecutionContext);

        verify(providerSyncService, never()).syncResources(any());
        verify(providerSyncJobService, never()).unschedule(any());
    }

    @Test
    void testExecuteWhenFlowIsRunning() {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(true);

        underTest.executeTracedJob(jobExecutionContext);

        verify(providerSyncService, never()).syncResources(any());
        verify(providerSyncJobService, never()).unschedule(any());
    }

    @Test
    void testExecuteWhenStackStatusUnschedulable() {

        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        when(stack.getStackStatus().getStatus()).thenReturn(Status.DELETE_COMPLETED);
        when(jobExecutionContext.getJobDetail()).thenReturn(new JobDetailImpl());

        underTest.executeTracedJob(jobExecutionContext);

        verify(providerSyncService, never()).syncResources(any());
        verify(providerSyncJobService, times(1)).unschedule(any());
    }

    @Test
    void testExecuteWhenStackStatusAvailable() {

        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        StackStatus stackStatus = mock(StackStatus.class);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
        when(internalCrnModifier.changeAccountIdInCrnString(eq(INTERNAL_CRN), eq(ACCOUNT_ID))).thenReturn(Crn.fromString(MODIFIED_INTERNAL_CRN));

        underTest.executeTracedJob(jobExecutionContext);

        verify(providerSyncService, times(1)).syncResources(eq(stack));
        verify(providerSyncJobService, never()).unschedule(any());
    }

    @Test
    void testExecuteWhenStackStatusNotAvailable() {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        when(stack.getStackStatus().getStatus()).thenReturn(Status.STOPPED);

        underTest.executeTracedJob(jobExecutionContext);

        verify(providerSyncService, never()).syncResources(any());
        verify(providerSyncJobService, never()).unschedule(any());
    }
}