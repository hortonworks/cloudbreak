package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsInstanceCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;

@ExtendWith(MockitoExtension.class)
class AwsMetadataCollectorTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final CloudInstanceLifeCycle CLOUD_INSTANCE_LIFE_CYCLE = CloudInstanceLifeCycle.SPOT;

    private static final String INTERNAL_LB_DNS = "internal-lb.aws.dns";

    private static final String EXTERNAL_LB_DNS = "external-lb.aws.dns";

    private static final String INTERNAL_LB_ID = "LoadBalancerInternal";

    private static final String EXTERNAL_LB_ID = "LoadBalancerExternal";

    private static final String ZONE_1 = "zone1";

    private static final String ZONE_2 = "zone2";

    private static final String SUBNET_ID_1 = "subnetId1";

    private static final String SUBNET_ID_2 = "subnetId2";

    private static final String AVAILABILITY_ZONE_1 = "availabilityZone1";

    private static final String AVAILABILITY_ZONE_2 = "availabilityZone2";

    private static final String RESOURCE_CRN = "resourceCrn";

    private final DescribeInstancesRequest describeInstancesRequestGw = DescribeInstancesRequest.builder().build();

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Mock
    private AwsLifeCycleMapper awsLifeCycleMapper;

    @Mock
    private AmazonCloudFormationClient amazonCFClient;

    @Mock
    private AmazonAutoScalingClient amazonASClient;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Mock
    private AwsLoadBalancerMetadataCollector awsLoadBalancerMetadataCollector;

    @Mock
    private AwsInstanceCommonService awsInstanceCommonService;

    @InjectMocks
    private AwsMetadataCollector underTest;

    @Captor
    private ArgumentCaptor<DescribeSubnetsRequest> describeSubnetsRequestCaptor;

    @Test
    void collectMigratedExistingOneGroup() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        vms.add(new CloudInstance("i-1",
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));

        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .instanceId("i-1")
                                .privateIpAddress("privateIp")
                                .publicIpAddress("publicIp")
                                .subnetId(SUBNET_ID_1)
                                .build())
                        .build())
                .build();

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.retryableDescribeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResponse);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = underTest.collect(ac, Collections.emptyList(), vms, vms);

        assertEquals(1L, statuses.size());
        assertEquals("i-1", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        assertEquals("privateIp", statuses.get(0).getMetaData().getPrivateIp());
        assertEquals("publicIp", statuses.get(0).getMetaData().getPublicIp());

        verifyQueriedSubnetIds(SUBNET_ID_1);
        verifyResultSubnetIds(statuses, SUBNET_ID_1);
        verifyResultAvailabilityZones(statuses, AVAILABILITY_ZONE_1);
    }

    private void initSubnetsQuery(Map<String, String> subnetIdToAvailabilityZoneMap) {
        List<Subnet> subnets = subnetIdToAvailabilityZoneMap.entrySet().stream()
                .map(entry -> initSubnet(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        DescribeSubnetsResponse describeSubnetsResponse = DescribeSubnetsResponse.builder().subnets(subnets).build();
        when(amazonEC2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(describeSubnetsResponse);
    }

    private Subnet initSubnet(String subnetId, String availabilityZone) {
        return Subnet.builder()
                .subnetId(subnetId)
                .availabilityZone(availabilityZone)
                .build();
    }

    private void verifyQueriedSubnetIds(String... subnetIdsExpected) {
        verify(amazonEC2Client).describeSubnets(describeSubnetsRequestCaptor.capture());
        DescribeSubnetsRequest describeSubnetsRequestCaptured = describeSubnetsRequestCaptor.getValue();
        assertThat(describeSubnetsRequestCaptured).isNotNull();
        assertThat(describeSubnetsRequestCaptured.subnetIds()).containsOnly(subnetIdsExpected);
    }

    private void verifyResultSubnetIds(List<CloudVmMetaDataStatus> statuses, String... subnetIdsExpected) {
        assertThat(statuses).hasSize(subnetIdsExpected.length);
        for (int i = 0; i < statuses.size(); i++) {
            assertThat(statuses.get(i).getCloudVmInstanceStatus().getCloudInstance().getStringParameter(NetworkConstants.SUBNET_ID))
                    .isEqualTo(subnetIdsExpected[i]);
        }
    }

    private void verifyResultAvailabilityZones(List<CloudVmMetaDataStatus> statuses, String... availabilityZonesExpected) {
        assertThat(statuses).hasSize(availabilityZonesExpected.length);
        for (int i = 0; i < statuses.size(); i++) {
            assertThat(statuses.get(i).getCloudVmInstanceStatus().getCloudInstance().getAvailabilityZone())
                    .isEqualTo(availabilityZonesExpected[i]);
        }
    }

    @Test
    void collectInCaseOfRepairButVolumesAreDeleted() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        List<CloudResource> resources = new ArrayList<>();
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab",
                List.of(new VolumeSetAttributes.Volume("volid1", "device", 100, "type", CloudVolumeUsageType.GENERAL)),
                100, "type");
        resources.add(CloudResource.builder().withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.REQUESTED).withName("volume1").withGroup("worker")
                .withParameters(Map.of("attributes",
                        volumeSetAttributes))
                .build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        vms.add(new CloudInstance(null,
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, Map.of(FQDN, "fqdn1"), 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1", Map.of(FQDN, "fqdn1")));

        InstanceBlockDeviceMapping instanceBlockDeviceMapping = InstanceBlockDeviceMapping.builder()
                .ebs(EbsInstanceBlockDevice.builder().volumeId("volid1").build()).build();

        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .instanceId("i-1")
                                .privateIpAddress("privateIp1")
                                .publicIpAddress("publicIp1")
                                .blockDeviceMappings(instanceBlockDeviceMapping)
                                .subnetId(SUBNET_ID_1)
                                .build())
                        .build())
                .build();

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.retryableDescribeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResponse);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1), entry(SUBNET_ID_2, AVAILABILITY_ZONE_2)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = underTest.collect(ac, resources, vms, Collections.emptyList());
        assertEquals(1L, statuses.size());
        assertEquals("i-1", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        assertEquals("privateIp1", statuses.get(0).getMetaData().getPrivateIp());
        assertEquals("publicIp1", statuses.get(0).getMetaData().getPublicIp());
    }

    @Test
    void collectUnknownInstances() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        vms.add(new CloudInstance(null,
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));
        vms.add(new CloudInstance(null,
                new InstanceTemplate("fla", "cbgateway", 6L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));
        vms.add(new CloudInstance(null,
                new InstanceTemplate("fla", "cbgateway", 7L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));

        List<Instance> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Instance instance = Instance.builder()
                    .instanceId("i-" + i)
                    .privateIpAddress("privateIp" + i)
                    .publicIpAddress("publicIp" + i)
                    .subnetId(i == 0 ? SUBNET_ID_1 : i == 1 ? SUBNET_ID_2 : null)
                    .build();
            instances.add(instance);
        }

        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instances).build()).build();

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.retryableDescribeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResponse);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1), entry(SUBNET_ID_2, AVAILABILITY_ZONE_2)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = underTest.collect(ac, Collections.emptyList(), vms, Collections.emptyList());

        assertEquals(3L, statuses.size());
        assertEquals("i-0", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        assertEquals("privateIp0", statuses.get(0).getMetaData().getPrivateIp());
        assertEquals("publicIp0", statuses.get(0).getMetaData().getPublicIp());
        assertEquals("i-1", statuses.get(1).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        assertEquals("privateIp1", statuses.get(1).getMetaData().getPrivateIp());
        assertEquals("publicIp1", statuses.get(1).getMetaData().getPublicIp());
        assertEquals("i-2", statuses.get(2).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        assertEquals("privateIp2", statuses.get(2).getMetaData().getPrivateIp());
        assertEquals("publicIp2", statuses.get(2).getMetaData().getPublicIp());

        verifyQueriedSubnetIds(SUBNET_ID_1, SUBNET_ID_2, null);
        verifyResultSubnetIds(statuses, SUBNET_ID_1, SUBNET_ID_2, null);
        verifyResultAvailabilityZones(statuses, AVAILABILITY_ZONE_1, AVAILABILITY_ZONE_2, null);
    }

    @Test
    void collectNewAndExistingOne() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        vms.add(new CloudInstance(null,
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));
        vms.add(new CloudInstance("i-1",
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));

        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(List.of(
                                Instance.builder()
                                        .instanceId("i-1")
                                        .privateIpAddress("privateIp1")
                                        .publicIpAddress("publicIp1")
                                        .subnetId(SUBNET_ID_1)
                                        .build(),
                                Instance.builder()
                                        .instanceId("i-2")
                                        .privateIpAddress("privateIp2")
                                        .publicIpAddress("publicIp2")
                                        .subnetId(SUBNET_ID_1)
                                        .build()))
                        .build())
                .build();

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Arrays.asList("i-1", "i-2");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.retryableDescribeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResponse);

        Mockito.when(awsLifeCycleMapper.getLifeCycle(any())).thenReturn(CLOUD_INSTANCE_LIFE_CYCLE);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = underTest.collect(ac, Collections.emptyList(), vms, vms);

        assertEquals(2L, statuses.size());
        assertTrue(statuses.stream().anyMatch(predicate -> "i-1".equals(predicate.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())));
        assertTrue(statuses.stream().anyMatch(predicate -> "privateIp1".equals(predicate.getMetaData().getPrivateIp())));
        assertTrue(statuses.stream().anyMatch(predicate -> "publicIp1".equals(predicate.getMetaData().getPublicIp())));

        assertTrue(statuses.stream().anyMatch(predicate -> "i-2".equals(predicate.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())));
        assertTrue(statuses.stream().anyMatch(predicate -> "privateIp2".equals(predicate.getMetaData().getPrivateIp())));
        assertTrue(statuses.stream().anyMatch(predicate -> "publicIp2".equals(predicate.getMetaData().getPublicIp())));

        assertTrue(statuses.stream().allMatch(predicate -> CLOUD_INSTANCE_LIFE_CYCLE.equals(predicate.getMetaData().getLifeCycle())));

        verifyQueriedSubnetIds(SUBNET_ID_1);
        verifyResultSubnetIds(statuses, SUBNET_ID_1, SUBNET_ID_1);
        verifyResultAvailabilityZones(statuses, AVAILABILITY_ZONE_1, AVAILABILITY_ZONE_1);
    }

    @Test
    void collectNewNodes() {
        List<CloudInstance> everyVms = new ArrayList<>();
        List<CloudInstance> newVms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance1 = new CloudInstance(null,
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L, "imageId",
                        TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1");
        everyVms.add(cloudInstance1);
        newVms.add(cloudInstance1);

        everyVms.add(new CloudInstance("i-1",
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));

        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(List.of(
                                Instance.builder()
                                        .instanceId("i-1")
                                        .subnetId(SUBNET_ID_1)
                                        .build(),
                                Instance.builder()
                                        .instanceId("i-2")
                                        .privateIpAddress("privateIp2")
                                        .publicIpAddress("publicIp2")
                                        .subnetId(SUBNET_ID_2)
                                        .build()))
                        .build())
                .build();

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Arrays.asList("i-1", "i-2");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.retryableDescribeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResponse);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_2, AVAILABILITY_ZONE_2)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = underTest.collect(ac, Collections.emptyList(), newVms, everyVms);

        assertEquals(1L, statuses.size());
        assertTrue(statuses.stream().anyMatch(predicate -> "i-2".equals(predicate.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())));
        assertTrue(statuses.stream().anyMatch(predicate -> "privateIp2".equals(predicate.getMetaData().getPrivateIp())));
        assertTrue(statuses.stream().anyMatch(predicate -> "publicIp2".equals(predicate.getMetaData().getPublicIp())));

        verifyQueriedSubnetIds(SUBNET_ID_1, SUBNET_ID_2);
        verifyResultSubnetIds(statuses, SUBNET_ID_2);
        assertThat(everyVms.get(0).getStringParameter(NetworkConstants.SUBNET_ID)).isNull();
        verifyResultAvailabilityZones(statuses, AVAILABILITY_ZONE_2);
    }

    @Test
    void testCollectLoadBalancers() {
        setupMethodsForLoadBalancer(true);
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = underTest.collectLoadBalancer(ac,
                List.of(LoadBalancerType.PRIVATE, LoadBalancerType.PUBLIC), null);

        assertEquals(2, metadata.size());
        Optional<CloudLoadBalancerMetadata> internalMetadata = metadata.stream()
                .filter(m -> m.getType() == LoadBalancerType.PRIVATE)
                .findFirst();
        assertTrue(internalMetadata.isPresent());
        assertEquals(INTERNAL_LB_DNS, internalMetadata.get().getCloudDns());
        assertEquals(ZONE_1, internalMetadata.get().getHostedZoneId());
        Optional<CloudLoadBalancerMetadata> externalMetadata = metadata.stream()
                .filter(m -> m.getType() == LoadBalancerType.PUBLIC)
                .findFirst();
        assertTrue(externalMetadata.isPresent());
        assertEquals(EXTERNAL_LB_DNS, externalMetadata.get().getCloudDns());
        assertEquals(ZONE_2, externalMetadata.get().getHostedZoneId());
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    void testCollectLoadBalancerOnlyDefaultGateway() {
        setupMethodsForLoadBalancer(true);
        when(awsLoadBalancerMetadataCollector.getParameters(any(), any(), any())).thenReturn(Map.of());
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = underTest.collectLoadBalancer(ac,
                List.of(LoadBalancerType.PRIVATE), null);

        assertEquals(1, metadata.size());
        Optional<CloudLoadBalancerMetadata> internalMetadata = metadata.stream()
                .filter(m -> m.getType() == LoadBalancerType.PRIVATE)
                .findFirst();
        assertTrue(internalMetadata.isPresent());
        assertEquals(INTERNAL_LB_DNS, internalMetadata.get().getCloudDns());
        assertEquals(ZONE_1, internalMetadata.get().getHostedZoneId());
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    void testCollectLoadBalancerOnlyEndpointAccessGateway() {
        setupMethodsForLoadBalancer(true);
        when(awsLoadBalancerMetadataCollector.getParameters(any(), any(), any())).thenReturn(Map.of());
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = underTest.collectLoadBalancer(ac,
                List.of(LoadBalancerType.PUBLIC), null);

        assertEquals(1, metadata.size());
        Optional<CloudLoadBalancerMetadata> externalMetadata = metadata.stream()
                .filter(m -> m.getType() == LoadBalancerType.PUBLIC)
                .findFirst();
        assertTrue(externalMetadata.isPresent());
        assertEquals(EXTERNAL_LB_DNS, externalMetadata.get().getCloudDns());
        assertEquals(ZONE_2, externalMetadata.get().getHostedZoneId());
    }

    @Test
    void testCollectLoadBalancerMissingMetadata() {
        setupMethodsForLoadBalancer(false);
        when(awsLoadBalancerMetadataCollector.getParameters(any(), any(), any())).thenReturn(Map.of());
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = underTest.collectLoadBalancer(ac,
                List.of(LoadBalancerType.PRIVATE, LoadBalancerType.PUBLIC), null);

        assertEquals(1, metadata.size());
        Optional<CloudLoadBalancerMetadata> externalMetadata = metadata.stream()
                .filter(m -> m.getType() == LoadBalancerType.PUBLIC)
                .findFirst();
        assertTrue(externalMetadata.isPresent());
        assertEquals(LoadBalancerType.PUBLIC, metadata.iterator().next().getType());
        assertEquals(EXTERNAL_LB_DNS, metadata.iterator().next().getCloudDns());
        assertEquals(ZONE_2, metadata.iterator().next().getHostedZoneId());
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
        CloudCredential credential = new CloudCredential("crn", null, null, "acc");
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);
        authenticatedContext.putParameter(AmazonEc2Client.class, amazonEC2Client);
        return authenticatedContext;
    }

    private void setupMethodsForLoadBalancer(boolean canFindInternalLB) {
        LoadBalancer internalLoadBalancer = LoadBalancer.builder()
                .dnsName(INTERNAL_LB_DNS)
                .canonicalHostedZoneId(ZONE_1)
                .build();
        LoadBalancer externalLoadBalancer = LoadBalancer.builder()
                .dnsName(EXTERNAL_LB_DNS)
                .canonicalHostedZoneId(ZONE_2)
                .build();

        if (canFindInternalLB) {
            when(cloudFormationStackUtil.getLoadBalancerByLogicalId(any(), eq(INTERNAL_LB_ID))).thenReturn(internalLoadBalancer);
        } else {
            when(cloudFormationStackUtil.getLoadBalancerByLogicalId(any(), eq(INTERNAL_LB_ID))).thenThrow(new RuntimeException("missing metadata"));
        }
        when(cloudFormationStackUtil.getLoadBalancerByLogicalId(any(), eq(EXTERNAL_LB_ID))).thenReturn(externalLoadBalancer);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PRIVATE))).thenReturn(AwsLoadBalancerScheme.INTERNAL);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);
    }

    @Test
    void collectTestWhenError() {
        List<CloudInstance> vms = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        vms.add(new CloudInstance("i-1",
                new InstanceTemplate("fla", "cbgateway", 5L, new ArrayList<>(), InstanceStatus.CREATED, null, 0L, "imageId",
                        TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));

        UnsupportedOperationException exception = new UnsupportedOperationException("Serious problem");
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenThrow(exception);

        AuthenticatedContext ac = authenticatedContext();
        CloudConnectorException result = assertThrows(CloudConnectorException.class, () -> underTest.collect(ac,
                Collections.emptyList(), vms, vms));

        assertThat(result).hasMessage("Serious problem");
        assertThat(result).hasCause(exception);
    }

    @Test
    void testCollectWhenAutoscalingGroupEmptyForAGroupThatHasMetadataOnOurSide() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        vms.add(new CloudInstance("i-1",
                new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null, 0L,
                        "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                instanceAuthentication,
                "subnet-1",
                "az1"));
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);
        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(List.of());
        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1)));
        AuthenticatedContext ac = authenticatedContext();

        List<CloudVmMetaDataStatus> statuses = underTest.collect(ac, Collections.emptyList(), vms, vms);

        assertEquals(0L, statuses.size());
        assertTrue(statuses.stream().allMatch(predicate -> CLOUD_INSTANCE_LIFE_CYCLE.equals(predicate.getMetaData().getLifeCycle())));
        verify(cloudFormationStackUtil, times(0)).createDescribeInstancesRequest(any());
        verify(amazonEC2Client, times(0)).retryableDescribeInstances(any());
    }

    @Test
    void testCollectInstanceTypes() {
        AuthenticatedContext ac = authenticatedContext();
        List<String> instanceIds = List.of("instance1", "instance2");
        when(awsInstanceCommonService.collectInstanceTypes(eq(ac), eq(instanceIds)))
                .thenReturn(new InstanceTypeMetadata(Map.of("instance1", "large", "instance2", "large")));
        InstanceTypeMetadata result = underTest.collectInstanceTypes(ac, instanceIds);
        verify(awsInstanceCommonService, times(1)).collectInstanceTypes(eq(ac), eq(instanceIds));
        Map<String, String> instanceTypes = result.getInstanceTypes();
        assertThat(instanceTypes).hasSize(2);
        assertThat(instanceTypes).containsEntry("instance1", "large");
        assertThat(instanceTypes).containsEntry("instance2", "large");
    }

    @Test
    void testCollectCdpInstances() {
        InstanceCheckMetadata instanceCheckMetadata1 = mock(InstanceCheckMetadata.class);
        InstanceCheckMetadata instanceCheckMetadata2 = mock(InstanceCheckMetadata.class);
        AuthenticatedContext ac = authenticatedContext();
        CloudStack cloudStack = mock(CloudStack.class);
        List<String> knownInstanceIds = mock(List.class);
        when(awsInstanceCommonService.collectCdpInstances(ac, RESOURCE_CRN, knownInstanceIds))
                .thenReturn(List.of(instanceCheckMetadata1, instanceCheckMetadata2));

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(ac, RESOURCE_CRN, cloudStack, knownInstanceIds);

        assertThat(result).containsExactlyInAnyOrder(instanceCheckMetadata1, instanceCheckMetadata2);
    }
}
