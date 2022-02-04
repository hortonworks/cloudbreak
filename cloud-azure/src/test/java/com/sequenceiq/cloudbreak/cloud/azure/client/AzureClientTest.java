package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.compute.CachingTypes;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.DiskSkuTypes;
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.compute.Encryption;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.implementation.DiskInner;
import com.microsoft.azure.management.compute.implementation.VirtualMachineInner;
import com.microsoft.azure.management.keyvault.AccessPolicy;
import com.microsoft.azure.management.keyvault.AccessPolicyEntry;
import com.microsoft.azure.management.keyvault.KeyPermissions;
import com.microsoft.azure.management.keyvault.Permissions;
import com.microsoft.azure.management.resources.fluentcore.model.implementation.IndexableRefreshableWrapperImpl;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AzureClientTest {

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    @Mock
    private AzureClientCredentials azureClientCredentials;

    @Mock
    private AzureAuthExceptionHandler azureAuthExceptionHandler;

    @InjectMocks
    private AzureClient underTest;

    @Mock(extraInterfaces = Disk.DefinitionStages.WithCreate.class)
    private IndexableRefreshableWrapperImpl withCreateIndexableRefreshableWrapperImpl;

    @Mock
    private DiskInner diskInner;

    @Captor
    private ArgumentCaptor<Encryption> encryptionCaptor;

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
        underTest.setupDiskEncryptionWithDesIfNeeded(diskEncryptionSetId, (Disk.DefinitionStages.WithCreate) withCreateIndexableRefreshableWrapperImpl);

        verify(withCreateIndexableRefreshableWrapperImpl, never()).inner();
    }

    @Test
    void setupDiskEncryptionWithDesIfNeededTestWhenDesGiven() {
        when(withCreateIndexableRefreshableWrapperImpl.inner()).thenReturn(diskInner);

        underTest.setupDiskEncryptionWithDesIfNeeded(DISK_ENCRYPTION_SET_ID, (Disk.DefinitionStages.WithCreate) withCreateIndexableRefreshableWrapperImpl);

        verify(diskInner).withEncryption(encryptionCaptor.capture());
        Encryption encryption = encryptionCaptor.getValue();
        assertThat(encryption).isNotNull();
        assertThat(encryption.diskEncryptionSetId()).isEqualTo(DISK_ENCRYPTION_SET_ID);
    }

    @Test
    void setupDiskEncryptionWithDesIfNeededTestWhenAzureSdkInternalTypeChecks() throws ClassNotFoundException {
        // This class is package private
        Class<?> clazzDisksImpl = Class.forName("com.microsoft.azure.management.compute.implementation.DisksImpl");
        assertThat(Disks.class.isAssignableFrom(clazzDisksImpl)).isTrue();

        // This class is package private
        Class<?> clazzDiskImpl = Class.forName("com.microsoft.azure.management.compute.implementation.DiskImpl");
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
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);

        when(virtualMachine.inner()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.apply()).thenReturn(virtualMachine);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        ArgumentCaptor<CachingTypes> captor = ArgumentCaptor.forClass(CachingTypes.class);

        underTest.attachDiskToVm(disk, virtualMachine);
        verify(virtualMachineUpdate, times(1)).withDataDiskDefaultCachingType(captor.capture());
        Assert.assertEquals(CachingTypes.NONE, captor.getValue());
    }

    @Test
    public void testAttachDiskToVmWhenEverythingIsNormalShouldUseCachingREAD() {
        int sizeInGb = 4095;
        DiskSkuTypes diskSkuTypes = DiskSkuTypes.PREMIUM_LRS;

        Disk disk = mock(Disk.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);

        when(virtualMachine.inner()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.apply()).thenReturn(virtualMachine);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        when(disk.sku()).thenReturn(diskSkuTypes);
        ArgumentCaptor<CachingTypes> captor = ArgumentCaptor.forClass(CachingTypes.class);

        underTest.attachDiskToVm(disk, virtualMachine);
        verify(virtualMachineUpdate, times(1)).withDataDiskDefaultCachingType(captor.capture());
        Assert.assertEquals(CachingTypes.READ_ONLY, captor.getValue());
    }

    @Test
    public void testAttachDiskToVmWhenEDiskTypeUltraSSDShouldUseCachingREADONLY() {
        int sizeInGb = 4095;
        DiskSkuTypes diskSkuTypes = DiskSkuTypes.ULTRA_SSD_LRS;

        Disk disk = mock(Disk.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInner virtualMachineInner = mock(VirtualMachineInner.class);
        VirtualMachine.Update virtualMachineUpdate = mock(VirtualMachine.Update.class);

        when(virtualMachine.inner()).thenReturn(virtualMachineInner);
        when(virtualMachineInner.withPlan(null)).thenReturn(virtualMachineInner);
        when(virtualMachine.update()).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withExistingDataDisk(any(Disk.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.withDataDiskDefaultCachingType(any(CachingTypes.class))).thenReturn(virtualMachineUpdate);
        when(virtualMachineUpdate.apply()).thenReturn(virtualMachine);
        when(disk.sizeInGB()).thenReturn(sizeInGb);
        when(disk.sku()).thenReturn(diskSkuTypes);
        ArgumentCaptor<CachingTypes> captor = ArgumentCaptor.forClass(CachingTypes.class);

        underTest.attachDiskToVm(disk, virtualMachine);
        verify(virtualMachineUpdate, times(1)).withDataDiskDefaultCachingType(captor.capture());
        Assert.assertEquals(CachingTypes.READ_ONLY, captor.getValue());
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

        when(accessPolicy1.inner()).thenReturn(new AccessPolicyEntry()
                .withObjectId("100")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY, KeyPermissions.GET))));

        when(accessPolicy2.inner()).thenReturn(new AccessPolicyEntry()
                .withObjectId("200")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.UNWRAP_KEY, KeyPermissions.GET))));

        when(accessPolicy3.inner()).thenReturn(new AccessPolicyEntry()
                .withObjectId("300")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.GET))));

        when(accessPolicy4.inner()).thenReturn(new AccessPolicyEntry()
                .withObjectId("400")
                .withPermissions(new Permissions().withKeys(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY))));


        Assert.assertTrue(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "100"));
        Assert.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "200"));
        Assert.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "300"));
        Assert.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "400"));
        Assert.assertFalse(underTest.checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, "dummy"));
    }

}