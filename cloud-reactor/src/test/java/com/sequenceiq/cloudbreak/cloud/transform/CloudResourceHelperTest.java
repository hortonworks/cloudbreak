package com.sequenceiq.cloudbreak.cloud.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class CloudResourceHelperTest {

    private static final CloudResource CLOUD_RESOURCE_AWS_INSTANCE = createCloudResourceByType(ResourceType.AWS_INSTANCE);

    private static final CloudResource CLOUD_RESOURCE_AWS_INSTANCE_2 = createCloudResourceByType(ResourceType.AWS_INSTANCE);

    private static final CloudResource CLOUD_RESOURCE_AWS_VPC = createCloudResourceByType(ResourceType.AWS_VPC);

    @Mock
    private ResourceNotifier resourceNotifier;

    @InjectMocks
    private CloudResourceHelper underTest;

    @Captor
    private ArgumentCaptor<List<CloudResource>> listArgumentCaptor;

    private static CloudResource createCloudResourceByType(ResourceType resourceType) {
        return CloudResource.builder()
                .withType(resourceType)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
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

    static Object[][] getResourceTypeInstancesFromListDataProvider() {
        return new Object[][]{
                // testCaseName type resources resultExpected
                {"AWS_INSTANCE, []", ResourceType.AWS_INSTANCE, List.of(), List.of()},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_INSTANCE]", ResourceType.AWS_INSTANCE, List.of(CLOUD_RESOURCE_AWS_INSTANCE),
                        List.of(CLOUD_RESOURCE_AWS_INSTANCE)},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC]", ResourceType.AWS_INSTANCE,
                        List.of(CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC), List.of(CLOUD_RESOURCE_AWS_INSTANCE)},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_VPC]", ResourceType.AWS_INSTANCE, List.of(CLOUD_RESOURCE_AWS_VPC), List.of()},
                {"AWS_INSTANCE, [CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC, CLOUD_RESOURCE_AWS_INSTANCE_2]", ResourceType.AWS_INSTANCE,
                        List.of(CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_VPC, CLOUD_RESOURCE_AWS_INSTANCE_2),
                        List.of(CLOUD_RESOURCE_AWS_INSTANCE, CLOUD_RESOURCE_AWS_INSTANCE_2)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getResourceTypeInstancesFromListDataProvider")
    void getResourceTypeInstancesFromListTest(String testCaseName, ResourceType type, List<CloudResource> resources, List<CloudResource> resultExpected) {
        assertThat(underTest.getResourceTypeInstancesFromList(type, resources)).isEqualTo(resultExpected);
    }

    @Test
    void updateDeleteOnTerminationFlagButNoResourcesTest() {
        underTest.updateDeleteOnTerminationFlag(List.of(), false, mock(CloudContext.class));
        verifyNoInteractions(resourceNotifier);
    }

    @Test
    void updateDeleteOnTerminationFlagButAttributesTest() {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.hasParameter(CloudResource.ATTRIBUTES)).thenReturn(false);
        underTest.updateDeleteOnTerminationFlag(List.of(cloudResource), false, mock(CloudContext.class));
        verifyNoInteractions(resourceNotifier);
    }

    @Test
    void updateDeleteOnTerminationFlagAndHasVolumeSetTest() {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.hasParameter(CloudResource.ATTRIBUTES)).thenReturn(true);
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)).thenReturn(mock(VolumeSetAttributes.class));
        List<CloudResource> reattachableVolumeSets = List.of(cloudResource);
        underTest.updateDeleteOnTerminationFlag(reattachableVolumeSets, false, mock(CloudContext.class));
        verify(resourceNotifier, times(1)).notifyUpdates(listArgumentCaptor.capture(), any());
        assertThat(listArgumentCaptor.getValue()).containsExactly(cloudResource);
    }

}
