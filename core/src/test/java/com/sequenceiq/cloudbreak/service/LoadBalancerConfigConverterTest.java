package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.service.LoadBalancerConfigConverter.MISSING_CLOUD_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.common.api.type.TargetGroupType;

public class LoadBalancerConfigConverterTest {

    private static final String LB_ARN = "arn://loadbalancer";

    private static final String TARGET_ARN = "arn://targetgroup";

    private static final String LISTENER_ARN = "arn://listener";

    private static final Integer PORT1 = 443;

    private static final Integer PORT2 = 444;

    private static final Integer PORT3 = 445;

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
            .withParameters(createParams(0))
            .build();

        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = underTest.convertLoadBalancer(AWS, cloudLoadBalancerMetadata);
        assertNotNull(cloudLoadBalancerConfigDbWrapper.getAwsConfig());
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = cloudLoadBalancerConfigDbWrapper.getAwsConfig();
        assertEquals(LB_ARN, awsLoadBalancerConfigDb.getArn());
    }

    @Test
    public void testConvertAwsTargetGroup() {
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
            .withParameters(createParams(1))
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
            .withParameters(createParams(3))
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
            .withParameters(createParams(1))
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

    private Map<String, Object> createParams(int numPorts) {
        Map<String, Object> params = new HashMap<>();
        params.put(AwsLoadBalancerMetadataView.LOADBALANCER_ARN, LB_ARN);
        for (int i = 0; i < numPorts; i++) {
            int port = PORT1 + i;
            params.put(AwsLoadBalancerMetadataView.getListenerParam(port), LISTENER_ARN);
            params.put(AwsLoadBalancerMetadataView.getTargetGroupParam(port), TARGET_ARN);
        }
        return params;
    }
}
