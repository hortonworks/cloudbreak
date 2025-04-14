package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.AvailabilityChecker;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshJobTest {
    private static final Long LOCAL_ID = 1L;

    private static final String ACCOUNT_ID = "account-id";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String MODIFIED_INTERNAL_CRN = "crn:cdp:iam:us-west-1:account-id:user:__internal__actor__";

    private static final String FLOW_CHAIN_ID = "flowChainId";

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
    private JobExecutionContext jobExecutionContext;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private AvailabilityChecker availabilityChecker;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private DynamicEntitlementRefreshJob underTest;

    @Mock
    private Operation operation;

    @Mock
    private FlowIdentifier flowIdentifier;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
        lenient().when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        lenient().when(operationService.getOperationForAccountIdAndOperationId(ACCOUNT_ID, FLOW_CHAIN_ID)).thenReturn(operation);
        lenient().when(operation.getStatus()).thenReturn(OperationState.RUNNING);
        lenient().when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        lenient().when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap());
        lenient().when(flowIdentifier.getPollableId()).thenReturn(FLOW_CHAIN_ID);
    }

    @Test
    void testWhenFeatureFlagIsDisabled() {
        Stack stack = stack(Status.AVAILABLE);
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.FALSE);
        underTest.executeJob(jobExecutionContext);
        verify(flowLogService, never()).isOtherFlowRunning(any());
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(availabilityChecker, never()).isRequiredPackagesInstalled(any(), any());
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenFlowIsRunning() {
        Stack stack = stack(Status.AVAILABLE);
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(flowLogService.isOtherFlowRunning(LOCAL_ID)).thenReturn(Boolean.TRUE);

        underTest.executeJob(jobExecutionContext);
        verify(flowLogService, times(1)).isOtherFlowRunning(eq(LOCAL_ID));
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(availabilityChecker, never()).isRequiredPackagesInstalled(any(), any());
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenClusterRunningAndRescheduleLastFailed() throws JobExecutionException {
        Stack stack = stack(Status.AVAILABLE);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(DynamicEntitlementRefreshJob.FLOW_CHAIN_ID, "flowChainId");
        jobDataMap.putAsString(DynamicEntitlementRefreshJob.ERROR_COUNT, 4);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshService.previousFlowFailed(stack, FLOW_CHAIN_ID)).thenReturn(true);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack)).thenReturn(flowIdentifier);
        lenient().when(availabilityChecker.isRequiredPackagesInstalled(eq(stack), eq(Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey())))).thenReturn(true);

        underTest.executeJob(jobExecutionContext);

        verify(dynamicEntitlementRefreshJobService, never()).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, times(1)).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
        verify(dynamicEntitlementRefreshJobService).reScheduleWithBackoff(eq(LOCAL_ID), any(), eq(5));
    }

    @Test
    void testExecuteWhenClusterRunningAndRescheduleLastSuccess() throws JobExecutionException {
        Stack stack = stack(Status.AVAILABLE);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(DynamicEntitlementRefreshJob.FLOW_CHAIN_ID, "flowChainId");
        jobDataMap.putAsString(DynamicEntitlementRefreshJob.ERROR_COUNT, 4);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshService.previousFlowFailed(stack, FLOW_CHAIN_ID)).thenReturn(false);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack)).thenReturn(flowIdentifier);
        lenient().when(availabilityChecker.isRequiredPackagesInstalled(eq(stack), eq(Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey())))).thenReturn(true);

        underTest.executeJob(jobExecutionContext);

        verify(dynamicEntitlementRefreshJobService, never()).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, times(1)).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
        verify(dynamicEntitlementRefreshJobService).reScheduleWithBackoff(eq(LOCAL_ID), any(), eq(0));
    }

    @Test
    void testExecuteWhenClusterDeleted() {
        Stack stack = stack(Status.DELETE_COMPLETED);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(flowLogService.isOtherFlowRunning(LOCAL_ID)).thenReturn(Boolean.FALSE);
        lenient().when(availabilityChecker.isRequiredPackagesInstalled(eq(stack), eq(Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey())))).thenReturn(true);
        underTest.executeJob(jobExecutionContext);
        verify(flowLogService, times(1)).isOtherFlowRunning(eq(LOCAL_ID));
        verify(dynamicEntitlementRefreshJobService, times(1)).unschedule(eq(jobKey));
        verify(availabilityChecker, never()).isRequiredPackagesInstalled(any(), any());
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenRequiredPackagesAreNotInstalled() {
        Stack stack = stack(Status.AVAILABLE);
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(availabilityChecker.isRequiredPackagesInstalled(eq(stack), eq(Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey())))).thenReturn(false);
        underTest.executeJob(jobExecutionContext);
        verify(flowLogService, times(1)).isOtherFlowRunning(eq(LOCAL_ID));
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(availabilityChecker, times(1)).isRequiredPackagesInstalled(eq(stack), any());
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenClusterNotAvailable() {
        Stack stack = stack(Status.STOPPED);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(LOCAL_ID)).thenReturn(Boolean.FALSE);
        when(availabilityChecker.isRequiredPackagesInstalled(eq(stack), eq(Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey())))).thenReturn(true);
        underTest.executeJob(jobExecutionContext);
        verify(flowLogService, times(1)).isOtherFlowRunning(eq(LOCAL_ID));
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(availabilityChecker, times(1)).isRequiredPackagesInstalled(eq(stack), any());
        verify(dynamicEntitlementRefreshService, times(1)).getChangedWatchedEntitlementsAndStoreNewFromUms(eq(stack));
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenClusterRunning() {
        Stack stack = stack(Status.AVAILABLE);
        when(stackService.getByIdWithListsInTransaction(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack)).thenReturn(flowIdentifier);
        lenient().when(availabilityChecker.isRequiredPackagesInstalled(eq(stack), eq(Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey())))).thenReturn(true);

        underTest.executeJob(jobExecutionContext);
        verify(flowLogService, times(1)).isOtherFlowRunning(eq(LOCAL_ID));
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(availabilityChecker, times(1)).isRequiredPackagesInstalled(eq(stack), any());
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(eq(stack));
        verify(dynamicEntitlementRefreshService, times(1)).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
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