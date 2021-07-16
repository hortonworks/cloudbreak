package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.service.LoadBalancerConfigConverter.MISSING_CLOUD_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.loadbalancer.AwsLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.loadbalancer.AwsTargetGroupMetadata;
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
        AwsLoadBalancerMetadata loadBalancerMetadata = new AwsLoadBalancerMetadata();
        loadBalancerMetadata.setArn(LB_ARN);

        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = underTest.convertLoadBalancer(AWS, loadBalancerMetadata);
        assertNotNull(cloudLoadBalancerConfigDbWrapper.getAwsConfig());
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = cloudLoadBalancerConfigDbWrapper.getAwsConfig();
        assertEquals(LB_ARN, awsLoadBalancerConfigDb.getArn());
    }

    @Test
    public void testConvertAwsTargetGroup() {
        AwsTargetGroupMetadata targetGroupMetadata = new AwsTargetGroupMetadata();
        targetGroupMetadata.setPort(PORT1);
        targetGroupMetadata.setListenerArn(LISTENER_ARN);
        targetGroupMetadata.setTargetGroupArn(TARGET_ARN);
        AwsLoadBalancerMetadata loadBalancerMetadata = new AwsLoadBalancerMetadata();
        loadBalancerMetadata.setTargetGroupMetadata(List.of(targetGroupMetadata));
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair = new TargetGroupPortPair(PORT1, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AWS, loadBalancerMetadata, targetGroup);
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
        AwsTargetGroupMetadata targetGroupMetadata1 = new AwsTargetGroupMetadata();
        targetGroupMetadata1.setPort(PORT1);
        targetGroupMetadata1.setListenerArn(LISTENER_ARN);
        targetGroupMetadata1.setTargetGroupArn(TARGET_ARN);
        AwsTargetGroupMetadata targetGroupMetadata2 = new AwsTargetGroupMetadata();
        targetGroupMetadata2.setPort(PORT2);
        targetGroupMetadata2.setListenerArn(LISTENER_ARN);
        targetGroupMetadata2.setTargetGroupArn(TARGET_ARN);
        AwsTargetGroupMetadata targetGroupMetadata3 = new AwsTargetGroupMetadata();
        targetGroupMetadata3.setPort(PORT3);
        targetGroupMetadata3.setListenerArn(LISTENER_ARN);
        targetGroupMetadata3.setTargetGroupArn(TARGET_ARN);
        AwsLoadBalancerMetadata loadBalancerMetadata = new AwsLoadBalancerMetadata();
        loadBalancerMetadata.setTargetGroupMetadata(List.of(targetGroupMetadata1, targetGroupMetadata2, targetGroupMetadata3));
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair1 = new TargetGroupPortPair(PORT1, PORT1);
        TargetGroupPortPair portPair2 = new TargetGroupPortPair(PORT2, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair1, portPair2));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AWS, loadBalancerMetadata, targetGroup);
        assertNotNull(targetGroupConfigDbWrapper.getAwsConfig());
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = targetGroupConfigDbWrapper.getAwsConfig();
        assertEquals(2, awsTargetGroupConfigDb.getPortArnMapping().size());
        assertEquals(Set.of(PORT1, PORT2), awsTargetGroupConfigDb.getPortArnMapping().keySet());
    }

    @Test
    public void testConvertAwsTargetGroupMissingPortInMetadata() {
        AwsTargetGroupMetadata targetGroupMetadata1 = new AwsTargetGroupMetadata();
        targetGroupMetadata1.setPort(PORT1);
        targetGroupMetadata1.setListenerArn(LISTENER_ARN);
        targetGroupMetadata1.setTargetGroupArn(TARGET_ARN);
        AwsLoadBalancerMetadata loadBalancerMetadata = new AwsLoadBalancerMetadata();
        loadBalancerMetadata.setTargetGroupMetadata(List.of(targetGroupMetadata1));
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        TargetGroupPortPair portPair1 = new TargetGroupPortPair(PORT1, PORT1);
        TargetGroupPortPair portPair2 = new TargetGroupPortPair(PORT2, PORT2);

        when(loadBalancerConfigService.getTargetGroupPortPairs(eq(targetGroup))).thenReturn(Set.of(portPair1, portPair2));

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = underTest.convertTargetGroup(AWS, loadBalancerMetadata, targetGroup);
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
}
