package com.sequenceiq.cloudbreak.cloud.aws;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
public class AwsMetaDataCollectorTest {

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
    private DescribeInstancesRequest describeInstancesRequestGw;

    @Mock
    private DescribeInstancesResult describeInstancesResultGw;

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Mock
    private AwsLoadBalancerMetadataCollector awsLoadBalancerMetadataCollector;

    @InjectMocks
    private AwsMetadataCollector awsMetadataCollector;

    @Mock
    private DescribeSubnetsResult describeSubnetsResult;

    @Captor
    private ArgumentCaptor<DescribeSubnetsRequest> describeSubnetsRequestCaptor;

    @Test
    public void collectMigratedExistingOneGroup() {
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

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance = Mockito.mock(Instance.class);
        when(instance.getInstanceId()).thenReturn("i-1");
        when(instance.getPrivateIpAddress()).thenReturn("privateIp");
        when(instance.getPublicIpAddress()).thenReturn("publicIp");
        when(instance.getSubnetId()).thenReturn(SUBNET_ID_1);

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, Collections.emptyList(), vms, vms);

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
        when(amazonEC2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(describeSubnetsResult);
        when(describeSubnetsResult.getSubnets()).thenReturn(subnets);
    }

    private Subnet initSubnet(String subnetId, String availabilityZone) {
        Subnet subnet = Mockito.mock(Subnet.class);
        when(subnet.getSubnetId()).thenReturn(subnetId);
        when(subnet.getAvailabilityZone()).thenReturn(availabilityZone);
        return subnet;
    }

    private void verifyQueriedSubnetIds(String... subnetIdsExpected) {
        verify(amazonEC2Client).describeSubnets(describeSubnetsRequestCaptor.capture());
        DescribeSubnetsRequest describeSubnetsRequestCaptured = describeSubnetsRequestCaptor.getValue();
        assertThat(describeSubnetsRequestCaptured).isNotNull();
        assertThat(describeSubnetsRequestCaptured.getSubnetIds()).containsOnly(subnetIdsExpected);
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
    public void collectUnknownInstances() {
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

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        List<Instance> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Instance instance = Mockito.mock(Instance.class);
            when(instance.getInstanceId()).thenReturn("i-" + i);
            when(instance.getPrivateIpAddress()).thenReturn("privateIp" + i);
            when(instance.getPublicIpAddress()).thenReturn("publicIp" + i);
            when(instance.getSubnetId()).thenReturn(i == 0 ? SUBNET_ID_1 : i == 1 ? SUBNET_ID_2 : null);
            instances.add(instance);
        }
        Instance[] instancesArray = new Instance[instances.size()];
        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instances.toArray(instancesArray)));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1), entry(SUBNET_ID_2, AVAILABILITY_ZONE_2)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, Collections.emptyList(), vms, Collections.emptyList());

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
    public void collectNewAndExistingOne() {
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
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Arrays.asList("i-1", "i-2");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Mockito.when(awsLifeCycleMapper.getLifeCycle(any())).thenReturn(CLOUD_INSTANCE_LIFE_CYCLE);

        Instance instance1 = Mockito.mock(Instance.class);
        when(instance1.getInstanceId()).thenReturn("i-1");
        when(instance1.getPrivateIpAddress()).thenReturn("privateIp1");
        when(instance1.getPublicIpAddress()).thenReturn("publicIp1");
        when(instance1.getSubnetId()).thenReturn(SUBNET_ID_1);

        Instance instance2 = Mockito.mock(Instance.class);
        when(instance2.getInstanceId()).thenReturn("i-2");
        when(instance2.getPrivateIpAddress()).thenReturn("privateIp2");
        when(instance2.getPublicIpAddress()).thenReturn("publicIp2");
        when(instance2.getSubnetId()).thenReturn(SUBNET_ID_1);

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance1, instance2));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_1, AVAILABILITY_ZONE_1)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, Collections.emptyList(), vms, vms);

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
    public void collectNewNodes() {
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

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Arrays.asList("i-1", "i-2");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance1 = Mockito.mock(Instance.class);
        when(instance1.getInstanceId()).thenReturn("i-1");
        when(instance1.getSubnetId()).thenReturn(SUBNET_ID_1);

        Instance instance2 = Mockito.mock(Instance.class);
        when(instance2.getInstanceId()).thenReturn("i-2");
        when(instance2.getPrivateIpAddress()).thenReturn("privateIp2");
        when(instance2.getPublicIpAddress()).thenReturn("publicIp2");
        when(instance2.getSubnetId()).thenReturn(SUBNET_ID_2);

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance1, instance2));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        initSubnetsQuery(Map.ofEntries(entry(SUBNET_ID_2, AVAILABILITY_ZONE_2)));

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, Collections.emptyList(), newVms, everyVms);

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
    public void testCollectLoadBalancers() {
        setupMethodsForLoadBalancer(true);
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = awsMetadataCollector.collectLoadBalancer(ac,
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
    public void testCollectLoadBalancerOnlyDefaultGateway() {
        setupMethodsForLoadBalancer(true);
        when(awsLoadBalancerMetadataCollector.getParameters(any(), any(), any())).thenReturn(Map.of());
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = awsMetadataCollector.collectLoadBalancer(ac,
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
    public void testCollectLoadBalancerOnlyEndpointAccessGateway() {
        setupMethodsForLoadBalancer(true);
        when(awsLoadBalancerMetadataCollector.getParameters(any(), any(), any())).thenReturn(Map.of());
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = awsMetadataCollector.collectLoadBalancer(ac,
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
    public void testCollectLoadBalancerMissingMetadata() {
        setupMethodsForLoadBalancer(false);
        when(awsLoadBalancerMetadataCollector.getParameters(any(), any(), any())).thenReturn(Map.of());
        AuthenticatedContext ac = authenticatedContext();
        List<CloudLoadBalancerMetadata> metadata = awsMetadataCollector.collectLoadBalancer(ac,
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

    private Reservation getReservation(Instance... instance) {
        List<Instance> instances = Arrays.asList(instance);
        Reservation r = new Reservation();
        r.setInstances(instances);
        return r;
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
        CloudCredential credential = new CloudCredential("crn", null, null, false);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);
        authenticatedContext.putParameter(AmazonEc2Client.class, amazonEC2Client);
        return authenticatedContext;
    }

    private void setupMethodsForLoadBalancer(boolean canFindInternalLB) {
        LoadBalancer internalLoadBalancer = new LoadBalancer()
                .withDNSName(INTERNAL_LB_DNS)
                .withCanonicalHostedZoneId(ZONE_1);
        LoadBalancer externalLoadBalancer = new LoadBalancer()
                .withDNSName(EXTERNAL_LB_DNS)
                .withCanonicalHostedZoneId(ZONE_2);

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
        CloudConnectorException result = assertThrows(CloudConnectorException.class, () -> awsMetadataCollector.collect(ac,
                Collections.emptyList(), vms, vms));

        assertThat(result).hasMessage("Serious problem");
        assertThat(result).hasCause(exception);
    }
}
