package com.sequenceiq.cloudbreak.cloud.azure.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class AzureStackViewTest {

    private static final String STACK_NAME = "stackName";

    private static final int STACK_NAME_PREFIX_LENGTH = 12;

    private static final String CUSTOM_IMAGE_NAME = "customImageName";

    private static final String INSTANCE_ID = "instanceId";

    private static final InstanceGroupType TYPE = InstanceGroupType.CORE;

    private static final String GROUP_NAME = "groupName";

    private static final int ROOT_VOLUME_SIZE = 12;

    private static final String ATTACHED_DISK_STORAGE_NAME = "attachedDiskStorageName";

    private static final String ATTACHED_DISK_STORAGE_TYPE = "attachedDiskStorageType";

    private static final String INSTANCE_SUBNET_ID = "instanceSubnetId";

    private static final String MANAGED_IDENTITY = "managedIdentity";

    @Mock
    private Group group;

    @Mock
    private AzureStorageView armStorageView;

    @Mock
    private AzureSubnetStrategy subnetStrategy;

    @Mock
    private CloudInstance instance;

    @Mock
    private InstanceTemplate template;

    @Mock
    private Volume volume;

    @Mock
    private CloudAdlsGen2View cloudAdlsGen2View;

    @Test
    void constructorTestWhenNoGroup() {
        AzureStackView underTest = new AzureStackView(STACK_NAME, STACK_NAME_PREFIX_LENGTH, List.of(), armStorageView, subnetStrategy, Map.of());

        Map<String, List<AzureInstanceView>> groups = underTest.getInstancesByGroupType();
        assertThat(groups).isNotNull();
        assertThat(groups).isEmpty();

        List<AzureInstanceGroupView> instanceGroups = underTest.getInstanceGroups();
        assertThat(instanceGroups).isNotNull();
        assertThat(instanceGroups).isEmpty();

        List<String> instanceGroupNames = underTest.getInstanceGroupNames();
        assertThat(instanceGroupNames).isNotNull();
        assertThat(instanceGroupNames).isEmpty();
    }

    @Test
    void constructorTestWhenGroupGivenWithoutInstance() {
        initGroupBasics();

        AzureStackView underTest = new AzureStackView(STACK_NAME, STACK_NAME_PREFIX_LENGTH, List.of(group), armStorageView, subnetStrategy, Map.of());

        Map<String, List<AzureInstanceView>> groups = underTest.getInstancesByGroupType();
        assertThat(groups).isNotNull();
        assertThat(groups).isEmpty();

        verifyInstanceGroups(underTest.getInstanceGroups(), false);
        verifyInstanceGroupNames(underTest.getInstanceGroupNames());
    }

    @Test
    void constructorTestWhenGroupGivenWithInstance() {
        initGroupBasics();
        initInstances();

        AzureStackView underTest = new AzureStackView(STACK_NAME, STACK_NAME_PREFIX_LENGTH, List.of(group), armStorageView, subnetStrategy,
                Map.of(INSTANCE_ID, CUSTOM_IMAGE_NAME));

        Map<String, List<AzureInstanceView>> groups = underTest.getInstancesByGroupType();
        assertThat(groups).isNotNull();
        assertThat(groups).hasSize(1);

        List<AzureInstanceView> azureInstanceViews = groups.get(TYPE.name());
        assertThat(azureInstanceViews).isNotNull();
        assertThat(azureInstanceViews).hasSize(1);

        AzureInstanceView azureInstanceView = azureInstanceViews.get(0);
        assertThat(azureInstanceView).isNotNull();
        assertThat(azureInstanceView.getInstance()).isSameAs(instance);
        assertThat(ReflectionTestUtils.getField(azureInstanceView, "stackName")).isEqualTo(STACK_NAME);
        assertThat(ReflectionTestUtils.getField(azureInstanceView, "stackNamePrefixLength")).isEqualTo(STACK_NAME_PREFIX_LENGTH);
        assertThat(azureInstanceView.getType()).isEqualTo(TYPE);
        assertThat(azureInstanceView.getAttachedDiskStorageName()).isEqualTo(ATTACHED_DISK_STORAGE_NAME);
        assertThat(azureInstanceView.getAttachedDiskStorageType()).isEqualTo(ATTACHED_DISK_STORAGE_TYPE);
        assertThat(azureInstanceView.getGroupName()).isEqualTo(GROUP_NAME);
        assertThat(azureInstanceView.getAvailabilitySetName()).isNull();
        assertThat(azureInstanceView.isManagedDisk()).isTrue();
        assertThat(azureInstanceView.getSubnetId()).isEqualTo(INSTANCE_SUBNET_ID);
        assertThat(azureInstanceView.getRootVolumeSize()).isEqualTo(ROOT_VOLUME_SIZE);
        assertThat(azureInstanceView.getCustomImageId()).isEqualTo(CUSTOM_IMAGE_NAME);
        assertThat(azureInstanceView.getManagedIdentity()).isEqualTo(MANAGED_IDENTITY);

        verifyInstanceGroups(underTest.getInstanceGroups(), true);
        verifyInstanceGroupNames(underTest.getInstanceGroupNames());
    }

    private void verifyInstanceGroups(List<AzureInstanceGroupView> instanceGroups, boolean managedDiskExpected) {
        assertThat(instanceGroups).isNotNull();
        assertThat(instanceGroups).hasSize(1);

        AzureInstanceGroupView azureInstanceGroupView = instanceGroups.get(0);
        assertThat(azureInstanceGroupView).isNotNull();
        assertThat(azureInstanceGroupView.getName()).isEqualTo(GROUP_NAME);
        assertThat(azureInstanceGroupView.getRootVolumeSize()).isEqualTo(ROOT_VOLUME_SIZE);
        assertThat(azureInstanceGroupView.isManagedDisk()).isEqualTo(managedDiskExpected);
    }

    private void verifyInstanceGroupNames(List<String> instanceGroupNames) {
        assertThat(instanceGroupNames).isNotNull();
        assertThat(instanceGroupNames).hasSize(1);

        assertThat(instanceGroupNames.get(0)).isEqualTo(GROUP_NAME);
    }

    private void initGroupBasics() {
        when(group.getType()).thenReturn(TYPE);
        when(group.getName()).thenReturn(GROUP_NAME);
        when(group.getRootVolumeSize()).thenReturn(ROOT_VOLUME_SIZE);
    }

    private void initInstances() {
        when(group.getInstances()).thenReturn(List.of(instance));
        when(group.getIdentity()).thenReturn(Optional.of(cloudAdlsGen2View));

        when(instance.getTemplate()).thenReturn(template);
        when(instance.getInstanceId()).thenReturn(INSTANCE_ID);
        when(armStorageView.getAttachedDiskStorageName(template)).thenReturn(ATTACHED_DISK_STORAGE_NAME);
        when(template.getVolumes()).thenReturn(List.of(volume));
        when(volume.getType()).thenReturn(ATTACHED_DISK_STORAGE_TYPE);
        when(cloudAdlsGen2View.getManagedIdentity()).thenReturn(MANAGED_IDENTITY);

        when(subnetStrategy.getNextSubnetId()).thenReturn(INSTANCE_SUBNET_ID);
    }

}