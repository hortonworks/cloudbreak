package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.compute.Encryption;
import com.microsoft.azure.management.compute.implementation.DiskInner;
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

}