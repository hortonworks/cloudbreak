package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

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
class DynamicEntitlementRefreshJobTest {
    private static final Long LOCAL_ID = 1L;

    private static final String ACCOUNT_ID = "account-id";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String MODIFIED_INTERNAL_CRN = "crn:cdp:iam:us-west-1:account-id:user:__internal__actor__";

    @Mock
    private StackService stackService;

    @Mock
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Mock
    private DynamicEntitlementRefreshJobService dynamicEntitlementRefreshJobService;

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private InternalCrnModifier internalCrnModifier;

    @InjectMocks
    private DynamicEntitlementRefreshJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
    }

    @Test
    void testExecuteWhenClusterRunning() {
        Stack stack = stack(Status.AVAILABLE);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(internalCrnModifier.changeAccountIdInCrnString(eq(INTERNAL_CRN), eq(ACCOUNT_ID))).thenReturn(Crn.fromString(MODIFIED_INTERNAL_CRN));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, times(1)).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
    }

    @Test
    void testExecuteWhenClusterDeleted() {
        Stack stack = stack(Status.DELETE_COMPLETED);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(flowLogService.isOtherFlowRunning(LOCAL_ID)).thenReturn(Boolean.FALSE);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshJobService, times(1)).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
    }

    @Test
    void testExecuteWhenClusterNotAvailable() {
        Stack stack = stack(Status.STOPPED);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(internalCrnModifier.changeAccountIdInCrnString(eq(INTERNAL_CRN), eq(ACCOUNT_ID))).thenReturn(Crn.fromString(MODIFIED_INTERNAL_CRN));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
        when(flowLogService.isOtherFlowRunning(LOCAL_ID)).thenReturn(Boolean.FALSE);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
        verify(dynamicEntitlementRefreshService, times(1)).getChangedWatchedEntitlements(eq(stack));
    }

    private Stack stack(Status status) {
        Stack stack = mock(Stack.class);
        StackStatus stackStatus = mock(StackStatus.class);
        lenient().when(stack.getId()).thenReturn(LOCAL_ID);
        lenient().when(stack.getStackStatus()).thenReturn(stackStatus);
        lenient().when(stackStatus.getStatus()).thenReturn(status);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        return stack;
    }

}