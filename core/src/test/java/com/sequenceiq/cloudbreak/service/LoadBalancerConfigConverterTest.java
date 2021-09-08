package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.service.LoadBalancerConfigConverter.MISSING_CLOUD_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpLoadBalancerNamesDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpTargetGroupConfigDb;
import com.sequenceiq.common.api.type.TargetGroupType;

public class LoadBalancerConfigConverterTest {

    private static final String LB_ARN = "arn://loadbalancer";

    private static final String TARGET_ARN = "arn://targetgroup";

    private static final String LISTENER_ARN = "arn://listener";

    private static final Integer PORT1 = 443;

    private static final Integer PORT2 = 444;

    private static final Integer PORT3 = 445;

    private static final String LB_NAME = "load-balancer-name";

    private static final String AVAILABILITY_SET_NAME = "availability-set";

    private static final String INSTANCE_GROUP_NAME = "instace-group-name";

    private static final String BACKEND_SERVICE_NAME = "backend-service-name";

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @InjectMocks
    private LoadBalancerConfigConverter underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertAwsLoadBalancer() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
            .withParameters(createAwsParams(0))
            .build();

        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = underTest.convertLoadBalancer(AWS, cloudLoadBalancerMetadata);
        assertNotNull(cloudLoadBalancerConfigDbWrapper.getAwsConfig());
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = cloudLoadBalancerConfigDbWrapper.getAwsConfig();
        assertEquals(LB_ARN, awsLoadBalancerConfigDb.getArn());
    }

    @Test
    public void testConvertAwsTargetGroup() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
            .withParameters(createAwsParams(1))
            .build();
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair = new TargetGroupPortPair(PORT1, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AWS, cloudLoadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getAwsConfig());
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = targetGroupConfigDbWrapper.getAwsConfig();
        assertEquals(1, awsTargetGroupConfigDb.getPortArnMapping().size());
        assertEquals(PORT1, awsTargetGroupConfigDb.getPortArnMapping().keySet().iterator().next());
        AwsTargetGroupArnsDb targetGroupArns = awsTargetGroupConfigDb.getPortArnMapping().get(PORT1);
        assertEquals(LISTENER_ARN, targetGroupArns.getListenerArn());
        assertEquals(TARGET_ARN, targetGroupArns.getTargetGroupArn());
    }

    @Test
    public void testConvertAwsTargetGroupExtraPortsInMetadata() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
            .withParameters(createAwsParams(3))
            .build();
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair1 = new TargetGroupPortPair(PORT1, PORT1);
        TargetGroupPortPair portPair2 = new TargetGroupPortPair(PORT2, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair1, portPair2));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AWS, cloudLoadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getAwsConfig());
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = targetGroupConfigDbWrapper.getAwsConfig();
        assertEquals(2, awsTargetGroupConfigDb.getPortArnMapping().size());
        assertEquals(Set.of(PORT1, PORT2), awsTargetGroupConfigDb.getPortArnMapping().keySet());
    }

    @Test
    public void testConvertAwsTargetGroupMissingPortInMetadata() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
            .withParameters(createAwsParams(1))
            .build();
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair1 = new TargetGroupPortPair(PORT1, PORT1);
        TargetGroupPortPair portPair2 = new TargetGroupPortPair(PORT2, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair1, portPair2));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AWS, cloudLoadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getAwsConfig());
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = targetGroupConfigDbWrapper.getAwsConfig();
        assertEquals(2, awsTargetGroupConfigDb.getPortArnMapping().size());
        assertEquals(Set.of(PORT1, PORT2), awsTargetGroupConfigDb.getPortArnMapping().keySet());
        AwsTargetGroupArnsDb targetGroupArns = awsTargetGroupConfigDb.getPortArnMapping().get(PORT1);
        assertEquals(LISTENER_ARN, targetGroupArns.getListenerArn());
        assertEquals(TARGET_ARN, targetGroupArns.getTargetGroupArn());
        targetGroupArns = awsTargetGroupConfigDb.getPortArnMapping().get(PORT2);
        assertEquals(MISSING_CLOUD_RESOURCE, targetGroupArns.getListenerArn());
        assertEquals(MISSING_CLOUD_RESOURCE, targetGroupArns.getTargetGroupArn());
    }

    @Test
    public void testConvertAzureLoadBalancer() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                .withParameters(createAzureParams(0))
                .build();

        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = underTest.convertLoadBalancer(AZURE, cloudLoadBalancerMetadata);
        assertNotNull(cloudLoadBalancerConfigDbWrapper.getAzureConfig());
        AzureLoadBalancerConfigDb azureLoadBalancerConfigDb = cloudLoadBalancerConfigDbWrapper.getAzureConfig();
        assertEquals(LB_NAME, azureLoadBalancerConfigDb.getName());
    }

    @Test
    public void testConvertAzureTargetGroup() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                .withParameters(createAzureParams(1))
                .build();
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair = new TargetGroupPortPair(PORT1, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AZURE, cloudLoadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getAzureConfig());
        AzureTargetGroupConfigDb azureTargetGroupConfigDb = targetGroupConfigDbWrapper.getAzureConfig();
        assertEquals(1, azureTargetGroupConfigDb.getPortAvailabilitySetMapping().size());
        assertEquals(PORT1, azureTargetGroupConfigDb.getPortAvailabilitySetMapping().keySet().iterator().next());
        List<String> availabilitySets = azureTargetGroupConfigDb.getPortAvailabilitySetMapping().get(PORT1);
        assertEquals(1, availabilitySets.size());
        assertEquals(AVAILABILITY_SET_NAME, availabilitySets.get(0));
    }

    @Test
    public void testConvertAzureTargetGroupMissingAvailabilitySetInMetadata() {
        Map<String, Object> azureParams = createAzureParams(1);
        azureParams.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(PORT2), null);
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                .withParameters(azureParams)
                .build();
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair1 = new TargetGroupPortPair(PORT1, PORT2);
        TargetGroupPortPair portPair2 = new TargetGroupPortPair(PORT2, PORT2);
        TargetGroupPortPair portPair3 = new TargetGroupPortPair(PORT3, PORT3);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair1, portPair2, portPair3));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AZURE, cloudLoadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getAzureConfig());
        AzureTargetGroupConfigDb azureTargetGroupConfigDb = targetGroupConfigDbWrapper.getAzureConfig();
        assertEquals(Set.of(PORT1, PORT2, PORT3), azureTargetGroupConfigDb.getPortAvailabilitySetMapping().keySet());
        List<String> availabilitySets = azureTargetGroupConfigDb.getPortAvailabilitySetMapping().get(PORT1);
        assertEquals(List.of(AVAILABILITY_SET_NAME), availabilitySets);
        availabilitySets = azureTargetGroupConfigDb.getPortAvailabilitySetMapping().get(PORT2);
        assertEquals(List.of(MISSING_CLOUD_RESOURCE), availabilitySets);
        availabilitySets = azureTargetGroupConfigDb.getPortAvailabilitySetMapping().get(PORT3);
        assertEquals(List.of(MISSING_CLOUD_RESOURCE), availabilitySets);
    }

    @Test
    public void testConvertGcpLoadBalancer() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                .withParameters(creatGcpParams(0))
                .build();
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = underTest.convertLoadBalancer(GCP, cloudLoadBalancerMetadata);
        assertNotNull(cloudLoadBalancerConfigDbWrapper.getGcpConfig());
        GcpLoadBalancerConfigDb gcpLoadBalancerConfigDb = cloudLoadBalancerConfigDbWrapper.getGcpConfig();
        assertEquals(LB_NAME, gcpLoadBalancerConfigDb.getName());

    }

    @Test
    public void testConvertGcpTargetGroup() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                .withParameters(creatGcpParams(1))
                .build();

        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair = new TargetGroupPortPair(PORT1, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(GCP, cloudLoadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getGcpConfig());
        GcpTargetGroupConfigDb gcpTargetGroupConfigDb = targetGroupConfigDbWrapper.getGcpConfig();

        assertEquals(1, gcpTargetGroupConfigDb.getPortMapping().size());
        assertEquals(PORT1, gcpTargetGroupConfigDb.getPortMapping().keySet().iterator().next());
        GcpLoadBalancerNamesDb gcpLoadBalancerNamesDb = gcpTargetGroupConfigDb.getPortMapping().get(PORT1);
        assertEquals(BACKEND_SERVICE_NAME, gcpLoadBalancerNamesDb.getBackendServiceName());
        assertEquals(INSTANCE_GROUP_NAME, gcpLoadBalancerNamesDb.getInstanceGroupName());
    }

    private Map<String, Object> createAwsParams(int numPorts) {
        Map<String, Object> params = new HashMap<>();
        params.put(AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LB_ARN);
        for (int i = 0; i < numPorts; i++) {
            int port = PORT1 + i;
            params.put(AwsLoadBalancerMetadataView.getListenerParam(port), LISTENER_ARN);
            params.put(AwsLoadBalancerMetadataView.getTargetGroupParam(port), TARGET_ARN);
        }
        return params;
    }

    private Map<String, Object> createAzureParams(int numPorts) {
        Map<String, Object> params = new HashMap<>();
        params.put(AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME);
        for (int i = 0; i < numPorts; i++) {
            int port = PORT1 + i;
            params.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(port), AVAILABILITY_SET_NAME);
        }
        return params;
    }

    private Map<String, Object> creatGcpParams(int numPorts) {
        Map<String, Object> params = new HashMap<>();
        params.put(GcpLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME);
        for (int i = 0; i < numPorts; i++) {
            int port = PORT1 + i;
            params.put(GcpLoadBalancerMetadataView.getBackendServiceParam(port), BACKEND_SERVICE_NAME);
            params.put(GcpLoadBalancerMetadataView.getInstanceGroupParam(port), INSTANCE_GROUP_NAME);
        }
        return params;
    }
}
