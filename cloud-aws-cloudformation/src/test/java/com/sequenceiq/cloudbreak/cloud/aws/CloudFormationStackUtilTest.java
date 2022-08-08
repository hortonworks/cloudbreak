package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class CloudFormationStackUtilTest {

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AmazonCloudFormationClient cfClient;

    @Mock
    private AmazonElasticLoadBalancingClient elbClient;

    @InjectMocks
    private CloudFormationStackUtil underTest;

    @Mock
    private Group gatewayGroup;

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() throws Exception {

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(12L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("default")
                .build();
        authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());

        //init real default value
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 200);
        when(loadBalancerTypeConverter.convert((LoadBalancerType) any())).thenCallRealMethod();


    }

    @Test
    void testAddLoadBalancerDoNotAddComputeGroup() {
        when(gatewayGroup.getName()).thenReturn("gateway");

        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(elbClient);

        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cfClient);

        DescribeStackResourceResult resourcesResult = new DescribeStackResourceResult()
                .withStackResourceDetail(new StackResourceDetail().withPhysicalResourceId("targetGroupArn"));
        when(cfClient.describeStackResource(any(DescribeStackResourceRequest.class))).thenReturn(resourcesResult);

        TargetHealthDescription targetHealthDescription = new TargetHealthDescription();
        targetHealthDescription.withTarget(new TargetDescription().withId("i-gatewayId"));
        DescribeTargetHealthResult describeTargetHealthResult = new DescribeTargetHealthResult();
        describeTargetHealthResult.withTargetHealthDescriptions(List.of(targetHealthDescription));
        when(elbClient.describeTargetHealth(any(DescribeTargetHealthRequest.class))).thenReturn(describeTargetHealthResult);
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 443);
        cloudLoadBalancer.addPortToTargetGroupMapping(targetGroupPortPair, Set.of(gatewayGroup));

        //compute node
        CloudResource cloudResource = new CloudResource.Builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("i-029627fe1ce09b970")
                .withGroup("compute")
                .withStackAware(true)
                .withInstanceId("i-029627fe1ce09b970")
                .withAvailabilityZone("us-west-2")
                .withParams(Map.of("privateId", "9", "instanceType", "r5d.4xlarge"))
                .build();
        underTest.addLoadBalancerTargets(authenticatedContext, cloudLoadBalancer, List.of(cloudResource));


        verify(elbClient, times(0)).registerTargets(any());
    }

    @Test
    void testAddLoadBalancerOnlyAddsGatewayGroup() {
        when(gatewayGroup.getName()).thenReturn("gateway");

        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(elbClient);

        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cfClient);

        DescribeStackResourceResult resourcesResult = new DescribeStackResourceResult()
                .withStackResourceDetail(new StackResourceDetail().withPhysicalResourceId("targetGroupArn"));
        when(cfClient.describeStackResource(any(DescribeStackResourceRequest.class))).thenReturn(resourcesResult);

        TargetHealthDescription targetHealthDescription = new TargetHealthDescription();
        targetHealthDescription.withTarget(new TargetDescription().withId("i-gatewayId"));
        DescribeTargetHealthResult describeTargetHealthResult = new DescribeTargetHealthResult();
        describeTargetHealthResult.withTargetHealthDescriptions(List.of(targetHealthDescription));
        when(elbClient.describeTargetHealth(any(DescribeTargetHealthRequest.class))).thenReturn(describeTargetHealthResult);
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 443);
        cloudLoadBalancer.addPortToTargetGroupMapping(targetGroupPortPair, Set.of(gatewayGroup));

        //compute node
        CloudResource cloudResource = new CloudResource.Builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("i-029627fe1ce09b970")
                .withGroup("gateway")
                .withStackAware(true)
                .withInstanceId("i-029627fe1ce09b970")
                .withAvailabilityZone("us-west-2")
                .withParams(Map.of("privateId", "9", "instanceType", "r5d.4xlarge"))
                .build();
        underTest.addLoadBalancerTargets(authenticatedContext, cloudLoadBalancer, List.of(cloudResource));

        ArgumentCaptor<RegisterTargetsRequest> captor = ArgumentCaptor.forClass(RegisterTargetsRequest.class);

        verify(elbClient, times(1)).registerTargets(captor.capture());
        List<TargetDescription> targets = captor.getValue().getTargets();
        Assertions.assertEquals(cloudResource.getInstanceId(), targets.get(0).getId());

    }
}
