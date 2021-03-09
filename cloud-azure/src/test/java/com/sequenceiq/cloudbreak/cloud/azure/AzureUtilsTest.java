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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

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
    public void deallocateInstancesShouldSucceed() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzureWithoutRetry(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(new HashMap<>(),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.STARTED))));
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
    public void deallocateStoppedInstanceShouldBeSkippedFromDeallocation() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzureWithoutRetry(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(new HashMap<>(),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.STOPPED))));
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
    public void deallocateInstancesShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzureWithoutRetry(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(new HashMap<>(),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.IN_PROGRESS),
                                new CloudVmInstanceStatus(createCloudInstance("instance3"), InstanceStatus.DELETE_REQUESTED))));
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance1"))).thenReturn(Completable.complete());
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deallocateInstances(ac,
                        List.of(createCloudInstance("instance1"), createCloudInstance("instance2"),
                                createCloudInstance("instance3"))));

        verify(azureClient, times(3)).deallocateVirtualMachineAsync(anyString(), anyString());
    }

    @Test
    public void deleteInstancesShouldSucceed() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("instance1", "instance2"),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.STARTED))));
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), anyString())).thenReturn(Completable.complete());

        List<CloudVmInstanceStatus> statusesAfterDelete = underTest.deleteInstances(ac,
                List.of(createCloudInstance("instance1"), createCloudInstance("instance2")));

        verify(azureClient, times(2)).deleteVirtualMachine(anyString(), anyString());
        assertEquals(2, statusesAfterDelete.size());
        statusesAfterDelete.forEach(status -> assertEquals(InstanceStatus.TERMINATED, status.getStatus()));
    }

    @Test
    public void deleteTerminatedInstancesShouldBeSkippedFromDeletion() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("instance1", "instance2", "instance3"),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.TERMINATED),
                                new CloudVmInstanceStatus(createCloudInstance("instance3"), InstanceStatus.TERMINATED_BY_PROVIDER))));
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), anyString())).thenReturn(Completable.complete());

        List<CloudVmInstanceStatus> statusesAfterDelete = underTest.deleteInstances(ac,
                List.of(createCloudInstance("instance1"), createCloudInstance("instance2"), createCloudInstance("instance4")));

        verify(azureClient, times(1)).deleteVirtualMachine(anyString(), anyString());
        assertEquals(3, statusesAfterDelete.size());

        statusesAfterDelete.forEach(status -> Assertions.assertThat(status.getStatus()).isIn(InstanceStatus.TERMINATED, InstanceStatus.TERMINATED_BY_PROVIDER));
    }

    @Test
    public void deleteInstancesShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("instance1", "instance2", "instance3"),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.IN_PROGRESS),
                                new CloudVmInstanceStatus(createCloudInstance("instance3"), InstanceStatus.DELETE_REQUESTED))));
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance1"))).thenReturn(Completable.complete());
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteInstances(ac,
                        List.of(createCloudInstance("instance1"), createCloudInstance("instance2"),
                                createCloudInstance("instance3"))));

        verify(azureClient, times(3)).deleteVirtualMachine(anyString(), anyString());
    }

    @Test
    public void deleteInstancesByNameShouldSucceed() {
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), anyString())).thenReturn(Completable.complete());

        underTest.deleteInstancesByName(ac, "resourceGroup", List.of("instance1", "instance2"));

        verify(azureClient, times(2)).deleteVirtualMachine(anyString(), anyString());
    }

    @Test
    public void deleteInstancesByNameShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AuthenticatedContext ac = Mockito.mock(AuthenticatedContext.class);
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance1"))).thenReturn(Completable.complete());
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteInstancesByName(ac, "resourceGroup", List.of("instance1", "instance2", "instance3")));

        verify(azureClient, times(3)).deleteVirtualMachine(anyString(), anyString());
    }

    @Test
    public void deleteNetworkInterfacesShouldSucceed() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), anyString())).thenReturn(Completable.complete());

        underTest.deleteNetworkInterfaces(azureClient, "resourceGroup", List.of("network1", "network2"));

        verify(azureClient, times(2)).deleteNetworkInterfaceAsync(anyString(), anyString());
    }

    @Test
    public void deleteNetworkInterfacesShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), eq("network1"))).thenReturn(Completable.complete());
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), eq("network2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), eq("network3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteNetworkInterfaces(azureClient, "resourceGroup", List.of("network1", "network2", "network3")));

        verify(azureClient, times(3)).deleteNetworkInterfaceAsync(anyString(), anyString());
    }

    @Test
    public void deletePublicIpsShouldSucceed() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), anyString())).thenReturn(Completable.complete());

        underTest.deletePublicIps(azureClient, "resourceGroup", List.of("ip1", "ip2"));

        verify(azureClient, times(2)).deletePublicIpAddressByNameAsync(anyString(), anyString());
    }

    @Test
    public void deletePublicIpsShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), eq("ip1"))).thenReturn(Completable.complete());
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), eq("ip2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), eq("ip3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deletePublicIps(azureClient, "resourceGroup", List.of("ip1", "ip2", "ip3")));

        verify(azureClient, times(3)).deletePublicIpAddressByNameAsync(anyString(), anyString());
    }

    @Test
    public void deleteAvailabilitySetsShouldSucceed() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deleteAvailabilitySetAsync(anyString(), anyString())).thenReturn(Completable.complete());

        underTest.deleteAvailabilitySets(azureClient, "resourceGroup", List.of("availabilitySet1", "availabilitySet2"));

        verify(azureClient, times(2)).deleteAvailabilitySetAsync(anyString(), anyString());
    }

    @Test
    public void deleteAvailabilitySetsShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deleteAvailabilitySetAsync(anyString(), eq("availabilitySet1"))).thenReturn(Completable.complete());
        when(azureClient.deleteAvailabilitySetAsync(anyString(), eq("availabilitySet2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deleteAvailabilitySetAsync(anyString(), eq("availabilitySet3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteAvailabilitySets(azureClient, "resourceGroup",
                        List.of("availabilitySet1", "availabilitySet2", "availabilitySet3")));

        verify(azureClient, times(3)).deleteAvailabilitySetAsync(anyString(), anyString());
    }

    @Test
    public void deleteGenericResourcesShouldSucceed() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deleteGenericResourceByIdAsync(anyString())).thenReturn(Completable.complete());

        underTest.deleteGenericResources(azureClient, List.of("genericResource1", "genericResource2"));

        verify(azureClient, times(2)).deleteGenericResourceByIdAsync(anyString());
    }

    @Test
    public void deleteGenericResourcesShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = Mockito.mock(AzureClient.class);
        when(azureClient.deleteGenericResourceByIdAsync(eq("genericResource1"))).thenReturn(Completable.complete());
        when(azureClient.deleteGenericResourceByIdAsync(eq("genericResource2"))).thenReturn(Completable.error(new RuntimeException("failed1")));
        when(azureClient.deleteGenericResourceByIdAsync(eq("genericResource3"))).thenReturn(Completable.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteGenericResources(azureClient, List.of("genericResource1", "genericResource2", "genericResource3")));

        verify(azureClient, times(3)).deleteGenericResourceByIdAsync(anyString());
    }

    @NotNull
    private CloudInstance createCloudInstance(String instanceId) {
        return new CloudInstance(instanceId, null, null);
    }

    private Map<String, VirtualMachine> createVirtualMachineMap(String... instanceIds) {
        Map<String, VirtualMachine> virtualMachineMap = new HashMap<>();
        if (instanceIds != null) {
            for (String instanceId : instanceIds) {
                virtualMachineMap.put(instanceId, null);
            }
        }
        return virtualMachineMap;
    }
}
