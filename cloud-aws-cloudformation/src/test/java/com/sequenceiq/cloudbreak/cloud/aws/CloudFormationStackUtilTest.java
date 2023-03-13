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

import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResourceDetail;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;

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

        DescribeStackResourceResponse resourcesResponse = DescribeStackResourceResponse.builder()
                .stackResourceDetail(StackResourceDetail.builder().physicalResourceId("targetGroupArn").build())
                .build();
        when(cfClient.describeStackResource(any(DescribeStackResourceRequest.class))).thenReturn(resourcesResponse);

        TargetHealthDescription targetHealthDescription = TargetHealthDescription.builder()
                .target(TargetDescription.builder().id("i-gatewayId").build())
                .build();
        DescribeTargetHealthResponse describeTargetHealthResponse = DescribeTargetHealthResponse.builder()
                .targetHealthDescriptions(List.of(targetHealthDescription))
                .build();
        when(elbClient.describeTargetHealth(any(DescribeTargetHealthRequest.class))).thenReturn(describeTargetHealthResponse);
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 443);
        cloudLoadBalancer.addPortToTargetGroupMapping(targetGroupPortPair, Set.of(gatewayGroup));

        //compute node
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("i-029627fe1ce09b970")
                .withGroup("compute")
                .withStackAware(true)
                .withInstanceId("i-029627fe1ce09b970")
                .withAvailabilityZone("us-west-2")
                .withParameters(Map.of("privateId", "9", "instanceType", "r5d.4xlarge"))
                .build();
        underTest.addLoadBalancerTargets(authenticatedContext, cloudLoadBalancer, List.of(cloudResource));

        verify(elbClient, times(0)).registerTargets(any());
    }

    @Test
    void testAddLoadBalancerOnlyAddsGatewayGroup() {
        when(gatewayGroup.getName()).thenReturn("gateway");

        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(elbClient);

        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cfClient);

        DescribeStackResourceResponse resourcesResponse = DescribeStackResourceResponse.builder()
                .stackResourceDetail(StackResourceDetail.builder().physicalResourceId("targetGroupArn").build())
                .build();
        when(cfClient.describeStackResource(any(DescribeStackResourceRequest.class))).thenReturn(resourcesResponse);

        TargetHealthDescription targetHealthDescription = TargetHealthDescription.builder()
                .target(TargetDescription.builder().id("i-gatewayId").build())
                .build();
        DescribeTargetHealthResponse describeTargetHealthResponse = DescribeTargetHealthResponse.builder()
                .targetHealthDescriptions(List.of(targetHealthDescription))
                .build();
        when(elbClient.describeTargetHealth(any(DescribeTargetHealthRequest.class))).thenReturn(describeTargetHealthResponse);
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(443, 443);
        cloudLoadBalancer.addPortToTargetGroupMapping(targetGroupPortPair, Set.of(gatewayGroup));

        //compute node
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("i-029627fe1ce09b970")
                .withGroup("gateway")
                .withStackAware(true)
                .withInstanceId("i-029627fe1ce09b970")
                .withAvailabilityZone("us-west-2")
                .withParameters(Map.of("privateId", "9", "instanceType", "r5d.4xlarge"))
                .build();
        underTest.addLoadBalancerTargets(authenticatedContext, cloudLoadBalancer, List.of(cloudResource));

        ArgumentCaptor<RegisterTargetsRequest> captor = ArgumentCaptor.forClass(RegisterTargetsRequest.class);

        verify(elbClient, times(1)).registerTargets(captor.capture());
        List<TargetDescription> targets = captor.getValue().targets();
        Assertions.assertEquals(cloudResource.getInstanceId(), targets.get(0).id());
    }
}
