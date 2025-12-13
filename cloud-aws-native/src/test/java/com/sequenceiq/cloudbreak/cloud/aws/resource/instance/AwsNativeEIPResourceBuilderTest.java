package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsElasticIpService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.EIpAttributes;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;

@ExtendWith(MockitoExtension.class)
public class AwsNativeEIPResourceBuilderTest {

    @InjectMocks
    private AwsNativeEIPResourceBuilder underTest;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private AwsResourceNameService resourceNameService;

    @Spy
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsContext awsContext;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private Group group;

    @Mock
    private Image image;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private PersistenceRetriever persistenceRetriever;

    @Mock
    private AwsElasticIpService awsElasticIpService;

    @Mock
    private AwsMethodExecutor awsMethodExecutor;

    @Test
    public void testCreateWhenGatewayAndMapPublicIpOnLaunch() {
        String subnetId = "subnetId";
        String name = "name";
        String groupName = "groupName";
        long privateId = 0L;
        String resName = "resName";
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(group.getType()).thenReturn(InstanceGroupType.GATEWAY);
        when(group.getName()).thenReturn(groupName);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(cloudInstance.getSubnetId()).thenReturn(subnetId);
        when(awsNetworkService.isMapPublicOnLaunch(List.of(subnetId), amazonEc2Client)).thenReturn(true);
        when(cloudContext.getName()).thenReturn(name);
        when(resourceNameService.eip(name, groupName, privateId)).thenReturn(resName);

        List<CloudResource> actual = underTest.create(awsContext, cloudInstance, privateId, ac, group, image);

        assertEquals(1L, actual.size());
        assertEquals(resName, actual.get(0).getName());
        assertEquals(String.valueOf(privateId), actual.get(0).getReference());
    }

    @Test
    public void testCreateWhenGatewayAndNoMapPublicIpOnLaunch() {
        String subnetId = "subnetId";
        String name = "name";
        String groupName = "groupName";
        long privateId = 0L;
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(group.getType()).thenReturn(InstanceGroupType.GATEWAY);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(cloudInstance.getSubnetId()).thenReturn(subnetId);
        when(awsNetworkService.isMapPublicOnLaunch(List.of(subnetId), amazonEc2Client)).thenReturn(false);

        List<CloudResource> actual = underTest.create(awsContext, cloudInstance, privateId, ac, group, image);

        assertEquals(0L, actual.size());
        verify(awsNetworkService).isMapPublicOnLaunch(List.of(subnetId), amazonEc2Client);
        verify(resourceNameService, never()).eip(name, groupName, privateId);
    }

    @Test
    public void testCreateWhenCore() {
        String subnetId = "subnetId";
        String name = "name";
        String groupName = "groupName";
        long privateId = 0L;
        when(group.getType()).thenReturn(InstanceGroupType.CORE);

        List<CloudResource> actual = underTest.create(awsContext, cloudInstance, privateId, ac, group, image);

        assertEquals(0L, actual.size());
        verify(awsNetworkService, never()).isMapPublicOnLaunch(List.of(subnetId), amazonEc2Client);
        verify(resourceNameService, never()).eip(name, groupName, privateId);
    }

    @Test
    public void testBuildWhenNoBuildableResource() throws Exception {
        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, 0L, ac, group, Collections.emptyList(), cloudStack);
        assertEquals(0L, actual.size());
    }

    @Test
    public void testBuildWhenHasBuildableResourceAndInstanceResourceExists() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_RESERVED_IP)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();

        CloudResource instanceResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceId")
                .withParameters(emptyMap())
                .build();

        when(amazonEc2Client.allocateAddress(any())).thenReturn(AllocateAddressResponse.builder().allocationId("allocId").build());
        when(awsElasticIpService.associateElasticIpsToInstances(any(), any(), any()))
                .thenReturn(List.of(AssociateAddressResponse.builder().associationId("assocId").build()));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(0L);
        when(persistenceRetriever.notifyRetrieve(0L, "0", CommonStatus.CREATED, ResourceType.AWS_INSTANCE))
                .thenReturn(Optional.of(instanceResource));
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, 0L, ac, group, List.of(cloudResource), cloudStack);

        assertEquals(1L, actual.size());
        EIpAttributes eIpAttributes = actual.get(0).getParameter(CloudResource.ATTRIBUTES, EIpAttributes.class);
        assertEquals("allocId", eIpAttributes.getAllocateId());
        assertEquals("assocId", eIpAttributes.getAssociationId());
        ArgumentCaptor<AllocateAddressRequest> allocateAddressRequestArgumentCaptor = ArgumentCaptor.forClass(AllocateAddressRequest.class);
        verify(amazonEc2Client).allocateAddress(allocateAddressRequestArgumentCaptor.capture());
        AllocateAddressRequest allocateAddressRequest = allocateAddressRequestArgumentCaptor.getValue();
        assertThat(allocateAddressRequest.tagSpecifications().get(0)).matches(ts -> ts.tags().stream()
                .anyMatch(t -> "Name".equals(t.key()) && "name".equals(t.value())), "contains tag with Name=name");
    }

    @Test
    public void testBuildWhenExistingNameTagShouldNotOverride() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_RESERVED_IP)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();

        CloudResource instanceResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceId")
                .withParameters(emptyMap())
                .build();

        when(cloudStack.getTags()).thenReturn(Map.of("Name", "doNotOverride"));
        when(amazonEc2Client.allocateAddress(any())).thenReturn(AllocateAddressResponse.builder().allocationId("allocId").build());
        when(awsElasticIpService.associateElasticIpsToInstances(any(), any(), any()))
                .thenReturn(List.of(AssociateAddressResponse.builder().associationId("assocId").build()));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(0L);
        when(persistenceRetriever.notifyRetrieve(0L, "0", CommonStatus.CREATED, ResourceType.AWS_INSTANCE))
                .thenReturn(Optional.of(instanceResource));
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);

        underTest.build(awsContext, cloudInstance, 0L, ac, group, List.of(cloudResource), cloudStack);

        ArgumentCaptor<AllocateAddressRequest> allocateAddressRequestArgumentCaptor = ArgumentCaptor.forClass(AllocateAddressRequest.class);
        verify(amazonEc2Client).allocateAddress(allocateAddressRequestArgumentCaptor.capture());
        AllocateAddressRequest allocateAddressRequest = allocateAddressRequestArgumentCaptor.getValue();
        assertThat(allocateAddressRequest.tagSpecifications().get(0)).matches(ts -> ts.tags().stream()
                .anyMatch(t -> "Name".equals(t.key()) && "doNotOverride".equals(t.value())), "contains tag with Name=doNotOverride");
    }
}
