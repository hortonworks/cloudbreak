package com.sequenceiq.common.api.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceTypeTest {

    private static final Set<ResourceType> TEMPLATE_TYPES = EnumSet.of(ResourceType.CLOUDFORMATION_STACK, ResourceType.ARM_TEMPLATE,
            ResourceType.YARN_APPLICATION, ResourceType.YARN_LOAD_BALANCER);

    private static final Set<ResourceType> INSTANCE_TYPES = EnumSet.of(ResourceType.GCP_INSTANCE, ResourceType.OPENSTACK_INSTANCE, ResourceType.MOCK_INSTANCE);

    private static final Set<ResourceType> CANARY_TYPES = EnumSet.of(ResourceType.RDS_HOSTNAME_CANARY, ResourceType.AZURE_DATABASE_CANARY,
            ResourceType.AZURE_PRIVATE_ENDPOINT_CANARY, ResourceType.AZURE_DNS_ZONE_GROUP_CANARY);

    private static final Set<ResourceType> VOLUME_SET_TYPES = EnumSet.of(ResourceType.AWS_VOLUMESET, ResourceType.GCP_ATTACHED_DISKSET,
            ResourceType.AZURE_VOLUMESET, ResourceType.OPENSTACK_ATTACHED_DISK);

    static Iterable<?> resourceTypesDataProvider() {
        return Arrays.stream(ResourceType.values())
                .filter(type -> !TEMPLATE_TYPES.contains(type))
                .filter(type -> !CANARY_TYPES.contains(type))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceTypesDataProvider")
    void getCommonResourceTypeTestWhenRESOURCE(ResourceType resourceType) {
        assertThat(resourceType.getCommonResourceType()).isEqualTo(CommonResourceType.RESOURCE);
    }

    static Iterable<?> templateTypesDataProvider() {
        return TEMPLATE_TYPES;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templateTypesDataProvider")
    void getCommonResourceTypeTestWhenTEMPLATE(ResourceType resourceType) {
        assertThat(resourceType.getCommonResourceType()).isEqualTo(CommonResourceType.TEMPLATE);
    }

    static Iterable<?> notInstanceTypesDataProvider() {
        return Arrays.stream(ResourceType.values()).filter(type -> !INSTANCE_TYPES.contains(type)).collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("notInstanceTypesDataProvider")
    void isInstanceResourceTestWhenFalse(ResourceType resourceType) {
        assertThat(ResourceType.isInstanceResource(resourceType)).isFalse();
    }

    static Iterable<?> instanceTypesDataProvider() {
        return INSTANCE_TYPES;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("instanceTypesDataProvider")
    void isInstanceResourceTestWhenTrue(ResourceType resourceType) {
        assertThat(ResourceType.isInstanceResource(resourceType)).isTrue();
    }

    static Iterable<?> canaryTypesDataProvider() {
        return CANARY_TYPES;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("canaryTypesDataProvider")
    void getCommonResourceTypeTestWhenCANARY(ResourceType resourceType) {
        assertThat(resourceType.getCommonResourceType()).isEqualTo(CommonResourceType.CANARY);
    }

    static Iterable<?> volumeSetTypesDataProvider() {
        return VOLUME_SET_TYPES;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("volumeSetTypesDataProvider")
    void isVolumeSetTestWhenTrue(ResourceType resourceType) {
        assertThat(ResourceType.isVolumeSet(resourceType)).isTrue();
    }

    static Iterable<?> notVolumeSetTypesDataProvider() {
        return Arrays.stream(ResourceType.values()).filter(type -> !VOLUME_SET_TYPES.contains(type)).collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("notVolumeSetTypesDataProvider")
    void isVolumeSetTestWhenFalse(ResourceType resourceType) {
        assertThat(ResourceType.isVolumeSet(resourceType)).isFalse();
    }
}