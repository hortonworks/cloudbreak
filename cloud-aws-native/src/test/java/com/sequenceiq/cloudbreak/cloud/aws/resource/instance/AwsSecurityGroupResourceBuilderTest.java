package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.DeleteSecurityGroupResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeModel;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeModelBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsModelService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AwsSecurityGroupResourceBuilderTest {

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
    private AwsNativeModelBuilder awsNativeModelBuilder;

    @Mock
    private AwsModelService awsModelService;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Security security;

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
    private CloudInstance cloudInstance;

    @Mock
    private ModelContext modelContext;

    @Mock
    private Location location;

    @Test
    public void testCreateWhenSecurityIdNull() {

        when(resourceNameService.resourceName(any(), any())).thenReturn("groupId");
        when(group.getSecurity()).thenReturn(security);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(security.getCloudSecurityId()).thenReturn(null);

        List<CloudResource> actual = underTest.create(awsContext, cloudInstance, 0, ac, group, image);
        Assertions.assertEquals(actual.get(0).getName(), "groupId");
    }

    @Test
    public void testCreateWhenSecurityIdNotNull() {
        when(group.getSecurity()).thenReturn(security);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(security.getCloudSecurityId()).thenReturn("sg-id");

        List<CloudResource> actual = underTest.create(awsContext, cloudInstance, 0, ac, group, image);
        Assertions.assertTrue(actual.isEmpty());
        verify(awsContext).putParameter(SecurityGroupBuilderUtil.SECURITY_GROUP_ID, "sg-id");
    }

    @Test
    public void testBuild() throws Exception {
        CloudResource resource = CloudResource.builder()
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();
        AwsNativeModel awsNativeModel = mock(AwsNativeModel.class);
        Map<String, String> resources = Map.of(SecurityGroupBuilderUtil.SECURITY_GROUP_ID, "groupId");

        when(awsModelService.buildDefaultModelContext(ac, cloudStack, null)).thenReturn(modelContext);
        when(awsNativeModelBuilder.build(modelContext)).thenReturn(awsNativeModel);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(securityGroupBuilderUtil.createSecurityGroup(any(), eq(group), eq(amazonEc2Client), eq(cloudContext), eq(awsNativeModel)))
                .thenReturn(resources);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, 0, ac, group, List.of(resource), cloudStack);
        Assertions.assertEquals("groupId", actual.get(0).getReference());
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

        CloudResource actual = underTest.delete(awsContext, ac, resource);

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

        CloudResource actual = underTest.delete(awsContext, ac, resource);

        Assertions.assertEquals(actual, resource);
    }
}
