package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(Parameterized.class)
public class AzureStackViewProviderTest {

    private static final String STACK_NAME = "Test Cluster";

    private static final String IMAGE_ID = "image-1";

    private static final String RESOURCE_GROUP = "resource group";

    private static final String NETWORK_ID = "network-1";

    private static final String INSTANCE_ID = "instance-1";

    private static final String GROUP_NAME = "group-1";

    private static final String IMAGE_NAME = "image-name";

    @Parameterized.Parameter
    public boolean marketplaceImage;

    @Parameterized.Parameter(1)
    public String imageId;

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

    @Before
    public void before() {
        initMocks(this);
        ReflectionTestUtils.setField(underTest, "stackNamePrefixLength", 255);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { true, null }, {false, "id" }
        });
    }

    @Test
    public void testGetAzureStackShouldReturnsANewAzureStackView() {
        CloudCredential cloudCredential = createCloudCredential();
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        AuthenticatedContext ac = new AuthenticatedContext(createCloudContext(), cloudCredential);
        Image imageModel = new Image(IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());

        Network network = mock(Network.class);
        AzureImage image = new AzureImage("id", "name", true);
        when(azureStorage.getCustomImage(any(), any(), any(), any())).thenReturn(image);
        List<Group> groups = createScaledGroups();
        when(cloudStack.getGroups()).thenReturn(groups);
        when(cloudStack.getParameters()).thenReturn(Collections.emptyMap());
        when(cloudStack.getNetwork()).thenReturn(network);
        when(cloudStack.getImage()).thenReturn(imageModel);
        when(network.getStringParameter("resourceGroupName")).thenReturn(RESOURCE_GROUP);
        when(network.getStringParameter("networkId")).thenReturn(NETWORK_ID);
        when(azureUtils.getCustomSubnetIds(network)).thenReturn(Collections.emptyList());
        when(azureImageFormatValidator.isMarketplaceImageFormat(imageModel)).thenReturn(marketplaceImage);

        AzureStackView actual = underTest.getAzureStack(azureCredentialView, cloudStack, client, ac);

        assertEquals("i1", actual.getGroups().get(InstanceGroupType.CORE.name()).get(0).getInstanceId());
        assertEquals(GROUP_NAME, actual.getInstanceGroups().get(0).getName());
        assertEquals(imageId, actual.getGroups().get(InstanceGroupType.CORE.name()).get(0).getCustomImageId());

    }

    private CloudCredential createCloudCredential() {
        return new CloudCredential("id", "name");
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
        return new CloudInstance(INSTANCE_ID, createInstanceTemplate(), null);
    }

    private InstanceTemplate createInstanceTemplate() {
        return new InstanceTemplate(null, INSTANCE_ID, 1L, Collections.emptyList(), null, Map.of("managedDisk", true), null,
                IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES);
    }

}