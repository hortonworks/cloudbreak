package com.sequenceiq.cloudbreak.converter.stack.loadbalancer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.AwsTargetGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.AzureTargetGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.TargetGroupResponse;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class LoadBalancerToLoadBalancerResponseConverterTest extends AbstractEntityConverterTest<LoadBalancer> {

    private static final String LB_FQDN = "loadbalancer.domain.name";

    private static final String LB_IP = "0.0.0.0";

    private static final String LB_DNS = "loadbalancer.dns";

    private static final int PORT = 443;

    private static final String INSTANCE_ID = "instanceId";

    private static final String LB_ARN = "arn:loadbalancer";

    private static final String TG_ARN = "arn:targetGroup";

    private static final String LISTENER_ARN = "arn:listener";

    private static final String AZURE_LB_NAME = "load-balancer-name";

    private static final String AZURE_AS_NAME = "availability-set-name";

    @Mock
    private TargetGroupPersistenceService targetGroupService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @InjectMocks
    private LoadBalancerToLoadBalancerResponseConverter underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(instanceGroupService.findByTargetGroupId(any())).thenReturn(Set.of(new InstanceGroup()));
        when(instanceMetaDataService.findAliveInstancesInInstanceGroup(any())).thenReturn(ceateInstanceMetadata());
        when(loadBalancerConfigService.getTargetGroupPortPairs(any())).thenReturn(Set.of(new TargetGroupPortPair(PORT, PORT)));
    }

    @Test
    public void testNoSavedCloudConfig() {
        LoadBalancer source = getSource();
        // GIVEN
        given(targetGroupService.findByLoadBalancerId(any())).willReturn(createAwsTargetGroups());
        // WHEN
        LoadBalancerResponse response = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(response, List.of("awsResourceId", "azureResourceId"));
        assertNull(response.getAwsResourceId());
        assertNull(response.getAzureResourceId());
    }

    @Test
    public void testConvertAws() {
        LoadBalancer source = getSource();
        // GIVEN
        getSource().setProviderConfig(createAwsLoadBalancerConfig());
        given(targetGroupService.findByLoadBalancerId(any())).willReturn(createAwsTargetGroups());
        // WHEN
        LoadBalancerResponse response = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(response, List.of("azureResourceId"));
        assertEquals(LB_ARN, response.getAwsResourceId().getArn());
        assertEquals(1, response.getTargets().size());
        TargetGroupResponse targetGroupResponse = response.getTargets().get(0);
        assertEquals(PORT, targetGroupResponse.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroupResponse.getTargetInstances());
        AwsTargetGroupResponse awsTargetGroupResponse = targetGroupResponse.getAwsResourceIds();
        assertNotNull(awsTargetGroupResponse);
        assertEquals(LISTENER_ARN, awsTargetGroupResponse.getListenerArn());
        assertEquals(TG_ARN, awsTargetGroupResponse.getTargetGroupArn());
    }

    @Test
    public void testConvertAzure() {
        LoadBalancer source = getSource();
        // GIVEN
        getSource().setProviderConfig(createAzureLoadBalancerConfig());
        given(targetGroupService.findByLoadBalancerId(any())).willReturn(createAzureTargetGroups());
        // WHEN
        LoadBalancerResponse response = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(response, List.of("awsResourceId"));
        assertEquals(AZURE_LB_NAME, response.getAzureResourceId().getName());
        assertEquals(1, response.getTargets().size());
        TargetGroupResponse targetGroupResponse = response.getTargets().get(0);
        assertEquals(PORT, targetGroupResponse.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroupResponse.getTargetInstances());
        AzureTargetGroupResponse azureTargetGroupResponse = targetGroupResponse.getAzureResourceId();
        assertNotNull(azureTargetGroupResponse);
        assertEquals(List.of(AZURE_AS_NAME), azureTargetGroupResponse.getAvailabilitySet());
    }

    @Override
    public LoadBalancer createSource() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PRIVATE);
        loadBalancer.setFqdn(LB_FQDN);
        loadBalancer.setIp(LB_IP);
        loadBalancer.setDns(LB_DNS);
        return loadBalancer;
    }

    private LoadBalancerConfigDbWrapper createAwsLoadBalancerConfig() {
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = new AwsLoadBalancerConfigDb();
        awsLoadBalancerConfigDb.setArn(LB_ARN);
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        cloudLoadBalancerConfigDbWrapper.setAwsConfig(awsLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    private Set<TargetGroup> createAwsTargetGroups() {
        AwsTargetGroupArnsDb awsTargetGroupArnsDb = new AwsTargetGroupArnsDb();
        awsTargetGroupArnsDb.setListenerArn(LISTENER_ARN);
        awsTargetGroupArnsDb.setTargetGroupArn(TG_ARN);
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = new AwsTargetGroupConfigDb();
        awsTargetGroupConfigDb.setPortArnMapping(Map.of(PORT, awsTargetGroupArnsDb));
        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        targetGroupConfigDbWrapper.setAwsConfig(awsTargetGroupConfigDb);
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setProviderConfig(targetGroupConfigDbWrapper);
        return Set.of(targetGroup);
    }

    private List<InstanceMetaData> ceateInstanceMetadata() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID);
        return List.of(instanceMetaData);
    }

    private LoadBalancerConfigDbWrapper createAzureLoadBalancerConfig() {
        AzureLoadBalancerConfigDb azureLoadBalancerConfigDb = new AzureLoadBalancerConfigDb();
        azureLoadBalancerConfigDb.setName(AZURE_LB_NAME);
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        cloudLoadBalancerConfigDbWrapper.setAzureConfig(azureLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    private Set<TargetGroup> createAzureTargetGroups() {
        AzureTargetGroupConfigDb azureTargetGroupConfigDb = new AzureTargetGroupConfigDb();
        azureTargetGroupConfigDb.setPortAvailabilitySetMapping(Map.of(PORT, List.of(AZURE_AS_NAME)));
        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        targetGroupConfigDbWrapper.setAzureConfig(azureTargetGroupConfigDb);
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setProviderConfig(targetGroupConfigDbWrapper);
        return Set.of(targetGroup);
    }
}
