package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.management.exception.AdditionalInfo;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.azure.resourcemanager.compute.models.PolicyViolation;
import com.azure.resourcemanager.compute.models.PolicyViolationCategory;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePremiumValidatorService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudExceptionConverter;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.Retry;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@ExtendWith(MockitoExtension.class)
public class AzureUtilsTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final int MAX_RESOURCE_NAME_LENGTH = 50;

    private static final int MAX_DISK_ENCRYPTION_SET_NAME_LENGTH = 80;

    @Mock
    private AzurePremiumValidatorService azurePremiumValidatorService;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private CloudExceptionConverter cloudExceptionConverter;

    @Mock
    private Retry retryService;

    @Mock
    private SchedulerProvider schedulerProvider;

    @InjectMocks
    private AzureUtils underTest;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", MAX_RESOURCE_NAME_LENGTH);
        lenient().when(schedulerProvider.io()).thenReturn(Schedulers.immediate());
    }

    @Test
    public void shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        CloudContext context = CloudContext.Builder.builder()
                .withId(7899L)
                .withName("thisisaverylongazureresourcenamewhichneedstobeshortened")
                .withCrn("crn")
                .withPlatform("dummy1")
                .withWorkspaceId(WORKSPACE_ID)
                .build();

        //WHEN
        String testResult = underTest.getStackName(context);

        //THEN
        assertNotNull(testResult, "The generated name must not be null!");
        assertEquals("thisisaverylongazureresourcenamewhichneedstobe7899", testResult, "The resource name is not the excepted one!");
        assertEquals(MAX_RESOURCE_NAME_LENGTH, testResult.length(), "The resource name length is wrong");

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
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), anyString(), any())).thenReturn(Mono.empty());

        List<CloudVmInstanceStatus> statusesAfterDeallocate = underTest.deallocateInstances(ac,
                List.of(createCloudInstance("instance1"), createCloudInstance("instance2")));

        verify(azureClient, times(2)).deallocateVirtualMachineAsync(anyString(), anyString(), any());
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
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), anyString(), any())).thenReturn(Mono.empty());

        List<CloudVmInstanceStatus> statusesAfterDeallocate = underTest.deallocateInstances(ac,
                List.of(createCloudInstance("instance1"), createCloudInstance("instance2")));

        verify(azureClient, times(1)).deallocateVirtualMachineAsync(anyString(), anyString(), any());
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
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance1"), any())).thenReturn(Mono.empty());
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance2"), any())).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deallocateVirtualMachineAsync(anyString(), eq("instance3"), any())).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deallocateInstances(ac,
                        List.of(createCloudInstance("instance1"), createCloudInstance("instance2"),
                                createCloudInstance("instance3"))));

        verify(azureClient, times(3)).deallocateVirtualMachineAsync(anyString(), anyString(), any());
    }

    @Test
    public void deleteInstancesShouldSucceed() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("instance1", "instance2"),
                        List.of(
                                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.STARTED))));
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), anyString())).thenReturn(Mono.empty());

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
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), anyString())).thenReturn(Mono.empty());

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
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance1"))).thenReturn(Mono.empty());
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance2"))).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance3"))).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteInstances(ac,
                        List.of(createCloudInstance("instance1"), createCloudInstance("instance2"),
                                createCloudInstance("instance3"))));

        verify(azureClient, times(3)).deleteVirtualMachine(anyString(), anyString());
    }

    @Test
    public void deleteInstancesByNameShouldSucceed() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), anyString())).thenReturn(Mono.empty());

        underTest.deleteInstancesByName(ac, "resourceGroup", List.of("instance1", "instance2"));

        verify(azureClient, times(2)).deleteVirtualMachine(anyString(), anyString());
    }

    @Test
    public void deleteInstancesByNameShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance1"))).thenReturn(Mono.empty());
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance2"))).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deleteVirtualMachine(anyString(), eq("instance3"))).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteInstancesByName(ac, "resourceGroup", List.of("instance1", "instance2", "instance3")));

        verify(azureClient, times(3)).deleteVirtualMachine(anyString(), anyString());
    }

    @Test
    public void deleteNetworkInterfacesShouldSucceed() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), anyString())).thenReturn(Mono.empty());

        underTest.deleteNetworkInterfaces(azureClient, "resourceGroup", List.of("network1", "network2"));

        verify(azureClient, times(2)).deleteNetworkInterfaceAsync(anyString(), anyString());
    }

    @Test
    public void deleteNetworkInterfacesShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), eq("network1"))).thenReturn(Mono.empty());
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), eq("network2"))).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deleteNetworkInterfaceAsync(anyString(), eq("network3"))).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteNetworkInterfaces(azureClient, "resourceGroup", List.of("network1", "network2", "network3")));

        verify(azureClient, times(3)).deleteNetworkInterfaceAsync(anyString(), anyString());
    }

    @Test
    public void deletePublicIpsShouldSucceed() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), anyString())).thenReturn(Mono.empty());

        underTest.deletePublicIps(azureClient, "resourceGroup", List.of("ip1", "ip2"));

        verify(azureClient, times(2)).deletePublicIpAddressByNameAsync(anyString(), anyString());
    }

    @Test
    public void deletePublicIpsShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), eq("ip1"))).thenReturn(Mono.empty());
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), eq("ip2"))).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deletePublicIpAddressByNameAsync(anyString(), eq("ip3"))).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deletePublicIps(azureClient, "resourceGroup", List.of("ip1", "ip2", "ip3")));

        verify(azureClient, times(3)).deletePublicIpAddressByNameAsync(anyString(), anyString());
    }

    @Test
    public void deleteLoadBalancersShouldSucceed() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteLoadBalancerAsync(anyString(), anyString())).thenReturn(Mono.empty());

        underTest.deleteLoadBalancers(azureClient, "resourceGroup", List.of("loadbalancer1", "loadbalancer2"));

        verify(azureClient, times(2)).deleteLoadBalancerAsync(anyString(), anyString());
    }

    @Test
    public void deleteLoadBalancersShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestsFinish() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteLoadBalancerAsync(anyString(), eq("loadbalancer1"))).thenReturn(Mono.empty());
        when(azureClient.deleteLoadBalancerAsync(anyString(), eq("loadbalancer2"))).thenReturn(Mono.error(new RuntimeException("failure message 1")));
        when(azureClient.deleteLoadBalancerAsync(anyString(), eq("loadbalancer3"))).thenReturn(Mono.error(new RuntimeException("failure message 2")));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.deleteLoadBalancers(azureClient, "resourceGroup", List.of("loadbalancer1", "loadbalancer2", "loadbalancer3")));

        verify(azureClient, times(3)).deleteLoadBalancerAsync(anyString(), anyString());
    }

    @Test
    public void deleteAvailabilitySetsShouldSucceed() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteAvailabilitySetAsync(anyString(), anyString())).thenReturn(Mono.empty());

        underTest.deleteAvailabilitySets(azureClient, "resourceGroup", List.of("availabilitySet1", "availabilitySet2"));

        verify(azureClient, times(2)).deleteAvailabilitySetAsync(anyString(), anyString());
    }

    @Test
    public void deleteAvailabilitySetsShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteAvailabilitySetAsync(anyString(), eq("availabilitySet1"))).thenReturn(Mono.empty());
        when(azureClient.deleteAvailabilitySetAsync(anyString(), eq("availabilitySet2"))).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deleteAvailabilitySetAsync(anyString(), eq("availabilitySet3"))).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteAvailabilitySets(azureClient, "resourceGroup",
                        List.of("availabilitySet1", "availabilitySet2", "availabilitySet3")));

        verify(azureClient, times(3)).deleteAvailabilitySetAsync(anyString(), anyString());
    }

    @Test
    public void deleteGenericResourcesShouldSucceed() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteGenericResourceByIdAsync(anyString())).thenReturn(Mono.empty());

        underTest.deleteGenericResources(azureClient, List.of("genericResource1", "genericResource2"));

        verify(azureClient, times(2)).deleteGenericResourceByIdAsync(anyString());
    }

    @Test
    public void deleteGenericResourcesShouldHandleAzureErrorsAndThrowCloudbreakServiceExceptionAfterAllRequestFinished() {
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClient.deleteGenericResourceByIdAsync(eq("genericResource1"))).thenReturn(Mono.empty());
        when(azureClient.deleteGenericResourceByIdAsync(eq("genericResource2"))).thenReturn(Mono.error(new RuntimeException("failed1")));
        when(azureClient.deleteGenericResourceByIdAsync(eq("genericResource3"))).thenReturn(Mono.error(new RuntimeException("failed2")));

        assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.deleteGenericResources(azureClient, List.of("genericResource1", "genericResource2", "genericResource3")));

        verify(azureClient, times(3)).deleteGenericResourceByIdAsync(anyString());
    }

    @Test
    public void shouldAdjustDesNameLengthIfItsTooLong() {
        String name = "aVeryVeryVeryLoooooooooooooooooooooooooooooooooooooongNaaaaaaaaaaaaaaaaaaaaame";
        String id = UUID.randomUUID().toString();
        String desName = underTest.generateDesNameByNameAndId(name, id);

        assertNotNull(desName, "The generated name must not be null!");
        assertNotEquals(
                desName, "The resource name is not the excepted one!", "aVeryVeryVeryLoooooooooooooooooooooooooooooooooooooongNaaaaaaaaaaaaaaaaaaaaame");
        assertEquals(MAX_DISK_ENCRYPTION_SET_NAME_LENGTH, desName.length(), "The resource name length is wrong");

    }

    @Test
    void convertToCloudConnectorExceptionTestWhenCloudExceptionAndDetails() {
        ApiError cloudError = AzureTestUtils.apiError("123", "foobar");
        List<ManagementError> details = new ArrayList<>();
        AzureTestUtils.setDetails(cloudError, details);
        details.add(AzureTestUtils.managementError("123", "detail1"));
        details.add(AzureTestUtils.managementError("123", "detail2"));
        ApiErrorException e = new ApiErrorException("Serious problem", null, cloudError);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result,
                "Checking resources failed, status code 123, error message: foobar, details: detail1, detail2");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenPermissionExceptionAndDetails() {
        String message = "The client does not have authorization to perform action 'Microsoft.Marketplace/offerTypes/publishers/offers/plans/agreements/read'";

        ManagementError managementError = AzureTestUtils.managementError("AuthorizationFailed", message);
        List<ManagementError> details = new ArrayList<>();
        details.add(AzureTestUtils.managementError("123", "detail1"));
        details.add(AzureTestUtils.managementError("AuthorizationFailed", message));
        AzureTestUtils.setDetails(managementError, details);
        ManagementException e = new ManagementException("Authorization failed", null, managementError);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Stack provision failed");

        verifyCloudConnectorException(result,
                "Stack provision failed failed, status code AuthorizationFailed, error message: The client does not have authorization to perform action " +
                        "'Microsoft.Marketplace/offerTypes/publishers/offers/plans/agreements/read', details: detail1, The client does not have authorization" +
                        " to perform action 'Microsoft.Marketplace/offerTypes/publishers/offers/plans/agreements/read'");
        assertEquals(CloudImageException.class, result.getClass());
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenTermsExceptionAndDetails() {
        String message = "Marketplace purchase eligibilty check returned errors. See inner errors for details.";

        ManagementError managementError = AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", message);
        List<ManagementError> details = new ArrayList<>();
        details.add(AzureTestUtils.managementError("123", "detail1"));
        details.add(AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", message));
        AzureTestUtils.setDetails(managementError, details);
        ManagementException e = new ManagementException("Terms failed", null, managementError);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Stack provision failed");

        verifyCloudConnectorException(result,
                "Stack provision failed failed, status code MarketplacePurchaseEligibilityFailed, error message: Marketplace purchase eligibilty check " +
                        "returned errors. See inner errors for details., details: detail1, Marketplace purchase eligibilty check returned errors. " +
                        "See inner errors for details.");
        assertEquals(CloudImageException.class, result.getClass());
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenNonMatchingExceptionAndDetails() {
        String message = "Marketplace purchase eligibilty check returned errors. See inner errors for details.";

        ManagementError managementError = AzureTestUtils.managementError("foo", message);
        List<ManagementError> details = new ArrayList<>();
        details.add(AzureTestUtils.managementError("123", "detail1"));
        details.add(AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", message));
        AzureTestUtils.setDetails(managementError, details);
        ManagementException e = new ManagementException("Terms failed", null, managementError);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Stack provision failed");

        verifyCloudConnectorException(result,
                "Stack provision failed failed, status code foo, error message: Marketplace purchase eligibilty check returned errors. " +
                        "See inner errors for details., details: detail1, Marketplace purchase eligibilty check returned errors. " +
                        "See inner errors for details.");
        assertNotEquals(CloudImageException.class, result.getClass());
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenExceptionHasNoDetails() {
        String message = "Marketplace purchase eligibilty check returned errors. See inner errors for details.";
        ManagementError managementError = AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", message);
        ManagementException e = new ManagementException("Terms failed", null, managementError);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result,
                "Checking resources failed: 'com.azure.core.management.exception.ManagementException: Terms failed: Marketplace purchase eligibilty" +
                        " check returned errors. See inner errors for details.', please go to Azure Portal for detailed message");
        assertEquals(CloudImageException.class, result.getClass());
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenDetailCloudErrorsHaveRequestDisallowedByPolicyCode() {
        ApiError cloudError = AzureTestUtils.apiError("InvalidTemplateDeployment",
                "The template deployment failed with multiple errors. Please see details for more information.");
        List<ManagementError> details = new ArrayList<>();
        AzureTestUtils.setDetails(cloudError, details);

        PolicyViolation policyViolation = new PolicyViolation();
        AzureTestUtils.setField(policyViolation, "category", PolicyViolationCategory.OTHER);
        AzureTestUtils.setField(policyViolation, "details", "dbajzath-azure-restricted-policy");

        ManagementError detail1 = AzureTestUtils.managementError("RequestDisallowedByPolicy",
                "Resource 'cbimgwu29d62091481040a03' was disallowed by policy. Reasons: 'West US 2 location is disabled'. " +
                        "See error details for policy resource IDs.");

        AzureTestUtils.setField(detail1, "additionalInfo", List.of(new AdditionalInfo("PolicyViolation", policyViolation)));
        details.add(detail1);

        ManagementError detail2 = AzureTestUtils.managementError("RequestDisallowedByPolicy",
                "Resource 'cbimgwu29d62091481040a03/default' was disallowed by policy. Reasons: 'West US 2 location is disabled'. " +
                        "See error details for policy resource IDs.");
        AzureTestUtils.setField(detail2, "additionalInfo", List.of(new AdditionalInfo("PolicyViolation", policyViolation)));
        details.add(detail2);

        ApiErrorException apiErrorException =
                new ApiErrorException("The template deployment failed with multiple errors. Please see details for more information.", null, cloudError);

        CloudConnectorException result = underTest.convertToCloudConnectorException(apiErrorException, "Storage account creation");

        verifyCloudConnectorException(result,
                "Storage account creation failed, status code InvalidTemplateDeployment, error message: The template deployment failed with multiple errors. " +
                        "Please see details for more information., details: Resource 'cbimgwu29d62091481040a03' was disallowed by policy. " +
                        "Reasons: 'West US 2 location is disabled'. Policy definition: Other - dbajzath-azure-restricted-policy, " +
                        "Resource 'cbimgwu29d62091481040a03/default' was disallowed by policy. Reasons: 'West US 2 location is disabled'. " +
                        "Policy definition: Other - dbajzath-azure-restricted-policy");
    }

    private void verifyCloudConnectorException(CloudConnectorException cloudConnectorException, String messageExpected) {
        assertThat(cloudConnectorException).isNotNull();
        assertThat(cloudConnectorException).hasMessage(messageExpected);
        assertThat(cloudConnectorException).hasNoCause();
    }

    private static ApiErrorException createCloudExceptionWithNoDetails(boolean withBody) {
        ApiErrorException e;
        if (withBody) {
            ApiError cloudError = new ApiError();
            AzureTestUtils.setDetails(cloudError, null);
            e = new ApiErrorException("Serious problem", null, cloudError);
        } else {
            e = new ApiErrorException("Serious problem", null);
        }
        return e;
    }

    static Object[][] convertToCloudConnectorExceptionTestWhenCloudExceptionAndNoDetailsDataProvider() {
        return new Object[][] {
                // testCaseName e
                {"CloudException without body", createCloudExceptionWithNoDetails(false)},
                {"CloudException with body but null details", createCloudExceptionWithNoDetails(true)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("convertToCloudConnectorExceptionTestWhenCloudExceptionAndNoDetailsDataProvider")
    void convertToCloudConnectorExceptionTestWhenCloudExceptionAndNoDetails(String testCaseName, ApiErrorException e) {
        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result,
                "Checking resources failed: 'com.azure.resourcemanager.compute.models.ApiErrorException: Serious problem', " +
                        "please go to Azure Portal for detailed message");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenThrowableAndCloudException() {
        Throwable e = createCloudExceptionWithNoDetails(false);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result,
                "Checking resources failed: 'com.azure.resourcemanager.compute.models.ApiErrorException: Serious problem', " +
                        "please go to Azure Portal for detailed message");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenThrowableAndNotCloudException() {
        Throwable e = new UnsupportedOperationException("Serious problem");
        CloudConnectorException cloudConnectorException = new CloudConnectorException("foobar");
        when(cloudExceptionConverter.convertToCloudConnectorException(e, "Checking resources")).thenReturn(cloudConnectorException);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        assertThat(result).isSameAs(cloudConnectorException);
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenNonMatchingExceptionAndDetailsOfDetails() {
        ManagementError parent = AzureTestUtils.managementError("parent", "Marketplace purchase eligibilty check returned errors");
        ManagementError childDetail = AzureTestUtils.managementError("child", "childdetail");
        ManagementError grandChildDetail = AzureTestUtils.managementError("grandchild", "grandchilddetail");
        AzureTestUtils.setDetails(parent, List.of(childDetail));
        AzureTestUtils.setDetails(childDetail, List.of(grandChildDetail));
        ManagementException e = new ManagementException("Terms failed", null, parent);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Stack provision failed");

        verifyCloudConnectorException(result,
                "Stack provision failed failed, status code parent, error message: Marketplace purchase eligibilty check returned errors," +
                        " details: childdetail (details: grandchilddetail)");
        assertNotEquals(CloudImageException.class, result.getClass());
    }

    @Test
    void convertToActionFailedExceptionCausedByCloudConnectorExceptionTest() {
        Throwable e = new UnsupportedOperationException("Serious problem");
        CloudConnectorException cloudConnectorException = new CloudConnectorException("foobar");
        when(cloudExceptionConverter.convertToCloudConnectorException(e, "Checking resources")).thenReturn(cloudConnectorException);

        Retry.ActionFailedException result = underTest.convertToActionFailedExceptionCausedByCloudConnectorException(e, "Checking resources");

        assertThat(result).isNotNull();
        assertThat(result.getCause()).isSameAs(cloudConnectorException);
    }

    @NotNull
    private CloudInstance createCloudInstance(String instanceId) {
        return new CloudInstance(instanceId, null, null, "subnet-1", "az1");
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

    /**
     * Expected outputs are copied from the <a href="https://md5calc.com/hash/adler32">online calculator</a> referenced in
     * <a href="https://docs.cloudera.com/cdp/latest/requirements-azure/topics/mc-azure-adls-images.html">the docs</a>.
     * Leading zeroes are trimmed to match the actual output.
     */
    @Test
    void encodeString() {
        Map<String, String> inputAndOutput = Map.of(
                "062d53231d9c48449a2540abe7df2f61", "8301085c",
                "cdppoc", "089d027a",
                "a", "00620062"
        );
        SoftAssertions softly = new SoftAssertions();
        inputAndOutput.forEach((input, output) ->
                softly.assertThat(underTest.encodeString(input))
                        .as(input)
                        .isEqualTo(output.replaceAll("^0+", "")));
        softly.assertAll();
    }

    @Test
    void testCheckResourceGroupExistenceWithRetryNoException() {
        AzureClient azureClient = mock(AzureClient.class);
        when(retryService.testWith2SecDelayMax5Times(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        when(azureClient.resourceGroupExists("rg-name")).thenReturn(true);

        boolean result = underTest.checkResourceGroupExistenceWithRetry(azureClient, "rg-name");

        assertTrue(result);
        verify(retryService, times(1)).testWith2SecDelayMax5Times(any(Supplier.class));
    }

    @Test
    void testCheckResourceGroupExistenceWithRetryExhausted() {
        AzureClient azureClient = mock(AzureClient.class);
        when(retryService.testWith2SecDelayMax5Times(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        when(azureClient.resourceGroupExists("rg-name")).thenThrow(new RuntimeException("error"));

        Retry.ActionFailedException ex = assertThrows(Retry.ActionFailedException.class,
                () -> underTest.checkResourceGroupExistenceWithRetry(azureClient, "rg-name"));

        assertEquals(RuntimeException.class, ex.getCause().getClass());
        assertEquals("java.lang.RuntimeException: error", ex.getMessage());
        verify(retryService, times(1)).testWith2SecDelayMax5Times(any(Supplier.class));
    }

    @ParameterizedTest
    @MethodSource("getCustomEndpointGatewaySubnetIdsScenarios")
    void getCustomEndpointGatewaySubnetIds(String subnets, List<String> expectedItems) {
        Network network = mock(Network.class);
        when(network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID)).thenReturn(subnets);
        List<String> result = underTest.getCustomEndpointGatewaySubnetIds(network);
        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedItems);
    }

    static Stream<Arguments> getCustomEndpointGatewaySubnetIdsScenarios() {
        return Stream.of(
                Arguments.of(null, List.of()),
                Arguments.of("", List.of()),
                Arguments.of("  ", List.of()),
                Arguments.of("subnet1", List.of("subnet1")),
                Arguments.of("subnet2,subnet1", List.of("subnet1", "subnet2"))
        );
    }
}
