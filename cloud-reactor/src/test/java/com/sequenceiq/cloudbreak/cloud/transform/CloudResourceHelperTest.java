package com.sequenceiq.cloudbreak.cloud.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

class CloudResourceHelperTest {

    private static final CloudResource CLOUD_RESOURCE_AWS_INSTANCE = createCloudResourceByType(ResourceType.AWS_INSTANCE);

    private static final CloudResource CLOUD_RESOURCE_AWS_INSTANCE_2 = createCloudResourceByType(ResourceType.AWS_INSTANCE);

    private static final CloudResource CLOUD_RESOURCE_AWS_VPC = createCloudResourceByType(ResourceType.AWS_VPC);

    private CloudResourceHelper underTest;

    @BeforeEach
    void setUp() {
        underTest = new CloudResourceHelper();
    }

    private static CloudResource createCloudResourceByType(ResourceType resourceType) {
        return CloudResource.builder()
                .type(resourceType)
                .status(CommonStatus.CREATED)
                .name("name")
                .build();
    }

    static Object[][] getResourceTypeFromListDataProvider() {
        return new Object[][]{
                // testCaseName type resources resultExpected
                {"AWS_INSTANCE, []", ResourceType.AWS_INSTANCE, List.of(), null},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_INSTANCE]", ResourceType.AWS_INSTANCE, List.of(CLOUD_RESOURCE_AWS_INSTANCE), CLOUD_RESOURCE_AWS_INSTANCE},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC]", ResourceType.AWS_INSTANCE,
                        List.of(CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC), CLOUD_RESOURCE_AWS_INSTANCE},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_VPC]", ResourceType.AWS_INSTANCE, List.of(CLOUD_RESOURCE_AWS_VPC), null},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC, CLOUD_RESOURCE_AWS_INSTANCE_2]", ResourceType.AWS_INSTANCE,
                        List.of(CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC, CLOUD_RESOURCE_AWS_INSTANCE_2), CLOUD_RESOURCE_AWS_INSTANCE},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getResourceTypeFromListDataProvider")
    void getResourceTypeFromListTest(String testCaseName, ResourceType type, List<CloudResource> resources, CloudResource resultExpected) {
        Optional<CloudResource> optionalCloudResource = underTest.getResourceTypeFromList(type, resources);

        if (resultExpected == null) {
            assertThat(optionalCloudResource).isEmpty();
        } else {
            assertThat(optionalCloudResource).hasValue(resultExpected);
        }
    }

}