package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePremiumValidatorService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

import rx.Completable;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

@ExtendWith(MockitoExtension.class)
public class AzureUtilsTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String MAX_RESOURCE_NAME_LENGTH = "50";

    @Mock
    private AzurePremiumValidatorService azurePremiumValidatorService;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @InjectMocks
    private AzureUtils underTest;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
        RxJavaHooks.setOnIOScheduler(current -> Schedulers.immediate());
    }

    @AfterEach
    public void tearDown() {
        RxJavaHooks.reset();
    }

    @Test
    public void shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        CloudContext context = CloudContext.Builder.builder()
                .withId(7899L)
                .withName("thisisaverylongazureresourcenamewhichneedstobeshortened")
                .withCrn("crn")
                .withPlatform("dummy1")
                .withUserId(USER_ID)
                .withWorkspaceId(WORKSPACE_ID)
                .build();

        //WHEN
        String testResult = underTest.getStackName(context);

        //THEN
        assertNotNull(testResult, "The generated name must not be null!");
        assertEquals("thisisaverylongazureresourcenamewhichneedstobe7899", testResult, "The resource name is not the excepted one!");
        assertEquals(Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), testResult.length(), "The resource name length is wrong");

    }

    @Test
    public void validateStorageTypeForGroupWhenPremiumStorageConfiguredAndFlavorNotPremiumThenShouldThrowCloudConnectorException() {
        String flavor = "Standard_A10";
        AzureDiskType azureDiskType = AzureDiskType.PREMIUM_LOCALLY_REDUNDANT;

        when(azurePremiumValidatorService.premiumDiskTypeConfigured(azureDiskType)).thenReturn(true);
        when(azurePremiumValidatorService.validPremiumConfiguration(flavor)).thenReturn(false);

        assertThrows(
                CloudConnectorException.class,
                () -> underTest.validateStorageTypeForGroup(azureDiskType, flavor));

        verify(azurePremiumValidatorService, times(1)).premiumDiskTypeConfigured(azureDiskType);
        verify(azurePremiumValidatorService, times(1)).validPremiumConfiguration(flavor);
    }

    @Test
    public void validateStorageTypeForGroupWhenPremiumStorageNotConfiguredThenShouldNotCallInstanceValidation() {
        String flavor = "Standard_A10";
        AzureDiskType azureDiskType = AzureDiskType.GEO_REDUNDANT;

        when(azurePremiumValidatorService.premiumDiskTypeConfigured(azureDiskType)).thenReturn(false);

        underTest.validateStorageTypeForGroup(azureDiskType, flavor);

        verify(azurePremiumValidatorService, times(1)).premiumDiskTypeConfigured(azureDiskType);
        verify(azurePremiumValidatorService, times(0)).validPremiumConfiguration(flavor);
    }

    @Test
    public void validateStorageTypeForGroupWhenPremiumStorageConfiguredAndFlavorIsPremiumThenShouldEverythinGoesFine() {
        String flavor = "Standard_DS10";
        AzureDiskType azureDiskType = AzureDiskType.PREMIUM_LOCALLY_REDUNDANT;

        when(azurePremiumValidatorService.premiumDiskTypeConfigured(azureDiskType)).thenReturn(true);
        when(azurePremiumValidatorService.validPremiumConfiguration(flavor)).thenReturn(true);

        underTest.validateStorageTypeForGroup(azureDiskType, flavor);

        verify(azurePremiumValidatorService, times(1)).validPremiumConfiguration(flavor);
        verify(azurePremiumValidatorService, times(1)).premiumDiskTypeConfigured(azureDiskType);
    }

    @Test
    public void deallocateInstancesShouldSuccess() throws BatchInstanceActionFailedException {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsFromAzureAndFillStatusesWithoutRetry(any(), anyList(), anyList()))
                .thenAnswer((Answer<Map<String, VirtualMachine>>) invocation -> {
                    List<CloudVmInstanceStatus> statuses = invocation.getArgument(2, List.class);
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED));
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.STARTED));
                    return new HashMap<>();
                });
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), anyString())).thenReturn(Completable.complete());

        List<CloudVmInstanceStatus> statusesAfterDeallocate = underTest.deallocateInstances(ac,
                List.of(createCloudInstance("instance1"), createCloudInstance("instance2")));

        verify(azureClient, times(2)).deallocateVirtualMachineAsync(anyString(), anyString());
        assertEquals(2, statusesAfterDeallocate.size());
        statusesAfterDeallocate.forEach(status -> assertEquals(InstanceStatus.STOPPED, status.getStatus()));
    }

    @Test
    public void deallocateStoppedInstanceShouldSkippedFromDeallocation() throws BatchInstanceActionFailedException {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsFromAzureAndFillStatusesWithoutRetry(any(), anyList(), anyList()))
                .thenAnswer((Answer<Map<String, VirtualMachine>>) invocation -> {
                    List<CloudVmInstanceStatus> statuses = invocation.getArgument(2, List.class);
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED));
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.STOPPED));
                    return new HashMap<>();
                });
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), anyString())).thenReturn(Completable.complete());

        List<CloudVmInstanceStatus> statusesAfterDeallocate = underTest.deallocateInstances(ac,
                List.of(createCloudInstance("instance1"), createCloudInstance("instance2")));

        verify(azureClient, times(1)).deallocateVirtualMachineAsync(anyString(), anyString());
        assertEquals(2, statusesAfterDeallocate.size());
        statusesAfterDeallocate.forEach(status -> assertEquals(InstanceStatus.STOPPED, status.getStatus()));
    }

    @Test
    public void deallocateInstancesShouldHandleAzureErrorsAndThrowBatchInstanceActionFailedExceptionAfterAllRequestFinished()
            throws BatchInstanceActionFailedException {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsFromAzureAndFillStatusesWithoutRetry(any(), anyList(), anyList()))
                .thenAnswer((Answer<Map<String, VirtualMachine>>) invocation -> {
                    List<CloudVmInstanceStatus> statuses = invocation.getArgument(2, List.class);
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED));
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.IN_PROGRESS));
                    statuses.add(new CloudVmInstanceStatus(createCloudInstance("instance3"), InstanceStatus.DELETE_REQUESTED));
                    return new HashMap<>();
                });
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance1"))).thenReturn(Completable.complete());
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        BatchInstanceActionFailedException thrown = assertThrows(
                BatchInstanceActionFailedException.class,
                () -> underTest.deallocateInstances(ac,
                        List.of(createCloudInstance("instance1"), createCloudInstance("instance2"), createCloudInstance("instance3"))));

        List<CloudVmInstanceStatus> statusesAfterDeallocate = thrown.getInstanceStatuses();
        verify(azureClient, times(3)).deallocateVirtualMachineAsync(anyString(), anyString());
        assertEquals(3, statusesAfterDeallocate.size());

        for (CloudVmInstanceStatus cloudVmInstanceStatus : statusesAfterDeallocate) {
            String instanceId = cloudVmInstanceStatus.getCloudInstance().getInstanceId();
            if ("instance1".equals(instanceId)) {
                assertEquals(InstanceStatus.STOPPED, cloudVmInstanceStatus.getStatus());
            } else if ("instance2".equals(instanceId)) {
                assertEquals(InstanceStatus.FAILED, cloudVmInstanceStatus.getStatus());
            } else if ("instance3".equals(instanceId)) {
                assertEquals(InstanceStatus.FAILED, cloudVmInstanceStatus.getStatus());
            }
        }
    }

    @NotNull
    private CloudInstance createCloudInstance(String instanceId) {
        return new CloudInstance(instanceId, null, null);
    }
}
