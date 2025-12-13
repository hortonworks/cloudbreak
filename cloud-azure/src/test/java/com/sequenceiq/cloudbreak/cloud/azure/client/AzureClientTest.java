package com.sequenceiq.cloudbreak.cloud.azure.client;

import static com.azure.resourcemanager.compute.models.NetworkAccessPolicy.DENY_ALL;
import static com.azure.resourcemanager.compute.models.PublicNetworkAccess.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.retry.RetryException;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.Region;
import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.fluent.ComputeManagementClient;
import com.azure.resourcemanager.compute.fluent.DisksClient;
import com.azure.resourcemanager.compute.fluent.ResourceSkusClient;
import com.azure.resourcemanager.compute.fluent.models.DiskInner;
import com.azure.resourcemanager.compute.fluent.models.ResourceSkuInner;
import com.azure.resourcemanager.compute.fluent.models.VirtualMachineInner;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.azure.resourcemanager.compute.models.CachingTypes;
import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.Blank;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithAvailabilityZone;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithCreate;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithDataDiskSource;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithDiskEncryption;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithDiskSource;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithGroup;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithHibernationSupport;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithLogicalSectorSize;
import com.azure.resourcemanager.compute.models.Disk.DefinitionStages.WithSku;
import com.azure.resourcemanager.compute.models.DiskSkuTypes;
import com.azure.resourcemanager.compute.models.DiskStorageAccountTypes;
import com.azure.resourcemanager.compute.models.DiskUpdate;
import com.azure.resourcemanager.compute.models.Disks;
import com.azure.resourcemanager.compute.models.Encryption;
import com.azure.resourcemanager.compute.models.NetworkAccessPolicy;
import com.azure.resourcemanager.compute.models.PublicNetworkAccess;
import com.azure.resourcemanager.compute.models.ResourceSkuLocationInfo;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachines;
import com.azure.resourcemanager.keyvault.models.AccessPolicy;
import com.azure.resourcemanager.keyvault.models.AccessPolicyEntry;
import com.azure.resourcemanager.keyvault.models.KeyPermissions;
import com.azure.resourcemanager.keyvault.models.Permissions;
import com.azure.resourcemanager.network.fluent.models.FrontendIpConfigurationInner;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerFrontend;
import com.azure.resourcemanager.network.models.LoadBalancers;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluent.DeploymentsClient;
import com.azure.resourcemanager.resources.fluent.ResourceManagementClient;
import com.azure.resourcemanager.resources.fluent.models.WhatIfOperationResultInner;
import com.azure.resourcemanager.resources.fluentcore.arm.AvailabilityZoneId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.azure.resourcemanager.resources.fluentcore.model.Creatable;
import com.azure.resourcemanager.resources.fluentcore.model.HasInnerModel;
import com.azure.resourcemanager.resources.fluentcore.model.implementation.IndexableRefreshableWrapperImpl;
import com.azure.resourcemanager.resources.implementation.GenericResourcesImpl;
import com.azure.resourcemanager.storage.models.Kind;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDisk;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureLoadBalancerFrontend;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
class AzureClientTest {

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private static final String RESOURCE_GROUP_NAME = "rg";

    private static final String SERVER_NAME = "serverName";

    private static final String PRIVATE_LB_NAME = "privateLb";

    private static final String PRIVATE_IP_ADDRESS1 = "10.11.12.13";

    private static final String FRONTEND_1_NAME = "frontend1";

    private static final String FRONTEND_2_NAME = "frontend2-gateway";

    private static final String GATEWAY_PRIVATE_IP_ADDRESS1 = "110.111.112.113";

    private static final String TAG_NAME = "created-for";

    @Mock
    private AzureClientFactory azureClientCredentials;

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private AzureResourceManager azureResourceManager;

    @Mock
    private LoadBalancers loadBalancerResource;

    @Mock
    private LoadBalancer privateLoadBalancer;

    @Mock
    private PostgreSqlManager postgreSqlManager;

    @Mock(extraInterfaces = {HasInnerModel.class, Creatable.class,
            Resource.class,
            WithSku.class,
            WithAvailabilityZone.class,
            WithDiskEncryption.class,
            WithHibernationSupport.class,
            WithLogicalSectorSize.class})
    private WithCreate withCreate;

    @Mock
    private com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager postgreSqlFlexibleManager;

    private AzureClient underTest;

    @Mock
    private AzureListResultFactory azureListResultFactory;

    @Mock
    private DiskInner diskInner;

    @Captor
    private ArgumentCaptor<Encryption> encryptionCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(azureClientCredentials.getAzureResourceManager()).thenReturn(azureResourceManager);
        lenient().when(azureClientCredentials.getPostgreSqlManager()).thenReturn(postgreSqlManager);
        lenient().when(azureExceptionHandler.handleException(any(Supplier.class))).thenCallRealMethod();
        lenient().when(azureExceptionHandler.handleException(any(Supplier.class), eq(Collections.emptySet()))).thenCallRealMethod();
        lenient().when(azureExceptionHandler.handleException(any(Supplier.class), any(), any())).thenCallRealMethod();
        lenient().doCallRealMethod().when(azureExceptionHandler).handleException(any(Runnable.class));

        underTest = new AzureClient(azureClientCredentials, azureExceptionHandler, azureListResultFactory);
    }

    static Object[][] setupDiskEncryptionWithDesIfNeededTestWhenDesAbsentDataProvider() {
        return new Object[][]{
                // testCaseName diskEncryptionSetId
                {"diskEncryptionSetId=null", null},
                {"diskEncryptionSetId=\"\"", ""},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("setupDiskEncryptionWithDesIfNeededTestWhenDesAbsentDataProvider")
    void setupDiskEncryptionWithDesIfNeededTestWhenDesAbsent(String testCaseName, String diskEncryptionSetId) {
        underTest.setupDiskEncryptionWithDesIfNeeded(diskEncryptionSetId, diskInner);

        verify(diskInner, never()).withEncryption(any());
    }

    @Test
    void setupDiskEncryptionWithDesIfNeededTestWhenDesGiven() {
        underTest.setupDiskEncryptionWithDesIfNeeded(DISK_ENCRYPTION_SET_ID, diskInner);

        verify(diskInner).withEncryption(encryptionCaptor.capture());
        Encryption encryption = encryptionCaptor.getValue();
        assertThat(encryption).isNotNull();
        assertThat(encryption.diskEncryptionSetId()).isEqualTo(DISK_ENCRYPTION_SET_ID);
    }

    @Test
    void setupDiskEncryptionWithDesIfNeededTestWhenAzureSdkInternalTypeChecks() throws ClassNotFoundException {
        // This class is package private
        Class<?> clazzDisksImpl = Class.forName("com.azure.resourcemanager.compute.implementation.DisksImpl");
        assertThat(Disks.class.isAssignableFrom(clazzDisksImpl)).isTrue();

        // This class is package private
        Class<?> clazzDiskImpl = Class.forName("com.azure.resourcemanager.compute.implementation.DiskImpl");
        assertThat(Disk.class.isAssignableFrom(clazzDiskImpl)).isTrue();
        assertThat(WithCreate.class.isAssignableFrom(clazzDiskImpl)).isTrue();
        assertThat(IndexableRefreshableWrapperImpl.class.isAssignableFrom(clazzDiskImpl)).isTrue();
        Constructor<?>[] clazzDiskImplConstructors = clazzDiskImpl.getDeclaredConstructors();
        assertThat(clazzDiskImplConstructors).isNotEmpty();
        Optional<Constructor<?>> clazzDiskImplConstructorOptional = Arrays.stream(clazzDiskImplConstructors)
                .filter(c -> Arrays.asList(c.getParameterTypes()).contains(DiskInner.class))
                .findFirst();
        assertThat(clazzDiskImplConstructorOptional).isPresent();
    }

    @Test
    public void testAttachDiskToVmWhenMaxSizeHigherShouldUseCachingNONE() {
        int sizeInGb = 4096;

        Disk disk = mock(Disk.class);
        List<Disk> disks = List.of(disk);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);

        when(virtualMachine.innerModel()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.apply()).thenReturn(virtualMachine);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        ArgumentCaptor<CachingTypes> captor = ArgumentCaptor.forClass(CachingTypes.class);

        underTest.attachDisksToVm(disks, virtualMachine);
        verify(virtualMachineUpdate, times(1)).withDataDiskDefaultCachingType(captor.capture());
        assertEquals(CachingTypes.NONE, captor.getValue());
    }

    @Test
    public void testAttachDiskToVmWhenEverythingIsNormalShouldUseCachingREAD() {
        int sizeInGb = 4095;
        DiskSkuTypes diskSkuTypes = DiskSkuTypes.PREMIUM_LRS;

        Disk disk = mock(Disk.class);
        List<Disk> disks = List.of(disk);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);

        when(virtualMachine.innerModel()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.apply()).thenReturn(virtualMachine);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        when(disk.sku()).thenReturn(diskSkuTypes);
        ArgumentCaptor<CachingTypes> captor = ArgumentCaptor.forClass(CachingTypes.class);

        underTest.attachDisksToVm(disks, virtualMachine);
        verify(virtualMachineUpdate, times(1)).withDataDiskDefaultCachingType(captor.capture());
        assertEquals(CachingTypes.READ_ONLY, captor.getValue());
    }

    @Test
    public void testAttachDiskToVmWhenEDiskTypeUltraSSDShouldUseCachingREADONLY() {
        int sizeInGb = 4095;
        DiskSkuTypes diskSkuTypes = DiskSkuTypes.ULTRA_SSD_LRS;

        Disk disk = mock(Disk.class);
        List<Disk> disks = List.of(disk);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);

        when(virtualMachine.innerModel()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.apply()).thenReturn(virtualMachine);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        when(disk.sku()).thenReturn(diskSkuTypes);
        ArgumentCaptor<CachingTypes> captor = ArgumentCaptor.forClass(CachingTypes.class);

        underTest.attachDisksToVm(disks, virtualMachine);
        verify(virtualMachineUpdate, times(1)).withDataDiskDefaultCachingType(captor.capture());
        assertEquals(CachingTypes.READ_ONLY, captor.getValue());
    }

    @Test
    public void testAttachDisksToVmIfAlreadyAttachedIssueHappened() {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachine.Update virtualMachineUpdate = getVirtualMachineUpdateForAttachDiskErrorTest(virtualMachine);
        when(azureExceptionHandler.isDiskAlreadyAttached(any())).thenReturn(true);
        when(virtualMachineUpdate.apply()).thenThrow(new ApiErrorException("", null, null));

        underTest.attachDisksToVm(getDisksForAttachDiskFailureTest(), virtualMachine);
        verify(virtualMachineUpdate).apply();
    }

    @Test
    public void testAttachDisksToVmIfConcurrentWriteIssueHappened() {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachine.Update virtualMachineUpdate = getVirtualMachineUpdateForAttachDiskErrorTest(virtualMachine);
        when(azureExceptionHandler.isDiskAlreadyAttached(any())).thenReturn(false);
        when(azureExceptionHandler.isConcurrentWrite(any())).thenReturn(true);
        when(virtualMachineUpdate.apply()).thenThrow(new ApiErrorException("", null, null));

        assertThrows(RetryException.class, () -> underTest.attachDisksToVm(getDisksForAttachDiskFailureTest(), virtualMachine));
        verify(virtualMachineUpdate).apply();
    }

    @Test
    public void testAttachDisksToVmIfOtherHttpStatusCodeErrorHappened() {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachine.Update virtualMachineUpdate = getVirtualMachineUpdateForAttachDiskErrorTest(virtualMachine);
        when(azureExceptionHandler.isConcurrentWrite(any())).thenReturn(false);
        when(azureExceptionHandler.isDiskAlreadyAttached(any())).thenReturn(false);
        when(virtualMachineUpdate.apply()).thenThrow(new ApiErrorException("", null, null));

        assertThrows(ApiErrorException.class, () -> underTest.attachDisksToVm(getDisksForAttachDiskFailureTest(), virtualMachine));
        verify(virtualMachineUpdate).apply();
    }

    private VirtualMachine.Update getVirtualMachineUpdateForAttachDiskErrorTest(VirtualMachine virtualMachine) {
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        when(virtualMachine.innerModel()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        return virtualMachineUpdate;
    }

    private List<Disk> getDisksForAttachDiskFailureTest() {
        int sizeInGb = 4095;
        DiskSkuTypes diskSkuTypes = DiskSkuTypes.ULTRA_SSD_LRS;
        Disk disk = mock(Disk.class);
        List<Disk> disks = List.of(disk);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        when(disk.sku()).thenReturn(diskSkuTypes);
        return disks;
    }

    @Test
    public void testKeyVaultAccessPolicyListForServicePrincipal() {
        List<AccessPolicy> accessPolicies = new ArrayList<AccessPolicy>();
        AccessPolicy accessPolicy1 = mock(AccessPolicy.class);
        AccessPolicy accessPolicy2 = mock(AccessPolicy.class);
        AccessPolicy accessPolicy3 = mock(AccessPolicy.class);
        AccessPolicy accessPolicy4 = mock(AccessPolicy.class);
        accessPolicies.add(accessPolicy1);
        accessPolicies.add(accessPolicy2);
        accessPolicies.add(accessPolicy3);
        accessPolicies.add(accessPolicy4);

        when(accessPolicy1.innerModel()).thenReturn(new AccessPolicyEntry()
                .withObjectId("100")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY, KeyPermissions.GET))));

        when(accessPolicy2.innerModel()).thenReturn(new AccessPolicyEntry()
                .withObjectId("200")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.UNWRAP_KEY, KeyPermissions.GET))));

        when(accessPolicy3.innerModel()).thenReturn(new AccessPolicyEntry()
                .withObjectId("300")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.GET))));

        when(accessPolicy4.innerModel()).thenReturn(new AccessPolicyEntry()
                .withObjectId("400")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY))));

        assertTrue(underTest.isValidKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "100"));
        assertFalse(underTest.isValidKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "200"));
        assertFalse(underTest.isValidKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "300"));
        assertFalse(underTest.isValidKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "400"));
        assertFalse(underTest.isValidKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "dummy"));
    }

    @Test
    void getVaultNameFromEncryptionKeyUrlTestPass() {
        String vaultName = underTest.getVaultNameFromEncryptionKeyUrl("https://dummyvaultName.vault.azure.net/keys/dummykeyName/dummykeyVersion");
        assertThat(vaultName).isEqualTo("dummyvaultName");
    }

    @Test
    void getVaultNameFromEncryptionKeyUrlTestFails() {
        String vaultName = underTest.getVaultNameFromEncryptionKeyUrl("wrongKeyUrl");
        assertThat(vaultName).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = LoadBalancerType.class, names = {"PRIVATE", "GATEWAY_PRIVATE"})
    void getPrivateLoadBalancerWith1Frontend(LoadBalancerType loadBalancerType) {
        when(azureResourceManager.loadBalancers()).thenReturn(loadBalancerResource);
        when(loadBalancerResource.getByResourceGroup(RESOURCE_GROUP_NAME, PRIVATE_LB_NAME)).thenReturn(privateLoadBalancer);
        LoadBalancerFrontend frontend1 = mock(LoadBalancerFrontend.class);
        FrontendIpConfigurationInner frontend1Inner = new FrontendIpConfigurationInner().withPrivateIpAddress(PRIVATE_IP_ADDRESS1);
        when(frontend1.innerModel()).thenReturn(frontend1Inner);
        Map<String, LoadBalancerFrontend> frontends = Map.of(FRONTEND_1_NAME, frontend1);
        when(privateLoadBalancer.frontends()).thenReturn(frontends);
        List<AzureLoadBalancerFrontend> result =
                underTest.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, PRIVATE_LB_NAME, loadBalancerType);

        assertThat(result).matches(r -> r.size() == 1)
                .anyMatch(fe -> LoadBalancerType.PRIVATE == fe.getLoadBalancerType()
                        && PRIVATE_IP_ADDRESS1.equals(fe.getIp()) && FRONTEND_1_NAME.equals(fe.getName()));
    }

    @ParameterizedTest
    @EnumSource(value = LoadBalancerType.class, names = {"PRIVATE", "GATEWAY_PRIVATE"})
    void getPrivateLoadBalancerWith2Frontends(LoadBalancerType loadBalancerType) {
        when(azureResourceManager.loadBalancers()).thenReturn(loadBalancerResource);
        when(loadBalancerResource.getByResourceGroup(RESOURCE_GROUP_NAME, PRIVATE_LB_NAME)).thenReturn(privateLoadBalancer);
        LoadBalancerFrontend frontend1 = mock(LoadBalancerFrontend.class);
        LoadBalancerFrontend frontend2 = mock(LoadBalancerFrontend.class);
        FrontendIpConfigurationInner frontend1Inner = new FrontendIpConfigurationInner().withPrivateIpAddress(PRIVATE_IP_ADDRESS1);
        FrontendIpConfigurationInner frontend2Inner = new FrontendIpConfigurationInner().withPrivateIpAddress(GATEWAY_PRIVATE_IP_ADDRESS1);
        when(frontend1.innerModel()).thenReturn(frontend1Inner);
        when(frontend2.innerModel()).thenReturn(frontend2Inner);
        Map<String, LoadBalancerFrontend> frontends = Map.of(FRONTEND_1_NAME, frontend1, FRONTEND_2_NAME, frontend2);
        when(privateLoadBalancer.frontends()).thenReturn(frontends);
        List<AzureLoadBalancerFrontend> result =
                underTest.getLoadBalancerFrontends(RESOURCE_GROUP_NAME, PRIVATE_LB_NAME, loadBalancerType);

        assertThat(result).matches(r -> r.size() == 2)
                .anyMatch(fe -> LoadBalancerType.PRIVATE == fe.getLoadBalancerType()
                        && PRIVATE_IP_ADDRESS1.equals(fe.getIp()) && FRONTEND_1_NAME.equals(fe.getName()))
                .anyMatch(fe -> LoadBalancerType.GATEWAY_PRIVATE == fe.getLoadBalancerType()
                        && GATEWAY_PRIVATE_IP_ADDRESS1.equals(fe.getIp()) && FRONTEND_2_NAME.equals(fe.getName()));
    }

    static Object[] zoneInfoFromAzure() {
        Map<String, List<String>> azureZoneInfo = new HashMap<>();
        azureZoneInfo.put("instanceType1", List.of("1"));
        azureZoneInfo.put("instanceType2", List.of("1", "2"));
        azureZoneInfo.put("instanceType3", List.of("1", "2", "3"));
        return new Object[]{
                Collections.emptyMap(),
                Map.of("instanceType1", List.of("1")),
                azureZoneInfo};
    }

    @ParameterizedTest
    @MethodSource("zoneInfoFromAzure")
    void testGetAvailabilityZones(Map<String, List<String>> azureZoneInfo) {
        VirtualMachines virtualMachines = mock(VirtualMachines.class);
        when(azureResourceManager.virtualMachines()).thenReturn(virtualMachines);
        ComputeManager computeManager = mock(ComputeManager.class);
        when(virtualMachines.manager()).thenReturn(computeManager);
        ComputeManagementClient computeManagementClient = mock(ComputeManagementClient.class);
        when(computeManager.serviceClient()).thenReturn(computeManagementClient);
        ResourceSkusClient resourceSkusClient = mock(ResourceSkusClient.class);
        when(computeManagementClient.getResourceSkus()).thenReturn(resourceSkusClient);
        PagedIterable<ResourceSkuInner> pagedIterable = mock(PagedIterable.class);
        when(resourceSkusClient.list(any(), any(), any())).thenReturn(pagedIterable);
        AzureListResult<ResourceSkuInner> azureListResult = mock(AzureListResult.class);
        when(azureListResultFactory.create(pagedIterable)).thenReturn(azureListResult);
        List<ResourceSkuInner> list = getResourceSkus(azureZoneInfo);
        when(azureListResult.getStream()).thenReturn(list.stream());
        Map<String, List<String>> zoneInfo = underTest.getAvailabilityZones("westus2");
        assertEquals(azureZoneInfo.size(), zoneInfo.size());
        azureZoneInfo.entrySet().stream().forEach(entry -> {
            assertTrue(zoneInfo.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), zoneInfo.get(entry.getKey()));
        });
    }

    @Test
    void testGetAvailabilityZonesWhenVmWhenVirtualMachineAZisNull() {
        VirtualMachines virtualMachines = mock(VirtualMachines.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(virtualMachines.getByResourceGroup(any(), any())).thenReturn(virtualMachine);
        when(virtualMachine.availabilityZones()).thenReturn(null);
        when(azureResourceManager.virtualMachines()).thenReturn(virtualMachines);

        Set<AvailabilityZoneId> availabilityZone = underTest.getAvailabilityZone("group", "name");
        assertEquals(availabilityZone.size(), 0);
    }

    static Object[] azureZones() {
        return new Object[]{null, "1", "2", "3"};
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @ParameterizedTest(name = "testCreateManagedDiskWithAvailabilityZoneForAZ{0}")
    @MethodSource("azureZones")
    public void testCreateManagedDiskWithAvailabilityZone(String availabilityZone) {
        WithCreate withCreate = setUpForDiskCreation();
        underTest.createManagedDisk(new AzureDisk("volume-1", 100, AzureDiskType.STANDARD_SSD_LRS, "westus2", "my-rg",
                Map.of(), null, availabilityZone));
        verify(withCreate, times(availabilityZone == null ? 0 : 1)).withAvailabilityZone(eq(AvailabilityZoneId.fromString(availabilityZone)));

        ArgumentCaptor<NetworkAccessPolicy> networkAccessPolicyArgumentCaptor = ArgumentCaptor.forClass(NetworkAccessPolicy.class);
        verify(diskInner).withNetworkAccessPolicy(networkAccessPolicyArgumentCaptor.capture());
        NetworkAccessPolicy networkAccessPolicy = networkAccessPolicyArgumentCaptor.getValue();
        assertNotNull(networkAccessPolicy);
        assertEquals(DENY_ALL, networkAccessPolicy);

        ArgumentCaptor<PublicNetworkAccess> publicNetworkAccessArgumentCaptor = ArgumentCaptor.forClass(PublicNetworkAccess.class);
        verify(diskInner).withPublicNetworkAccess(publicNetworkAccessArgumentCaptor.capture());
        PublicNetworkAccess publicNetworkAccess = publicNetworkAccessArgumentCaptor.getValue();
        assertNotNull(publicNetworkAccess);
        assertEquals(DISABLED, publicNetworkAccess);

    }

    private WithCreate setUpForDiskCreation() {
        Disks disks = mock(Disks.class);
        when(azureResourceManager.disks()).thenReturn(disks);
        Blank withBlank = mock(Blank.class);
        when(disks.define(any())).thenReturn(withBlank);
        WithGroup withGroup = mock(WithGroup.class);
        when(withBlank.withRegion(Region.US_WEST2)).thenReturn(withGroup);
        WithDiskSource withDiskSource = mock(WithDiskSource.class);
        when(withGroup.withExistingResourceGroup("my-rg")).thenReturn(withDiskSource);
        WithDataDiskSource withDataDiskSource = mock(WithDataDiskSource.class);
        when(withDiskSource.withData()).thenReturn(withDataDiskSource);
        when(withDataDiskSource.withSizeInGB(100)).thenReturn(withCreate);
        when(withCreate.withTags(any())).thenReturn(withCreate);
        when(withCreate.withSku(any())).thenReturn(withCreate);
        when(withCreate.withAvailabilityZone(any())).thenReturn(withCreate);
        HasInnerModel<DiskInner> hasInnerModel = (HasInnerModel<DiskInner>) withCreate;
        when(hasInnerModel.innerModel()).thenReturn(diskInner);
        return withCreate;
    }

    private List<ResourceSkuInner> getResourceSkus(Map<String, List<String>> instanceInformation) {
        return instanceInformation.entrySet().stream().map(entry -> {
            ResourceSkuInner resourceSkuInner = mock(ResourceSkuInner.class);
            ResourceSkuLocationInfo resourceSkuLocationInfo = mock(ResourceSkuLocationInfo.class);
            when(resourceSkuInner.name()).thenReturn(entry.getKey());
            when(resourceSkuInner.locationInfo()).thenReturn(List.of(resourceSkuLocationInfo));
            when(resourceSkuLocationInfo.zones()).thenReturn(entry.getValue());
            return resourceSkuInner;
        }).collect(Collectors.toList());
    }

    @Test
    void runWhatIfAnalysisShouldReturnEmptyOptionalOnSuccess() {
        String deploymentName = "yourDeploymentName";
        String validJsonTemplate = "{\"key\":\"value\"}";

        // Mocking the Azure API response
        WhatIfOperationResultInner operationResult = mock(WhatIfOperationResultInner.class);
        when(operationResult.status()).thenReturn("Succeeded");

        DeploymentsClient deploymentsClient = getDeploymentsClient();
        when(deploymentsClient.whatIf(any(), any(), any(), any())).thenReturn(operationResult);

        // When
        Optional<ManagementError> result = underTest.runWhatIfAnalysis(RESOURCE_GROUP_NAME, deploymentName, validJsonTemplate);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void runWhatIfAnalysisShouldReturnManagementErrorOnFailure() {
        String deploymentName = "yourDeploymentName";
        String validJsonTemplate = "{\"key\":\"value\"}";

        String message = "Marketplace purchase eligibilty check returned errors. See inner errors for details.";
        ManagementError managementError = AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", message);
        List<ManagementError> details = new ArrayList<>();
        details.add(AzureTestUtils.managementError("123", "detail1"));
        details.add(AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", message));
        AzureTestUtils.setDetails(managementError, details);

        // Mocking the Azure API response
        WhatIfOperationResultInner operationResult = mock(WhatIfOperationResultInner.class);
        when(operationResult.status()).thenReturn("Failed");
        when(operationResult.error()).thenReturn(managementError);

        // Mocking Azure Client behavior
        DeploymentsClient deploymentsClient = getDeploymentsClient();
        when(deploymentsClient.whatIf(any(), any(), any(), any())).thenReturn(operationResult);

        // When
        Optional<ManagementError> result = underTest.runWhatIfAnalysis(RESOURCE_GROUP_NAME, deploymentName, validJsonTemplate);

        // Then
        assertTrue(result.isPresent());
        assertEquals(managementError, result.get());
    }

    private DeploymentsClient getDeploymentsClient() {
        // Mocking Azure Client behavior
        GenericResourcesImpl genericResources = mock(GenericResourcesImpl.class);
        when(azureResourceManager.genericResources()).thenReturn(genericResources);
        ResourceManager resourceManager = mock(ResourceManager.class);
        when(genericResources.manager()).thenReturn(resourceManager);
        ResourceManagementClient resourceManagementClient = mock(ResourceManagementClient.class);
        when(resourceManager.serviceClient()).thenReturn(resourceManagementClient);
        DeploymentsClient deploymentsClient = mock(DeploymentsClient.class);
        when(resourceManagementClient.getDeployments()).thenReturn(deploymentsClient);
        return deploymentsClient;
    }

    @Test
    void testModifyDisk() {
        VirtualMachines virtualMachines = mock(VirtualMachines.class);
        when(azureResourceManager.virtualMachines()).thenReturn(virtualMachines);
        ComputeManager computeManager = mock(ComputeManager.class);
        when(virtualMachines.manager()).thenReturn(computeManager);
        ComputeManagementClient computeManagementClient = mock(ComputeManagementClient.class);
        when(computeManager.serviceClient()).thenReturn(computeManagementClient);
        DisksClient disksClient = mock(DisksClient.class);
        when(computeManagementClient.getDisks()).thenReturn(disksClient);
        underTest.modifyDisk("test-vol-name", "test-resource-group-name", 100, "Standard_LRS");
        verify(azureResourceManager, times(1)).virtualMachines();
        ArgumentCaptor<DiskUpdate> diskUpdateArgumentCaptor = ArgumentCaptor.forClass(DiskUpdate.class);
        verify(disksClient, times(1)).update(eq("test-resource-group-name"), eq("test-vol-name"),
                diskUpdateArgumentCaptor.capture());
        assertEquals(100, diskUpdateArgumentCaptor.getValue().diskSizeGB());
        assertEquals(DiskStorageAccountTypes.STANDARD_LRS, diskUpdateArgumentCaptor.getValue().sku().name());
    }

    @Test
    void testListDisksByTag() {
        AzureListResult<Disk> azureListResult = mock(AzureListResult.class);
        Disks disks = mock(Disks.class);
        Disk disk1 = mock(Disk.class);
        when(disk1.tags()).thenReturn(Map.of(TAG_NAME, "fqdn1"));
        Disk disk2 = mock(Disk.class);
        when(disk2.tags()).thenReturn(Map.of(TAG_NAME, "fqdn2"));
        Disk disk3 = mock(Disk.class);
        when(disk3.tags()).thenReturn(Map.of(TAG_NAME, "fqdn3"));
        Disk disk4 = mock(Disk.class);
        when(disk4.tags()).thenReturn(Map.of());
        Disk disk5 = mock(Disk.class);
        when(disk5.tags()).thenReturn(Map.of("OTHER_TAG", "TEST"));
        when(azureListResult.getAll()).thenReturn(List.of(disk1, disk2, disk3, disk4, disk5));
        when(azureResourceManager.disks()).thenReturn(disks);
        when(azureListResultFactory.listByResourceGroup(disks, RESOURCE_GROUP_NAME)).thenReturn(azureListResult);

        List<Disk> disk = underTest.listDisksByTag(RESOURCE_GROUP_NAME, TAG_NAME, List.of("fqdn1", "fqdn2"));

        assertEquals(2, disk.size());
        assertTrue(CollectionUtils.isEqualCollection(disk, List.of(disk1, disk2)));
    }

    @Test
    void testListDisksByTagNoneDisksReturned() {
        AzureListResult<Disk> azureListResult = mock(AzureListResult.class);
        Disks disks = mock(Disks.class);
        Disk disk1 = mock(Disk.class);
        when(disk1.tags()).thenReturn(Map.of(TAG_NAME, "fqdn1"));
        Disk disk2 = mock(Disk.class);
        when(disk2.tags()).thenReturn(Map.of(TAG_NAME, "fqdn2"));
        Disk disk3 = mock(Disk.class);
        when(disk3.tags()).thenReturn(Map.of(TAG_NAME, "fqdn3"));
        Disk disk4 = mock(Disk.class);
        when(disk4.tags()).thenReturn(Map.of());
        when(azureListResult.getAll()).thenReturn(List.of(disk1, disk2, disk3, disk4));
        when(azureResourceManager.disks()).thenReturn(disks);
        when(azureListResultFactory.listByResourceGroup(disks, RESOURCE_GROUP_NAME)).thenReturn(azureListResult);

        List<Disk> disk = underTest.listDisksByTag(RESOURCE_GROUP_NAME, TAG_NAME, List.of("fqdn4"));

        assertEquals(0, disk.size());
    }

    @Test
    void testGetStorageAccountFound() {
        Set<Kind> targetedAccountKinds = Set.of(Kind.STORAGE_V2, Kind.BLOCK_BLOB_STORAGE);
        StorageAccounts storageAccounts = mock(StorageAccounts.class);
        AzureListResult<StorageAccount> storageAccountAzureListResult = mock(AzureListResult.class);

        StorageAccount storageAccount1 = mock(StorageAccount.class);
        lenient().when(storageAccount1.kind()).thenReturn(Kind.STORAGE_V2);
        lenient().when(storageAccount1.name()).thenReturn("storageAccount1");

        StorageAccount storageAccount2 = mock(StorageAccount.class);
        lenient().when(storageAccount2.kind()).thenReturn(Kind.BLOCK_BLOB_STORAGE);
        lenient().when(storageAccount2.name()).thenReturn("storageAccount2");

        Set<StorageAccount> accounts = Set.of(
                storageAccount1,
                storageAccount2
        );

        when(storageAccountAzureListResult.getStream()).thenReturn(accounts.stream());
        when(azureResourceManager.storageAccounts()).thenReturn(storageAccounts);
        when(azureListResultFactory.list(eq(storageAccounts))).thenReturn(storageAccountAzureListResult);

        Optional<StorageAccount> result = underTest.getStorageAccount("storageAccount2", targetedAccountKinds);

        assertTrue(result.isPresent());
        assertEquals(storageAccount2, result.get());
    }

    @Test
    void testGetStorageAccountNotFound() {
        Set<Kind> targetedAccountKinds = Set.of(Kind.STORAGE_V2, Kind.BLOCK_BLOB_STORAGE);
        StorageAccounts storageAccounts = mock(StorageAccounts.class);
        AzureListResult<StorageAccount> storageAccountAzureListResult = mock(AzureListResult.class);

        StorageAccount storageAccount1 = mock(StorageAccount.class);
        lenient().when(storageAccount1.kind()).thenReturn(Kind.STORAGE_V2);
        lenient().when(storageAccount1.name()).thenReturn("storageAccount1");

        StorageAccount storageAccount2 = mock(StorageAccount.class);
        lenient().when(storageAccount2.kind()).thenReturn(Kind.BLOCK_BLOB_STORAGE);
        lenient().when(storageAccount2.name()).thenReturn("storageAccount2");

        Set<StorageAccount> accounts = Set.of(
                storageAccount1,
                storageAccount2
        );

        when(storageAccountAzureListResult.getStream()).thenReturn(accounts.stream());
        when(azureResourceManager.storageAccounts()).thenReturn(storageAccounts);
        when(azureListResultFactory.list(eq(storageAccounts))).thenReturn(storageAccountAzureListResult);

        Optional<StorageAccount> result = underTest.getStorageAccount("storageAccount3", targetedAccountKinds);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStorageAccountDifferentKind() {
        Set<Kind> targetedAccountKinds = Set.of(Kind.STORAGE_V2, Kind.BLOCK_BLOB_STORAGE);
        StorageAccounts storageAccounts = mock(StorageAccounts.class);
        AzureListResult<StorageAccount> storageAccountAzureListResult = mock(AzureListResult.class);

        StorageAccount storageAccount1 = mock(StorageAccount.class);
        lenient().when(storageAccount1.kind()).thenReturn(Kind.STORAGE_V2);
        lenient().when(storageAccount1.name()).thenReturn("storageAccount1");

        StorageAccount storageAccount2 = mock(StorageAccount.class);
        lenient().when(storageAccount2.kind()).thenReturn(Kind.BLOB_STORAGE);
        lenient().when(storageAccount2.name()).thenReturn("storageAccount2");

        Set<StorageAccount> accounts = Set.of(
                storageAccount1,
                storageAccount2
        );

        when(storageAccountAzureListResult.getStream()).thenReturn(accounts.stream());
        when(azureResourceManager.storageAccounts()).thenReturn(storageAccounts);
        when(azureListResultFactory.list(eq(storageAccounts))).thenReturn(storageAccountAzureListResult);

        Optional<StorageAccount> result = underTest.getStorageAccount("storageAccount2", targetedAccountKinds);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStorageAccountCaseInsensitiveName() {
        Set<Kind> targetedAccountKinds = Set.of(Kind.STORAGE_V2, Kind.BLOCK_BLOB_STORAGE);
        StorageAccounts storageAccounts = mock(StorageAccounts.class);
        AzureListResult<StorageAccount> storageAccountAzureListResult = mock(AzureListResult.class);

        StorageAccount storageAccount1 = mock(StorageAccount.class);
        lenient().when(storageAccount1.kind()).thenReturn(Kind.STORAGE_V2);
        lenient().when(storageAccount1.name()).thenReturn("storageAccount1");

        StorageAccount storageAccount2 = mock(StorageAccount.class);
        lenient().when(storageAccount2.kind()).thenReturn(Kind.BLOCK_BLOB_STORAGE);
        lenient().when(storageAccount2.name()).thenReturn("storageAccount2");

        Set<StorageAccount> accounts = Set.of(
                storageAccount1,
                storageAccount2
        );

        when(storageAccountAzureListResult.getStream()).thenReturn(accounts.stream());
        when(azureResourceManager.storageAccounts()).thenReturn(storageAccounts);
        when(azureListResultFactory.list(eq(storageAccounts))).thenReturn(storageAccountAzureListResult);

        Optional<StorageAccount> result = underTest.getStorageAccount("storageaccount2", targetedAccountKinds);

        assertTrue(result.isPresent());
        assertEquals(storageAccount2, result.get());
    }
}