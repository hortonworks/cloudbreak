package com.sequenceiq.cloudbreak.cloud.azure.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class AzureInstanceViewTest {

    private static final int STACK_NAME_PREFIX_LENGTH = 12;

    private static final InstanceGroupType TYPE = InstanceGroupType.CORE;

    private static final String ATTACHED_DISK_STORAGE = "attachedDiskStorage";

    private static final String ATTACHED_DISK_STORAGE_TYPE = "attachedDiskStorageType";

    private static final String GROUP_NAME = "groupName";

    private static final String STACK_NAME = "stackName";

    private static final String AVAILABILITY_SET_NAME = "availabilitySetName";

    private static final boolean MANAGED_DISK = true;

    private static final String SUBNET_ID = "subnetId";

    private static final int ROOT_VOLUME_SIZE = 34;

    private static final String CUSTOM_IMAGE_ID = "customImageId";

    private static final String MANAGED_IDENTITY = "managedIdentity";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private InstanceTemplate instanceTemplate;

    @BeforeEach
    void setUp() {
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
    }

    @Test
    void builderTestMinimal() {
        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance).build();

        assertThat(underTest.getInstance()).isSameAs(cloudInstance);
    }

    @Test
    void builderTestComplete() {
        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance)
                .withStackNamePrefixLength(STACK_NAME_PREFIX_LENGTH)
                .withType(TYPE)
                .withAttachedDiskStorage(ATTACHED_DISK_STORAGE)
                .withAttachedDiskStorageType(ATTACHED_DISK_STORAGE_TYPE)
                .withGroupName(GROUP_NAME)
                .withStackName(STACK_NAME)
                .withAvailabilitySetName(AVAILABILITY_SET_NAME)
                .withManagedDisk(MANAGED_DISK)
                .withSubnetId(SUBNET_ID)
                .withRootVolumeSize(ROOT_VOLUME_SIZE)
                .withCustomImageId(CUSTOM_IMAGE_ID)
                .withManagedIdentity(MANAGED_IDENTITY)
                .build();

        assertThat(underTest.getInstance()).isSameAs(cloudInstance);
        assertThat(ReflectionTestUtils.getField(underTest, "stackNamePrefixLength")).isEqualTo(STACK_NAME_PREFIX_LENGTH);
        assertThat(underTest.getType()).isSameAs(TYPE);
        assertThat(underTest.getAttachedDiskStorageName()).isEqualTo(ATTACHED_DISK_STORAGE);
        assertThat(underTest.getAttachedDiskStorageType()).isEqualTo(ATTACHED_DISK_STORAGE_TYPE);
        assertThat(underTest.getGroupName()).isEqualTo(GROUP_NAME);
        assertThat(ReflectionTestUtils.getField(underTest, "stackName")).isEqualTo(STACK_NAME);
        assertThat(underTest.getAvailabilitySetName()).isEqualTo(AVAILABILITY_SET_NAME);
        assertThat(underTest.isManagedDisk()).isEqualTo(MANAGED_DISK);
        assertThat(underTest.getSubnetId()).isEqualTo(SUBNET_ID);
        assertThat(underTest.getRootVolumeSize()).isEqualTo(ROOT_VOLUME_SIZE);
        assertThat(underTest.getCustomImageId()).isEqualTo(CUSTOM_IMAGE_ID);
        assertThat(underTest.getManagedIdentity()).isEqualTo(MANAGED_IDENTITY);
    }

    static Object[][] isManagedDiskEncryptionWithCustomKeyEnabledDataProvider() {
        return new Object[][]{
                // testCaseName propertyValue expectedResult
                {"null", null, false},
                {"false", false, false},
                {"true", true, true},
                {"\"false\"", "false", false},
                {"\"true\"", "true", true},
                {"\"True\"", "True", true},
                {"\"foo\"", "foo", false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("isManagedDiskEncryptionWithCustomKeyEnabledDataProvider")
    void isManagedDiskEncryptionWithCustomKeyEnabledTest(String testCaseName, Object propertyValue, boolean expectedResult) {
        when(instanceTemplate.getParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Object.class)).thenReturn(propertyValue);

        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance).build();

        assertThat(underTest.isManagedDiskEncryptionWithCustomKeyEnabled()).isEqualTo(expectedResult);
    }

    @Test
    void getDiskEncryptionSetIdTestWhenAbsent() {
        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance).build();

        assertThat(underTest.getDiskEncryptionSetId()).isNull();
    }

    @Test
    void getDiskEncryptionSetIdTestWhenGiven() {
        when(instanceTemplate.getStringParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).thenReturn(DISK_ENCRYPTION_SET_ID);

        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance).build();

        assertThat(underTest.getDiskEncryptionSetId()).isEqualTo(DISK_ENCRYPTION_SET_ID);
    }

    @Test
    void getInstanceIdTestWithCustomInstanceName() {
        String instanceName = "perdos-azure-sdx-3-m0-13664bf0a237bf859";

        when(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME)).thenReturn(instanceName);

        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance)
                .withStackName("perdos-azure-sdx-3")
                .build();

        assertThat(underTest.getInstanceId()).isEqualTo("m0-13664bf0a237bf859");
    }

    @Test
    void getInstanceIdTestWithoutInstanceName() {
        String dbId = "98765";
        String groupName = "master";
        Long privateId = 2L;

        when(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME)).thenReturn(null);
        when(cloudInstance.getDbIdOrDefaultIfNotExists()).thenReturn(dbId);
        when(instanceTemplate.getGroupName()).thenReturn(groupName);
        when(instanceTemplate.getPrivateId()).thenReturn(privateId);

        AzureInstanceView underTest = AzureInstanceView.builder(cloudInstance)
                .withStackName("perdos-azure-sdx-3")
                .build();

        assertThat(underTest.getInstanceId()).isEqualTo("m2-c37bf859");
    }
}