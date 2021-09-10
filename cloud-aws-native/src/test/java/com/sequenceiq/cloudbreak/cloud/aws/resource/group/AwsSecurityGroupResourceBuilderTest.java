package com.sequenceiq.cloudbreak.cloud.aws.resource.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.DeleteSecurityGroupResult;
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
        when(resourceNameService.resourceName(any(), any())).thenReturn("groupId");
        when(group.getSecurity()).thenReturn(security);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(security.getCloudSecurityId()).thenReturn(null);

        CloudResource actual = underTest.create(awsContext, ac, group, network);
        Assertions.assertEquals(actual.getName(), "groupId");
    }

    @Test
    public void testCreateWhenSecurityIdNotNull() {
        when(awsContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME), AvailabilityZone.availabilityZone(AZ)));
        when(group.getSecurity()).thenReturn(security);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(security.getCloudSecurityId()).thenReturn("sg-id");

        CloudResource actual = underTest.create(awsContext, ac, group, network);
        Assertions.assertNull(actual);
    }

    @Test
    public void testBuild() throws Exception {
        CloudResource resource = CloudResource.builder()
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();
        String groupId = "groupId";

        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(securityGroupBuilderUtil.createSecurityGroup(any(), eq(group), eq(amazonEc2Client), eq(cloudContext), eq(ac)))
                .thenReturn(groupId);

        CloudResource actual = underTest.build(awsContext, ac, group, network, security, resource);
        Assertions.assertEquals(groupId, actual.getReference());
    }

    @Test
    public void testDeleteWhenResultNull() throws Exception {
        CloudResource resource = CloudResource.builder()
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();

        when(awsMethodExecutor.execute(any(), eq(null))).thenReturn(null);

        CloudResource actual = underTest.delete(awsContext, ac, resource, network);

        Assertions.assertNull(actual);
    }

    @Test
    public void testDeleteWhenResultNotNull() throws Exception {
        CloudResource resource = CloudResource.builder()
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();

        when(awsMethodExecutor.execute(any(), eq(null))).thenReturn(new DeleteSecurityGroupResult());

        CloudResource actual = underTest.delete(awsContext, ac, resource, network);

        Assertions.assertEquals(actual, resource);
    }
}
