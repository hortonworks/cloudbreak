package com.sequenceiq.cloudbreak.job.dynamicentitlement;

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
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshJobTest {
    private static final Long LOCAL_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Mock
    private DynamicEntitlementRefreshJobService dynamicEntitlementRefreshJobService;

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private JobDetail jobDetail;

    @InjectMocks
    private DynamicEntitlementRefreshJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
    }

    @Test
    void testExecuteWhenClusterRunning() throws JobExecutionException {
        StackDto stack = stack(Status.AVAILABLE);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, times(1)).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
    }

    @Test
    void testExecuteWhenClusterDeleted() throws JobExecutionException {
        StackDto stack = stack(Status.DELETE_COMPLETED);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(jobKey);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshJobService, times(1)).unschedule(eq(jobKey));
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
    }

    private StackDto stack(Status stackStatus) {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        lenient().when(stackDto.getId()).thenReturn(LOCAL_ID);
        lenient().when(stackDto.getStatus()).thenReturn(stackStatus);
        return stackDto;
    }

}