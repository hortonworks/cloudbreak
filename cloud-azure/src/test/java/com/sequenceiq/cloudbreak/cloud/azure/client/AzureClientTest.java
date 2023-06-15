package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
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

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.Region;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.fluent.ComputeManagementClient;
import com.azure.resourcemanager.compute.fluent.ResourceSkusClient;
import com.azure.resourcemanager.compute.fluent.models.DiskInner;
import com.azure.resourcemanager.compute.fluent.models.ResourceSkuInner;
import com.azure.resourcemanager.compute.fluent.models.VirtualMachineInner;
import com.azure.resourcemanager.compute.models.CachingTypes;
import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.DiskSkuTypes;
import com.azure.resourcemanager.compute.models.Disks;
import com.azure.resourcemanager.compute.models.Encryption;
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
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.Server.Update;
import com.azure.resourcemanager.postgresql.models.Servers;
import com.azure.resourcemanager.resources.fluentcore.arm.AvailabilityZoneId;
import com.azure.resourcemanager.resources.fluentcore.model.implementation.IndexableRefreshableWrapperImpl;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDisk;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureLoadBalancerFrontend;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
class AzureClientTest {

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private static final String RESOURCE_GROUP_NAME = "rg";

    private static final String SERVER_NAME = "serverName";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String PRIVATE_LB_NAME = "privateLb";

    private static final String PRIVATE_IP_ADDRESS1 = "10.11.12.13";

    private static final String FRONTEND_1_NAME = "frontend1";

    private static final String FRONTEND_2_NAME = "frontend2-gateway";

    private static final String GATEWAY_PRIVATE_IP_ADDRESS1 = "110.111.112.113";

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

    private AzureClient underTest;

    @Mock(extraInterfaces = Disk.DefinitionStages.WithCreate.class)
    private IndexableRefreshableWrapperImpl withCreateIndexableRefreshableWrapperImpl;

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
        lenient().doCallRealMethod().when(azureExceptionHandler).handleException(any(Runnable.class));

        underTest = new AzureClient(azureClientCredentials, azureExceptionHandler, azureListResultFactory);
    }

    static Object[][] setupDiskEncryptionWithDesIfNeededTestWhenDesAbsentDataProvider() {
        return new Object[][] {
                // testCaseName diskEncryptionSetId
                { "diskEncryptionSetId=null", null },
                { "diskEncryptionSetId=\"\"", "" },
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("setupDiskEncryptionWithDesIfNeededTestWhenDesAbsentDataProvider")
    void setupDiskEncryptionWithDesIfNeededTestWhenDesAbsent(String testCaseName, String diskEncryptionSetId) {
        underTest.setupDiskEncryptionWithDesIfNeeded(diskEncryptionSetId, (Disk.DefinitionStages.WithCreate) withCreateIndexableRefreshableWrapperImpl);

        verify(withCreateIndexableRefreshableWrapperImpl, never()).innerModel();
    }

    @Test
    void setupDiskEncryptionWithDesIfNeededTestWhenDesGiven() {
        when(withCreateIndexableRefreshableWrapperImpl.innerModel()).thenReturn(diskInner);

        underTest.setupDiskEncryptionWithDesIfNeeded(DISK_ENCRYPTION_SET_ID, (Disk.DefinitionStages.WithCreate) withCreateIndexableRefreshableWrapperImpl);

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
        assertThat(Disk.DefinitionStages.WithCreate.class.isAssignableFrom(clazzDiskImpl)).isTrue();
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
        Assertions.assertEquals(CachingTypes.NONE, captor.getValue());
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
        Assertions.assertEquals(CachingTypes.READ_ONLY, captor.getValue());
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
        Assertions.assertEquals(CachingTypes.READ_ONLY, captor.getValue());
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

        Assertions.assertTrue(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "100"));
        Assertions.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "200"));
        Assertions.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "300"));
        Assertions.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "400"));
        Assertions.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "dummy"));
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
    @EnumSource(value = LoadBalancerType.class, names = { "PRIVATE", "GATEWAY_PRIVATE" })
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
    @EnumSource(value = LoadBalancerType.class, names = { "PRIVATE", "GATEWAY_PRIVATE" })
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

    @Test
    void testUpdateAdministratorLoginPassword() {
        Servers servers = mock(Servers.class);
        when(postgreSqlManager.servers()).thenReturn(servers);
        Server server = mock(Server.class);
        when(servers.getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME))).thenReturn(server);
        Update update = mock(Update.class);
        when(server.update()).thenReturn(update);
        when(update.withAdministratorLoginPassword(eq(NEW_PASSWORD))).thenReturn(update);
        when(update.apply()).thenReturn(server);
        underTest.updateAdministratorLoginPassword(RESOURCE_GROUP_NAME, SERVER_NAME, NEW_PASSWORD);
        verify(postgreSqlManager, times(1)).servers();
        verify(servers, times(1)).getByResourceGroup(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME));
        verify(server, times(1)).update();
        verify(update, times(1)).withAdministratorLoginPassword(eq(NEW_PASSWORD));
        verify(update, times(1)).apply();
    }

    static Object[] zoneInfoFromAzure() {
        Map<String, List<String>> azureZoneInfo = new HashMap<>();
        azureZoneInfo.put("instanceType1", List.of("1"));
        azureZoneInfo.put("instanceType2", List.of("1", "2"));
        azureZoneInfo.put("instanceType3", List.of("1", "2", "3"));
        return new Object[] {Collections.emptyMap(),
                Map.of("instanceType1", List.of("1")),
                azureZoneInfo
                };
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
        Assertions.assertEquals(azureZoneInfo.size(), zoneInfo.size());
        azureZoneInfo.entrySet().stream().forEach(entry -> {
            Assertions.assertEquals(true, zoneInfo.containsKey(entry.getKey()));
            Assertions.assertEquals(entry.getValue(), zoneInfo.get(entry.getKey()));
        });
    }

    static Object[] azureZones() {
        return new Object[]{null, "1", "2", "3"};
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @ParameterizedTest(name = "testCreateManagedDiskWithAvailabilityZoneForAZ{0}")
    @MethodSource("azureZones")
    public void testCreateManagedDiskWithAvailabilityZone(String availabilityZone) {
        Disk.DefinitionStages.WithCreate withCreate = setUpForDiskCreation();
        underTest.createManagedDisk(new AzureDisk("volume-1", 100, AzureDiskType.STANDARD_SSD_LRS, "westus2", "my-rg",
                Map.of(), null, availabilityZone));
        verify(withCreate, times(availabilityZone == null ? 0 : 1)).withAvailabilityZone(eq(AvailabilityZoneId.fromString(availabilityZone)));
    }

    private Disk.DefinitionStages.WithCreate setUpForDiskCreation() {
        Disks disks = mock(Disks.class);
        when(azureResourceManager.disks()).thenReturn(disks);
        Disk.DefinitionStages.Blank withBlank = mock(Disk.DefinitionStages.Blank.class);
        when(disks.define(any())).thenReturn(withBlank);
        Disk.DefinitionStages.WithGroup withGroup = mock(Disk.DefinitionStages.WithGroup.class);
        when(withBlank.withRegion(Region.US_WEST2)).thenReturn(withGroup);
        Disk.DefinitionStages.WithDiskSource withDiskSource = mock(Disk.DefinitionStages.WithDiskSource.class);
        when(withGroup.withExistingResourceGroup("my-rg")).thenReturn(withDiskSource);
        Disk.DefinitionStages.WithDataDiskSource withDataDiskSource = mock(Disk.DefinitionStages.WithDataDiskSource.class);
        when(withDiskSource.withData()).thenReturn(withDataDiskSource);
        Disk.DefinitionStages.WithCreate withCreate = mock(Disk.DefinitionStages.WithCreate.class);
        when(withDataDiskSource.withSizeInGB(100)).thenReturn(withCreate);
        when(withCreate.withTags(any())).thenReturn(withCreate);
        when(withCreate.withSku(any())).thenReturn(withCreate);
        when(withCreate.withAvailabilityZone(any())).thenReturn(withCreate);
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
}
