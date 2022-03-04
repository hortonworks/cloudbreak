package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class AwsLoadBalancerMetadataCollectorTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String TARGET_GROUP_ARN = "arn:targetgroup";

    private static final String LOAD_BALANCER_ARN = "arn:loadbalancer";

    private static final String LISTENER_ARN = "arn:listener";

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @Mock
    private AmazonCloudFormationClient cfRetryClient;

    @InjectMocks
    private AwsLoadBalancerMetadataCollector underTest;

    @Test
    public void testCollectInternalLoadBalancer() {
        int numPorts = 1;
        AuthenticatedContext ac = authenticatedContext();
        LoadBalancer loadBalancer = createLoadBalancer();
        List<StackResourceSummary> summaries = createSummaries(numPorts, true);
        ListStackResourcesResult result = new ListStackResourcesResult();
        result.setStackResourceSummaries(summaries);

        Map<String, Object> expectedParameters = Map.of(
            AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LOAD_BALANCER_ARN,
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 0, LISTENER_ARN + "0Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 0, TARGET_GROUP_ARN + "0Internal"
        );

        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cfRetryClient);
        when(cloudFormationStackUtil.getCfStackName(ac)).thenReturn("stackName");
        when(awsStackRequestHelper.createListStackResourcesRequest(eq("stackName"))).thenReturn(new ListStackResourcesRequest());
        when(cfRetryClient.listStackResources(any())).thenReturn(result);

        Map<String, Object> parameters = underTest.getParameters(ac, loadBalancer, AwsLoadBalancerScheme.INTERNAL);

        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerMultiplePorts() {
        int numPorts = 3;
        AuthenticatedContext ac = authenticatedContext();
        LoadBalancer loadBalancer = createLoadBalancer();
        List<StackResourceSummary> summaries = createSummaries(numPorts, true);
        ListStackResourcesResult result = new ListStackResourcesResult();
        result.setStackResourceSummaries(summaries);

        Map<String, Object> expectedParameters = Map.of(
            AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LOAD_BALANCER_ARN,
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 0, LISTENER_ARN + "0Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 0, TARGET_GROUP_ARN + "0Internal",
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 1, LISTENER_ARN + "1Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 1, TARGET_GROUP_ARN + "1Internal",
            AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 2, LISTENER_ARN + "2Internal",
            AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 2, TARGET_GROUP_ARN + "2Internal"
        );

        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cfRetryClient);
        when(cloudFormationStackUtil.getCfStackName(ac)).thenReturn("stackName");
        when(awsStackRequestHelper.createListStackResourcesRequest(eq("stackName"))).thenReturn(new ListStackResourcesRequest());
        when(cfRetryClient.listStackResources(any())).thenReturn(result);

        Map<String, Object> parameters = underTest.getParameters(ac, loadBalancer, AwsLoadBalancerScheme.INTERNAL);

        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerMissingTargetGroup() {
        int numPorts = 1;
        AuthenticatedContext ac = authenticatedContext();
        LoadBalancer loadBalancer = createLoadBalancer();
        List<StackResourceSummary> summaries = createSummaries(numPorts, false);
        ListStackResourcesResult result = new ListStackResourcesResult();
        result.setStackResourceSummaries(summaries);

        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put(AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LOAD_BALANCER_ARN);
        expectedParameters.put(AwsLoadBalancerMetadataView.LISTENER_ARN_PREFIX + 0, LISTENER_ARN + "0Internal");
        expectedParameters.put(AwsLoadBalancerMetadataView.TARGET_GROUP_ARN_PREFIX + 0, null);

        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cfRetryClient);
        when(cloudFormationStackUtil.getCfStackName(ac)).thenReturn("stackName");
        when(awsStackRequestHelper.createListStackResourcesRequest(eq("stackName"))).thenReturn(new ListStackResourcesRequest());
        when(cfRetryClient.listStackResources(any())).thenReturn(result);

        Map<String, Object> parameters = underTest.getParameters(ac, loadBalancer, AwsLoadBalancerScheme.INTERNAL);

        assertEquals(expectedParameters, parameters);
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext context = CloudContext.Builder.builder()
            .withId(5L)
            .withName("name")
            .withCrn("crn")
            .withPlatform("platform")
            .withVariant("variant")
            .withLocation(location)
            .withWorkspaceId(WORKSPACE_ID)
            .build();
        CloudCredential credential = new CloudCredential("crn", null, null, "acc", false);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);
        authenticatedContext.putParameter(AmazonEc2Client.class, amazonEC2Client);
        return authenticatedContext;
    }

    private LoadBalancer createLoadBalancer() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setLoadBalancerArn(LOAD_BALANCER_ARN);
        return loadBalancer;
    }

    private List<StackResourceSummary> createSummaries(int numPorts, boolean includeTargetGroup) {
        List<StackResourceSummary> summaries = new ArrayList<>();
        for (AwsLoadBalancerScheme scheme : AwsLoadBalancerScheme.class.getEnumConstants()) {
            StackResourceSummary lbSummary = new StackResourceSummary();
            lbSummary.setLogicalResourceId(AwsLoadBalancer.getLoadBalancerName(scheme));
            lbSummary.setPhysicalResourceId(LOAD_BALANCER_ARN + scheme.resourceName());
            summaries.add(lbSummary);
            for (int i = 0; i < numPorts; i++) {
                if (includeTargetGroup) {
                    StackResourceSummary tgSummary = new StackResourceSummary();
                    tgSummary.setLogicalResourceId(AwsTargetGroup.getTargetGroupName(i, scheme));
                    tgSummary.setPhysicalResourceId(TARGET_GROUP_ARN + i + scheme.resourceName());
                    summaries.add(tgSummary);
                }
                StackResourceSummary lSummary = new StackResourceSummary();
                lSummary.setLogicalResourceId(AwsListener.getListenerName(i, scheme));
                lSummary.setPhysicalResourceId(LISTENER_ARN + i + scheme.resourceName());
                summaries.add(lSummary);
            }
        }
        return summaries;
    }
}
