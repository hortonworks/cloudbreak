package com.sequenceiq.cloudbreak.converter.stack.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.AwsTargetGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.AzureTargetGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.GcpTargetGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.TargetGroupResponse;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
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
import com.sequenceiq.cloudbreak.service.loadbalancer.TargetGroupPortProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
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

    private static final String GCP_LB_NAME = "gcp-lb-name";

    private static final String GCP_INSTANCE_GROUP_NAME = "instance-group-name";

    private static final String GCP_BACKEND_SERVICE_NAME = "backend-service-name";

    @Mock
    private TargetGroupPersistenceService targetGroupService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private TargetGroupPortProvider targetGroupPortProvider;

    @InjectMocks
    private LoadBalancerToLoadBalancerResponseConverter underTest;

    @BeforeEach
    public void setUp() {
        when(instanceGroupService.findByTargetGroupId(any())).thenReturn(List.of(new InstanceGroup()));
        when(instanceMetaDataService.findAliveInstancesInInstanceGroup(any())).thenReturn(createInstanceMetadata());
        when(targetGroupPortProvider.getTargetGroupPortPairs(any())).thenReturn(Set.of(new TargetGroupPortPair(PORT, PORT)));
    }

    @Test
    public void testNoSavedCloudConfig() {
        LoadBalancer source = getSource();
        // GIVEN
        given(targetGroupService.findByLoadBalancerId(any())).willReturn(createAwsTargetGroups());
        // WHEN
        LoadBalancerResponse response = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(response, List.of("awsResourceId", "azureResourceId", "gcpResourceId"));
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
        assertAllFieldsNotNull(response, List.of("azureResourceId", "gcpResourceId"));
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

    @ParameterizedTest
    @EnumSource(LoadBalancerSku.class)
    public void testConvertAzure(LoadBalancerSku sku) {
        LoadBalancer source = getSource();
        // GIVEN
        getSource().setProviderConfig(createAzureLoadBalancerConfig(sku));
        given(targetGroupService.findByLoadBalancerId(any())).willReturn(createAzureTargetGroups());
        // WHEN
        LoadBalancerResponse response = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(response, List.of("awsResourceId", "gcpResourceId"));
        assertEquals(AZURE_LB_NAME, response.getAzureResourceId().getName());
        assertEquals(sku, response.getAzureResourceId().getSku());
        assertEquals(1, response.getTargets().size());
        TargetGroupResponse targetGroupResponse = response.getTargets().getFirst();
        assertEquals(PORT, targetGroupResponse.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroupResponse.getTargetInstances());
        AzureTargetGroupResponse azureTargetGroupResponse = targetGroupResponse.getAzureResourceId();
        assertNotNull(azureTargetGroupResponse);
        assertEquals(List.of(AZURE_AS_NAME), azureTargetGroupResponse.getAvailabilitySet());
    }

    @Test
    public void testConvertGcp() {
        LoadBalancer source = getSource();
        //GIVEN
        getSource().setProviderConfig(createGcpLoadBalancerConfig());
        given(targetGroupService.findByLoadBalancerId(any())).willReturn(creatGcpTargetGroups());
        //WHEN
        LoadBalancerResponse response = underTest.convert(source);
        //THEN
        assertAllFieldsNotNull(response, List.of("awsResourceId", "azureResourceId"));
        assertEquals(GCP_LB_NAME, response.getGcpResourceId().getName());
        assertEquals(1, response.getTargets().size());
        TargetGroupResponse targetGroupResponse = response.getTargets().getFirst();
        assertEquals(PORT, targetGroupResponse.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroupResponse.getTargetInstances());
        GcpTargetGroupResponse gcpTargetGroupResponse = targetGroupResponse.getGcpResourceId();
        assertNotNull(gcpTargetGroupResponse);
        assertEquals(GCP_INSTANCE_GROUP_NAME, gcpTargetGroupResponse.getGcpInstanceGroupName());
        assertEquals(GCP_BACKEND_SERVICE_NAME, gcpTargetGroupResponse.getGcpBackendServiceName());

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

    private List<InstanceMetaData> createInstanceMetadata() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID);
        return List.of(instanceMetaData);
    }

    private LoadBalancerConfigDbWrapper createAzureLoadBalancerConfig(LoadBalancerSku sku) {
        AzureLoadBalancerConfigDb azureLoadBalancerConfigDb = new AzureLoadBalancerConfigDb();
        azureLoadBalancerConfigDb.setName(AZURE_LB_NAME);
        azureLoadBalancerConfigDb.setSku(sku);
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

    private LoadBalancerConfigDbWrapper createGcpLoadBalancerConfig() {
        GcpLoadBalancerConfigDb gcpLoadBalancerConfigDb = new GcpLoadBalancerConfigDb();
        gcpLoadBalancerConfigDb.setName(GCP_LB_NAME);
        LoadBalancerConfigDbWrapper configDbWrapper = new LoadBalancerConfigDbWrapper();
        configDbWrapper.setGcpConfig(gcpLoadBalancerConfigDb);
        return configDbWrapper;
    }

    private Set<TargetGroup> creatGcpTargetGroups() {
        GcpTargetGroupConfigDb gcpTargetGroupConfigDb = new GcpTargetGroupConfigDb();
        GcpLoadBalancerNamesDb gcpLoadBalancerNamesDb = new GcpLoadBalancerNamesDb();
        gcpLoadBalancerNamesDb.setBackendServiceName(GCP_BACKEND_SERVICE_NAME);
        gcpLoadBalancerNamesDb.setInstanceGroupName(GCP_INSTANCE_GROUP_NAME);
        gcpTargetGroupConfigDb.addPortNameMapping(PORT, gcpLoadBalancerNamesDb);
        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        targetGroupConfigDbWrapper.setGcpConfig(gcpTargetGroupConfigDb);
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setProviderConfig(targetGroupConfigDbWrapper);
        return Set.of(targetGroup);

    }
}