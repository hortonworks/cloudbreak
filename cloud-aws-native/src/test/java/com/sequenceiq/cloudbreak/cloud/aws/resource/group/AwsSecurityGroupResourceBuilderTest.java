package com.sequenceiq.cloudbreak.cloud.aws.resource.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupResponse;

@ExtendWith(MockitoExtension.class)
public class AwsSecurityGroupResourceBuilderTest {

    private static final String REGION_NAME = "regionName";

    private static final String AZ = "AZ";

    @InjectMocks
    private AwsSecurityGroupResourceBuilder underTest;

    @Mock
    private AwsContext awsContext;

    @Mock
    private AwsMethodExecutor awsMethodExecutor;

    @Mock
    private AwsResourceNameService resourceNameService;

    @Mock
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Security security;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private Group group;

    @Mock
    private Network network;

    @Mock
    private CloudContext cloudContext;

    @Test
    public void testCreateWhenSecurityIdNull() {
        when(awsContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME), AvailabilityZone.availabilityZone(AZ)));
        when(resourceNameService.securityGroup(any(), any(), anyLong())).thenReturn("groupId");
        when(group.getSecurity()).thenReturn(security);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(security.getCloudSecurityId()).thenReturn(null);

        CloudResource actual = underTest.create(awsContext, ac, group, network);
        assertEquals(actual.getName(), "groupId");
    }

    @Test
    public void testCreateWhenSecurityIdNotNull() {
        when(awsContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME), AvailabilityZone.availabilityZone(AZ)));
        when(group.getSecurity()).thenReturn(security);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(security.getCloudSecurityId()).thenReturn("sg-id");

        CloudResource actual = underTest.create(awsContext, ac, group, network);
        assertNull(actual);
    }

    @Test
    public void testBuild() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.AWS_SECURITY_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(Collections.emptyMap())
                .build();
        String groupId = "groupId";

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(securityGroupBuilderUtil.createSecurityGroup(any(), eq(group), eq(amazonEc2Client), eq(cloudContext), eq(ac)))
                .thenReturn(groupId);

        CloudResource actual = underTest.build(awsContext, ac, group, network, security, resource);
        assertEquals(groupId, actual.getReference());
    }

    @Test
    public void testDeleteWhenResultNull() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.AWS_SECURITY_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withReference("ref")
                .withParameters(Collections.emptyMap())
                .build();

        when(awsMethodExecutor.execute(any(), eq(null))).thenReturn(null);

        CloudResource actual = underTest.delete(awsContext, ac, resource, network);

        assertNull(actual);
    }

    @Test
    public void testDeleteWhenResultNotNull() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.AWS_SECURITY_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withReference("ref")
                .withParameters(Collections.emptyMap())
                .build();

        when(awsMethodExecutor.execute(any(), eq(null))).thenReturn(DeleteSecurityGroupResponse.builder().build());

        CloudResource actual = underTest.delete(awsContext, ac, resource, network);

        assertEquals(resource, actual);
    }

    @Test
    public void testDeleteWhenReferenceNull() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.AWS_SECURITY_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(Collections.emptyMap())
                .build();

        CloudResource actual = underTest.delete(awsContext, ac, resource, network);

        assertNull(actual);
    }
}
