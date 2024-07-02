package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshJobTest {
    private static final Long LOCAL_ID = 1L;

    private static final String ACCOUNT_ID = "account-id";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String MODIFIED_INTERNAL_CRN = "crn:cdp:iam:us-west-1:account-id:user:__internal__actor__";

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

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private InternalCrnModifier internalCrnModifier;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private DynamicEntitlementRefreshJob underTest;

    @BeforeEach
    public void setUp() throws CloudbreakImageNotFoundException {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
        lenient().when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn(INTERNAL_CRN);
        lenient().when(internalCrnModifier.changeAccountIdInCrnString(eq(INTERNAL_CRN), eq(ACCOUNT_ID)))
                .thenReturn(Crn.fromString(MODIFIED_INTERNAL_CRN));
        Image image = mock(Image.class);
        lenient().when(image.getPackageVersions()).thenReturn(Map.of(ImagePackageVersion.CDP_PROMETHEUS.getKey(), "2.36.2"));
        lenient().when(imageService.getImage(eq(LOCAL_ID))).thenReturn(image);
    }

    @Test
    void testExecuteWhenFeatureIsDisabled() throws JobExecutionException, CloudbreakImageNotFoundException {
        StackDto stack = stack(Status.AVAILABLE);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.FALSE);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshConfig, times(1)).isDynamicEntitlementEnabled();
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(imageService, never()).getImage(any());
        verify(stack, never()).getInstanceGroupDtos();
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenClusterDeleted() throws JobExecutionException, CloudbreakImageNotFoundException {
        StackDto stack = stack(Status.DELETE_COMPLETED);
        JobKey jobKey = new JobKey(LOCAL_ID.toString(), "dynamic-entitlement-jobs");
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(jobKey);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshConfig, times(1)).isDynamicEntitlementEnabled();
        verify(dynamicEntitlementRefreshJobService, times(1)).unschedule(eq(jobKey));
        verify(imageService, never()).getImage(any());
        verify(stack, never()).getInstanceGroupDtos();
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenRequiredPackagesAreNotInstalled() throws JobExecutionException, CloudbreakImageNotFoundException {
        StackDto stack = stack(Status.AVAILABLE);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        Image image = mock(Image.class);
        when(image.getPackageVersions()).thenReturn(Map.of());
        when(imageService.getImage(eq(LOCAL_ID))).thenReturn(image);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshConfig, times(1)).isDynamicEntitlementEnabled();
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(imageService, times(1)).getImage(eq(LOCAL_ID));
        verify(stack, never()).getInstanceGroupDtos();
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenClusterNotAvailable() throws JobExecutionException, CloudbreakImageNotFoundException {
        StackDto stack = stack(Status.NODE_FAILURE);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshConfig, times(1)).isDynamicEntitlementEnabled();
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(imageService, times(1)).getImage(eq(LOCAL_ID));
        verify(stack, never()).getInstanceGroupDtos();
        verify(dynamicEntitlementRefreshService, times(1)).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenInstanceStopped() throws JobExecutionException, CloudbreakImageNotFoundException {
        StackDto stack = stack(Status.AVAILABLE);
        List<InstanceGroupDto> instanceGroupDtos = getInstanceGroupDtosWithSopped();
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroupDtos);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshConfig, times(1)).isDynamicEntitlementEnabled();
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(imageService, times(1)).getImage(eq(LOCAL_ID));
        verify(stack, times(1)).getInstanceGroupDtos();
        verify(dynamicEntitlementRefreshService, times(1)).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, never()).changeClusterConfigurationIfEntitlementsChanged(any());
    }

    @Test
    void testExecuteWhenClusterRunning() throws JobExecutionException, CloudbreakImageNotFoundException {
        StackDto stack = stack(Status.AVAILABLE);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeTracedJob(jobExecutionContext);
        verify(dynamicEntitlementRefreshConfig, times(1)).isDynamicEntitlementEnabled();
        verify(dynamicEntitlementRefreshJobService, never()).unschedule(any());
        verify(imageService, times(1)).getImage(eq(LOCAL_ID));
        verify(stack, times(1)).getInstanceGroupDtos();
        verify(dynamicEntitlementRefreshService, never()).getChangedWatchedEntitlementsAndStoreNewFromUms(any());
        verify(dynamicEntitlementRefreshService, times(1)).changeClusterConfigurationIfEntitlementsChanged(eq(stack));
    }

    private List<InstanceGroupDto> getInstanceGroupDtosWithSopped() {
        InstanceGroupDto ig1 = new InstanceGroupDto(mock(InstanceGroupView.class), List.of(getInstanceMetadataViewMock(InstanceStatus.SERVICES_HEALTHY)));
        InstanceGroupDto ig2 = new InstanceGroupDto(mock(InstanceGroupView.class),
                List.of(getInstanceMetadataViewMock(InstanceStatus.SERVICES_HEALTHY),
                        getInstanceMetadataViewMock(InstanceStatus.STOPPED)));
        return List.of(ig1, ig2);
    }

    private InstanceMetadataView getInstanceMetadataViewMock(InstanceStatus instanceStatus) {
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        when(instanceMetadataView.getInstanceStatus()).thenReturn(instanceStatus);
        return instanceMetadataView;
    }

    private StackDto stack(Status stackStatus) {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        lenient().when(stackDto.getId()).thenReturn(LOCAL_ID);
        lenient().when(stackDto.getStatus()).thenReturn(stackStatus);
        lenient().when(stackDto.getType()).thenReturn(WORKLOAD);
        lenient().when(stackDto.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getInstanceGroupDtos()).thenReturn(new ArrayList<>());
        return stackDto;
    }

}