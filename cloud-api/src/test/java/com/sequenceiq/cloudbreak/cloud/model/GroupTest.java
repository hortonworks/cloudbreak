package com.sequenceiq.cloudbreak.cloud.model;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@ExtendWith(MockitoExtension.class)
class GroupTest {

    private static final String GROUP_NAME = "myGroup";

    private static final String LOGIN_USER_NAME = "i_wanna_be_root";

    private static final String PUBLIC_KEY = "ssh-rsa yaddayaddayadda";

    private static final int ROOT_VOLUME_SIZE = 123;

    private static final String INSTANCE_ID = "machine-6789";

    @Mock
    private Security security;

    @Mock
    private InstanceAuthentication instanceAuthentication;

    @Test
    void constructorTestWhenInstancesIsNull() {
        assertThatNullPointerException().isThrownBy(() -> createGroup(null, createCloudInstance()));
    }

    @Test
    void constructorTestWhenInstancesContainingNull() {
        ArrayList<CloudInstance> instances = new ArrayList<>();
        instances.add(createCloudInstance());
        instances.add(null);
        instances.add(createCloudInstance());
        assertThatNullPointerException().isThrownBy(() -> createGroup(instances, createCloudInstance()));
    }

    @Test
    void getReferenceInstanceConfigurationTestWhenNoInstancesAndSkeletonIsNull() {
        Group group = createGroup(emptyList(), null);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(group::getReferenceInstanceConfiguration)
                .withMessage("There is no skeleton and instance available for Group -> name:" + GROUP_NAME);
    }

    @Test
    void getReferenceInstanceConfigurationTestWhenNoInstancesAndValidSkeleton() {
        CloudInstance cloudInstance = createCloudInstance();
        Group group = createGroup(emptyList(), cloudInstance);

        CloudInstance result = group.getReferenceInstanceConfiguration();
        assertThat(result).isSameAs(cloudInstance);
    }

    @Test
    void getReferenceInstanceConfigurationTestWhenSingleInstanceAndSkeletonIsNull() {
        CloudInstance cloudInstance = createCloudInstance();
        Group group = createGroup(List.of(cloudInstance), null);

        CloudInstance result = group.getReferenceInstanceConfiguration();
        assertThat(result).isSameAs(cloudInstance);
    }

    @Test
    void getReferenceInstanceConfigurationTestWhenSingleInstanceAndValidSkeleton() {
        CloudInstance cloudInstance = createCloudInstance();
        Group group = createGroup(List.of(cloudInstance), createCloudInstance());

        CloudInstance result = group.getReferenceInstanceConfiguration();
        assertThat(result).isSameAs(cloudInstance);
    }

    @Test
    void getReferenceInstanceConfigurationTestWhenMultipleInstances() {
        CloudInstance cloudInstanceFirst = createCloudInstance();
        Group group = createGroup(List.of(cloudInstanceFirst, createCloudInstance()), null);

        CloudInstance result = group.getReferenceInstanceConfiguration();
        assertThat(result).isSameAs(cloudInstanceFirst);
    }

    @Test
    void getReferenceInstanceTemplateTestWhenNoInstancesAndSkeletonIsNull() {
        Group group = createGroup(emptyList(), null);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(group::getReferenceInstanceTemplate)
                .withMessage("There is no skeleton and instance available for Group -> name:" + GROUP_NAME);
    }

    @Test
    void getReferenceInstanceTemplateTestWhenNoInstancesAndValidSkeleton() {
        CloudInstance cloudInstance = createCloudInstance();
        Group group = createGroup(emptyList(), cloudInstance);

        InstanceTemplate result = group.getReferenceInstanceTemplate();
        assertThat(result).isSameAs(cloudInstance.getTemplate());
    }

    @Test
    void getReferenceInstanceTemplateTestWhenSingleInstanceAndSkeletonIsNull() {
        CloudInstance cloudInstance = createCloudInstance();
        Group group = createGroup(List.of(cloudInstance), null);

        InstanceTemplate result = group.getReferenceInstanceTemplate();
        assertThat(result).isSameAs(cloudInstance.getTemplate());
    }

    @Test
    void getReferenceInstanceTemplateTestWhenSingleInstanceAndValidSkeleton() {
        CloudInstance cloudInstance = createCloudInstance();
        Group group = createGroup(List.of(cloudInstance), createCloudInstance());

        InstanceTemplate result = group.getReferenceInstanceTemplate();
        assertThat(result).isSameAs(cloudInstance.getTemplate());
    }

    @Test
    void getReferenceInstanceTemplateTestWhenMultipleInstances() {
        CloudInstance cloudInstanceFirst = createCloudInstance();
        Group group = createGroup(List.of(cloudInstanceFirst, createCloudInstance()), null);

        InstanceTemplate result = group.getReferenceInstanceTemplate();
        assertThat(result).isSameAs(cloudInstanceFirst.getTemplate());
    }

    @Test
    void builderTest() {
        String name = "name";
        InstanceGroupType type = InstanceGroupType.GATEWAY;
        List<CloudInstance> instances = new ArrayList<>();
        Security security = mock(Security.class);
        String publicKey = "publicKey";
        String loginUserName = "loginUserName";
        InstanceAuthentication instanceAuthentication = mock(InstanceAuthentication.class);
        CloudInstance skeleton = mock(CloudInstance.class);
        int rootVolumeSize = 50;
        CloudFileSystemView identity = mock(CloudFileSystemView.class);
        List<CloudInstance> deletedInstances = new ArrayList<>();
        GroupNetwork network = mock(GroupNetwork.class);
        Map<String, String> tags = new HashMap<>();
        String rootVolumeType = "rootVolumeType";
        Map<String, Object> parameters = new HashMap<>();

        Group underTest = Group.builder()
                .withName(name)
                .withType(type)
                .withInstances(instances)
                .withSecurity(security)
                .withPublicKey(publicKey)
                .withLoginUserName(loginUserName)
                .withInstanceAuthentication(instanceAuthentication)
                .withSkeleton(skeleton)
                .withRootVolumeSize(rootVolumeSize)
                .withIdentity(Optional.of(identity))
                .withDeletedInstances(deletedInstances)
                .withNetwork(network)
                .withTags(tags)
                .withRootVolumeType(rootVolumeType)
                .withParameters(parameters)
                .build();

        assertEquals(name, underTest.getName());
        assertEquals(type, underTest.getType());
        assertEquals(instances, underTest.getInstances());
        assertEquals(security, underTest.getSecurity());
        assertEquals(publicKey, underTest.getPublicKey());
        assertEquals(loginUserName, underTest.getLoginUserName());
        assertEquals(instanceAuthentication, underTest.getInstanceAuthentication());
        assertEquals(skeleton, underTest.getReferenceInstanceConfiguration());
        assertEquals(rootVolumeSize, underTest.getRootVolumeSize());
        assertEquals(identity, underTest.getIdentity().get());
        assertEquals(deletedInstances, underTest.getDeletedInstances());
        assertEquals(network, underTest.getNetwork());
        assertEquals(tags, underTest.getTags());
        assertEquals(rootVolumeType, underTest.getRootVolumeType());
        assertEquals(parameters, underTest.getParameters());
    }

    private Group createGroup(Collection<CloudInstance> instances, CloudInstance skeleton) {
        return Group.builder()
                .withName(GROUP_NAME)
                .withType(InstanceGroupType.GATEWAY)
                .withInstances(instances)
                .withSecurity(security)
                .withSkeleton(skeleton)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(LOGIN_USER_NAME)
                .withPublicKey(PUBLIC_KEY)
                .withRootVolumeSize(ROOT_VOLUME_SIZE)
                .withNetwork(createGroupNetwork())
                .build();
    }

    private CloudInstance createCloudInstance() {
        return new CloudInstance(INSTANCE_ID, mock(InstanceTemplate.class), instanceAuthentication, "subnet-123", "eu1a");
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

}