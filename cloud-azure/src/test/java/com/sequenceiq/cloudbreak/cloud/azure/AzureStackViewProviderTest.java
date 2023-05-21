package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class AzureStackViewProviderTest {

    private static final String STACK_NAME = "Test Cluster";

    private static final String IMAGE_ID = "image-1";

    private static final String RESOURCE_GROUP = "resource group";

    private static final String NETWORK_ID = "network-1";

    private static final String INSTANCE_ID = "instance-1";

    private static final String GROUP_NAME = "group-1";

    private static final String IMAGE_NAME = "image-name";

    @InjectMocks
    private AzureStackViewProvider underTest;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private AzureClient client;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private Retry retryService;

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "stackNamePrefixLength", 255);
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true, null}, {false, "id"}
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testGetAzureStackShouldReturnsANewAzureStackView(boolean marketplaceImage, String imageId) {
        CloudCredential cloudCredential = createCloudCredential();
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        AuthenticatedContext ac = new AuthenticatedContext(createCloudContext(), cloudCredential);
        Image imageModel = new Image(IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());

        Network network = mock(Network.class);
        List<Group> groups = createScaledGroups();
        AzureImage image = new AzureImage("id", "name", true);
        lenient().when(azureStorage.getCustomImage(any(), any(), any(), any())).thenReturn(image);
        when(cloudStack.getGroups()).thenReturn(groups);
        when(cloudStack.getParameters()).thenReturn(Collections.emptyMap());
        when(cloudStack.getNetwork()).thenReturn(network);
        when(network.getStringParameter("resourceGroupName")).thenReturn(RESOURCE_GROUP);
        when(network.getStringParameter("networkId")).thenReturn(NETWORK_ID);
        when(azureUtils.getCustomSubnetIds(network)).thenReturn(Collections.emptyList());
        when(azureImageFormatValidator.isMarketplaceImageFormat(IMAGE_ID)).thenReturn(marketplaceImage);

        AzureStackView actual = underTest.getAzureStack(azureCredentialView, cloudStack, client, ac);

        assertEquals("i1-c4ca4238", actual.getInstancesByGroupType().get(InstanceGroupType.CORE.name()).get(0).getInstanceId());
        if (marketplaceImage) {
            assertNull(actual.getInstancesByGroupType().get(InstanceGroupType.CORE.name()).get(0).getCustomImageId());
        } else {
            assertEquals("id", actual.getInstancesByGroupType().get(InstanceGroupType.CORE.name()).get(0).getCustomImageId());
        }
        assertEquals(GROUP_NAME, actual.getInstanceGroups().get(0).getName());
        assertEquals(imageId, actual.getInstancesByGroupType().get(InstanceGroupType.CORE.name()).get(0).getCustomImageId());

    }

    private CloudCredential createCloudCredential() {
        return new CloudCredential("id", "name", "account");
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(Region.region("us-west-1"), AvailabilityZone.availabilityZone("us-west-1"));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withName(STACK_NAME)
                .withLocation(location)
                .build();
        return cloudContext;
    }

    private List<Group> createScaledGroups() {
        Group group = mock(Group.class);
        CloudInstance cloudInstance = createCloudInstance();
        when(group.getInstances()).thenReturn(List.of(cloudInstance));
        when(group.getType()).thenReturn(InstanceGroupType.CORE);
        when(group.getName()).thenReturn(GROUP_NAME);
        return Collections.singletonList(group);
    }

    private CloudInstance createCloudInstance() {
        return new CloudInstance(INSTANCE_ID, createInstanceTemplate(), null, "subnet-1", "az1",
                Collections.singletonMap(CloudInstance.ID, 1L));
    }

    private InstanceTemplate createInstanceTemplate() {
        return new InstanceTemplate(null, INSTANCE_ID, 1L, Collections.emptyList(), null, Map.of("managedDisk", true), null,
                IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }

}