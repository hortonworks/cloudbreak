package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.LOAD_BALANCER_NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsInstanceCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeLbMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeLoadBalancerIpCollector;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceLifecycle;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;

@ExtendWith(MockitoExtension.class)
class AwsNativeMetadataCollectorTest {

    private static final String LB_PRIVATE_IP = "1.1.1.1";

    private static final String CRN = "crn";

    @Mock
    private AwsLifeCycleMapper awsLifeCycleMapper;

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonElasticLoadBalancingClient loadBalancingClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AwsNativeLbMetadataCollector awsNativeLbMetadataCollector;

    @Mock
    private AwsInstanceCommonService awsInstanceCommonService;

    @Mock
    private AwsNativeLoadBalancerIpCollector awsNativeLoadBalancerIpCollector;

    @InjectMocks
    private AwsNativeMetadataCollector underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "instanceFetchMaxBatchSize", 5);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn(CRN)
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-central-1")))
                .withAccountId("account")
                .build();
        Map<String, Object> credentialParams = Map.of("aws",
                Map.of("keyBased", Map.of("accessKey", "mo", "secretKey", "su")));
        CloudCredential credential = new CloudCredential("id", "alma", credentialParams, "acc");
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(context);
        lenient().when(authenticatedContext.getCloudCredential()).thenReturn(credential);
    }

    @Test
    void collectInstanceMetadataWhenTheSpecifiedInstanceIdsExist() {
        List<CloudInstance> allInstances = List.of();
        String anInstanceId = "anInstanceId";
        CloudResource cloudResource = getCloudResource(String.valueOf(1L), "instanceName", anInstanceId, AWS_INSTANCE);
        String secondInstanceId = "secondInstanceId";
        CloudResource secondCloudResource = getCloudResource(String.valueOf(2L), "secondInstanceName", secondInstanceId, AWS_INSTANCE);
        List<CloudResource> resources = List.of(cloudResource, secondCloudResource);
        InstanceTemplate instanceTemplate =
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        InstanceTemplate otherInstanceTemplate =
                new InstanceTemplate("flavor", "alma", 2L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        CloudInstance secondCloudInstance = new CloudInstance(secondInstanceId, otherInstanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance, secondCloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);

        Instance anInstance = getAnInstance(anInstanceId);
        Instance secondInstance = getAnInstance(secondInstanceId);
        Reservation reservation = Reservation.builder().instances(anInstance, secondInstance).build();
        List<Reservation> reservations = List.of(reservation);
        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder().reservations(reservations).build();
        when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResponse);

        List<CloudVmMetaDataStatus> metaDataStatuses = underTest.collect(authenticatedContext, resources, cloudInstances, allInstances);

        verify(ec2Client, times(1)).describeInstances(any());
        assertFalse(metaDataStatuses.isEmpty());
        assertEquals(resources.size(), metaDataStatuses.size());
        assertTrue(metaDataStatuses.stream()
                .allMatch(vmMetaDataStatus -> isNotEmpty(vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())));

        verifyCloudInstance(metaDataStatuses, anInstanceId, "subnet-123", "az1");
        verifyCloudInstance(metaDataStatuses, secondInstanceId, "subnet-123", "az1");
    }

    @Test
    void collectInstanceMetadataWhenTheSpecifiedInstanceIdsDoNotExist() {
        List<CloudInstance> allInstances = List.of();
        String anInstanceId = "anInstanceId";
        CloudResource cloudResource = getCloudResource(String.valueOf(1L), "instanceName", anInstanceId, AWS_INSTANCE);
        String secondInstanceId = "secondInstanceId";
        CloudResource secondCloudResource = getCloudResource(String.valueOf(2L), "secondInstanceName", secondInstanceId, AWS_INSTANCE);
        List<CloudResource> resources = List.of(cloudResource, secondCloudResource);
        InstanceTemplate instanceTemplate =
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        InstanceTemplate secondInstanceTemplate =
                new InstanceTemplate("flavor", "alma", 2L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance secondCloudInstance = new CloudInstance(secondInstanceId, secondInstanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance, secondCloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        String instancesNotFoundMessage = String.format("Instance with id could not be found: '%s, %s'", anInstanceId, secondInstanceId);
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message(instancesNotFoundMessage)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INSTANCE_NOT_FOUND).build())
                .build();
        when(ec2Client.describeInstances(any())).thenThrow(amazonServiceException);

        List<CloudVmMetaDataStatus> metaDataStatuses = underTest.collect(authenticatedContext, resources, cloudInstances, allInstances);

        verify(ec2Client, times(1)).describeInstances(any());
        assertFalse(metaDataStatuses.isEmpty());
        assertTrue(metaDataStatuses.stream()
                .allMatch(cloudVmMetaDataStatus -> InstanceStatus.TERMINATED.equals(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus())));
        assertTrue(metaDataStatuses.stream()
                .allMatch(vmMetaDataStatus -> isNotEmpty(vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())));
    }

    @Test
    void collectInstanceMetadataWhenOneOrMoreOfSpecifiedInstanceIdsDoNotExist() {
        List<CloudInstance> allInstances = List.of();
        String anInstanceId = "anInstanceId";
        CloudResource cloudResource = getCloudResource(String.valueOf(1L), "instanceName", anInstanceId, AWS_INSTANCE);
        String secondInstanceId = "secondInstanceId";
        CloudResource secondCloudResource = getCloudResource(String.valueOf(2L), "secondInstanceName", secondInstanceId, AWS_INSTANCE);
        List<CloudResource> resources = List.of(cloudResource, secondCloudResource);
        InstanceTemplate instanceTemplate =
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        InstanceTemplate secondInstanceTemplate =
                new InstanceTemplate("flavor", "alma", 2L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance secondCloudInstance = new CloudInstance(secondInstanceId, secondInstanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance, secondCloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        Instance anInstance = getAnInstance(anInstanceId);
        Reservation reservation = Reservation.builder().instances(anInstance).build();
        List<Reservation> reservations = List.of(reservation);
        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder().reservations(reservations).build();
        String instancesNotFoundMessage = String.format("Instance with ID could not be found: '%s'", secondInstanceId);
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message(instancesNotFoundMessage)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INSTANCE_NOT_FOUND).build())
                .build();
        when(ec2Client.describeInstances(any())).thenThrow(amazonServiceException).thenReturn(describeInstancesResponse);

        List<CloudVmMetaDataStatus> metaDataStatuses = underTest.collect(authenticatedContext, resources, cloudInstances, allInstances);

        verify(ec2Client, times(2)).describeInstances(any());
        assertFalse(metaDataStatuses.isEmpty());
        assertEquals(2, metaDataStatuses.size());
        assertTrue(metaDataStatuses.stream()
                .anyMatch(metaDataStatus -> InstanceStatus.TERMINATED.equals(metaDataStatus.getCloudVmInstanceStatus().getStatus())));
        assertTrue(metaDataStatuses.stream()
                .allMatch(vmMetaDataStatus -> isNotEmpty(vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())));

        verifyCloudInstance(metaDataStatuses, anInstanceId, "subnet-123", "az1");
        verifyCloudInstance(metaDataStatuses, String.valueOf(2L), "subnet-123", "az1");
    }

    @Test
    void collectInstanceMetadataWhenTheSpecifiedInstanceIdsExistAndMultipleBatchRequestsAreNecessary() {
        List<CloudInstance> allInstances = List.of();
        List<CloudResource> resources = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();
        List<Instance> ec2Instances = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            String anInstanceId = "anInstanceId" + i;
            InstanceTemplate instanceTemplate =
                    new InstanceTemplate("flavor", "alma", (long) i, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
            resources.add(getCloudResource(String.valueOf(i), "instanceName" + i, anInstanceId, AWS_INSTANCE));
            cloudInstances.add(new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1"));
            ec2Instances.add(getAnInstance(anInstanceId));
        }
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        DescribeInstancesResponse describeInstancesResponse1 = DescribeInstancesResponse.builder()
                .reservations(List.of(Reservation.builder().instances(ec2Instances.subList(0, 5)).build()))
                .build();
        DescribeInstancesResponse describeInstancesResponse2 = DescribeInstancesResponse.builder()
                .reservations(List.of(Reservation.builder().instances(ec2Instances.subList(5, 10)).build()))
                .build();
        DescribeInstancesResponse describeInstancesResponse3 = DescribeInstancesResponse.builder()
                .reservations(List.of(Reservation.builder().instances(ec2Instances.subList(10, ec2Instances.size())).build()))
                .build();

        when(ec2Client.describeInstances(any()))
                .thenReturn(describeInstancesResponse1)
                .thenReturn(describeInstancesResponse2)
                .thenReturn(describeInstancesResponse3);

        List<CloudVmMetaDataStatus> metaDataStatuses = underTest.collect(authenticatedContext, resources, cloudInstances, allInstances);

        verify(ec2Client, times(3)).describeInstances(any());
        assertFalse(metaDataStatuses.isEmpty());
        assertEquals(cloudInstances.size(), metaDataStatuses.size());

        for (int i = 0; i < 15; i++) {
            verifyCloudInstance(metaDataStatuses, "anInstanceId" + i, "subnet-123", "az1");
        }
    }

    @Test
    void collectInstanceMetadataWhenOneOrMoreOfSpecifiedInstanceIdsDoNotExistAndMultipleBatchRequestsAreNecessary() {
        List<CloudInstance> allInstances = List.of();

        List<CloudResource> resources = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();
        List<Instance> ec2Instances = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            String anInstanceId = "anInstanceId" + i;
            InstanceTemplate instanceTemplate =
                    new InstanceTemplate("flavor", "alma", (long) i, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
            resources.add(getCloudResource(String.valueOf(i), "instanceName" + i, anInstanceId, AWS_INSTANCE));
            cloudInstances.add(new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1"));
            ec2Instances.add(getAnInstance(anInstanceId));
        }
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("Instance with id could not be found: 'anInstanceId0'")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INSTANCE_NOT_FOUND).build())
                .build();
        DescribeInstancesResponse describeInstancesResponse1 = DescribeInstancesResponse.builder()
                .reservations(List.of(Reservation.builder().instances(ec2Instances.subList(1, 5)).build()))
                .build();
        AwsServiceException amazonServiceException2 = AwsServiceException.builder()
                .message("Instance with id could not be found: 'anInstanceId5'")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INSTANCE_NOT_FOUND).build())
                .build();
        DescribeInstancesResponse describeInstancesResponse2 = DescribeInstancesResponse.builder()
                .reservations(List.of(Reservation.builder().instances(ec2Instances.subList(6, 10)).build()))
                .build();
        DescribeInstancesResponse describeInstancesResponse3 = DescribeInstancesResponse.builder()
                .reservations(List.of(Reservation.builder().instances(ec2Instances.subList(10, ec2Instances.size())).build()))
                .build();

        when(ec2Client.describeInstances(any()))
                .thenThrow(amazonServiceException)
                .thenReturn(describeInstancesResponse1)
                .thenThrow(amazonServiceException2)
                .thenReturn(describeInstancesResponse2)
                .thenReturn(describeInstancesResponse3);

        List<CloudVmMetaDataStatus> metaDataStatuses = underTest.collect(authenticatedContext, resources, cloudInstances, allInstances);

        verify(ec2Client, times(5)).describeInstances(any());
        assertFalse(metaDataStatuses.isEmpty());
        assertEquals(cloudInstances.size(), metaDataStatuses.size());

        for (int i = 0; i < 15; i++) {
            String instanceId = i == 0 || i == 5 ? String.valueOf(i) : "anInstanceId" + i;
            verifyCloudInstance(metaDataStatuses, instanceId, "subnet-123", "az1");
        }
    }

    @Test
    void collectInstanceMetadataWhenNotExpectedAmazonServiceExceptionOccurs() {
        List<CloudInstance> allInstances = List.of();
        String anInstanceId = "anInstanceId";
        CloudResource cloudResource = getCloudResource("1", "instanceName", anInstanceId, AWS_INSTANCE);
        List<CloudResource> resources = List.of(cloudResource);

        InstanceTemplate instanceTemplate =
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("Something unexpected happened...")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("errorCode").build())
                .build();
        when(ec2Client.describeInstances(any())).thenThrow(amazonServiceException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.collect(authenticatedContext, resources, cloudInstances, allInstances));

        assertThat(cloudConnectorException).hasMessageStartingWith("Something unexpected happened...");
        assertThat(cloudConnectorException).hasCauseReference(amazonServiceException);

        verify(ec2Client, times(1)).describeInstances(any());
    }

    @Test
    void collectInstanceMetadataWhenRuntimeExceptionOccurs() {
        List<CloudInstance> allInstances = List.of();
        String anInstanceId = "anInstanceId";
        CloudResource cloudResource = getCloudResource("1", "instanceName", anInstanceId, AWS_INSTANCE);
        List<CloudResource> resources = List.of(cloudResource);

        InstanceTemplate instanceTemplate =
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        RuntimeException runtimeException = new RuntimeException("Something really bad happened...");
        when(ec2Client.describeInstances(any())).thenThrow(runtimeException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.collect(authenticatedContext, resources, cloudInstances, allInstances));

        assertThat(cloudConnectorException).hasMessageStartingWith("Something really bad happened...");
        assertThat(cloudConnectorException).hasCauseReference(runtimeException);

        verify(ec2Client, times(1)).describeInstances(any());
    }

    @Test
    void collectLoadBalancerMetadataWhenTheSpecifiedArnsDoNotExist() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("secondCrn", "secondInstanceName", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        LoadBalancerNotFoundException loadBalancerNotFoundException = LoadBalancerNotFoundException.builder()
                .message("One or more elastic lb not found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(LOAD_BALANCER_NOT_FOUND).build())
                .build();
        when(loadBalancingClient.describeLoadBalancers(any())).thenThrow(loadBalancerNotFoundException);

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources);

        verify(loadBalancingClient, times(1)).describeLoadBalancers(any());
        assertTrue(cloudLoadBalancerMetadata.isEmpty());
    }

    @Test
    void collectLoadBalancerMetadataWhenOneOfSpecifiedArnsDoNotExist() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("aCrn", "lbname", null, ELASTIC_LOAD_BALANCER);
        CloudResource secondCloudResource = getCloudResource("secondCrn", "lbnamesecond", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource, secondCloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        LoadBalancerNotFoundException loadBalancerNotFoundException = LoadBalancerNotFoundException.builder()
                .message("One or more elastic lb not found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(LOAD_BALANCER_NOT_FOUND).build())
                .build();
        LoadBalancer loadBalancer = LoadBalancer.builder().scheme(LoadBalancerSchemeEnum.INTERNAL).build();
        when(loadBalancingClient.describeLoadBalancers(any()))
                .thenReturn(DescribeLoadBalancersResponse.builder().loadBalancers(loadBalancer).build())
                .thenThrow(loadBalancerNotFoundException);
        when(awsNativeLoadBalancerIpCollector.getLoadBalancerIp(eq(ec2Client), any(), eq(CRN))).thenReturn(Optional.of(LB_PRIVATE_IP));

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources);

        verify(loadBalancingClient, times(2)).describeLoadBalancers(any());
        assertFalse(cloudLoadBalancerMetadata.isEmpty());
        assertTrue(cloudLoadBalancerMetadata.stream().allMatch(metadata -> LB_PRIVATE_IP.equals(metadata.getIp())));
    }

    @Test
    void collectLoadBalancerMetadataWhenTheSpecifiedArnsExists() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("aCrn", "lbname", null, ELASTIC_LOAD_BALANCER);
        cloudResource.putParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PRIVATE);
        CloudResource secondCloudResource = getCloudResource("secondCrn", "lbnamesecond", null, ELASTIC_LOAD_BALANCER);
        secondCloudResource.putParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.GATEWAY_PRIVATE);
        List<CloudResource> cloudResources = List.of(cloudResource, secondCloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        LoadBalancer loadBalancer = LoadBalancer.builder().scheme(LoadBalancerSchemeEnum.INTERNAL).build();
        when(loadBalancingClient.describeLoadBalancers(any()))
                .thenReturn(DescribeLoadBalancersResponse.builder().loadBalancers(loadBalancer).build());
        when(loadBalancerTypeConverter.convert(LoadBalancerSchemeEnum.INTERNAL)).thenReturn(LoadBalancerType.PRIVATE);
        when(awsNativeLoadBalancerIpCollector.getLoadBalancerIp(eq(ec2Client), any(), eq(CRN))).thenReturn(Optional.of(LB_PRIVATE_IP));

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources);

        verify(loadBalancingClient, times(2)).describeLoadBalancers(any());
        assertFalse(cloudLoadBalancerMetadata.isEmpty());
        assertEquals(cloudResources.size(), cloudLoadBalancerMetadata.size());
        assertThat(cloudLoadBalancerMetadata.stream().map(CloudLoadBalancerMetadata::getType).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(LoadBalancerType.PRIVATE, LoadBalancerType.GATEWAY_PRIVATE);
        assertTrue(cloudLoadBalancerMetadata.stream().allMatch(metadata -> LB_PRIVATE_IP.equals(metadata.getIp())));
    }

    @Test
    void collectLoadBalancerMetadataWhenWhenNotExpectedAmazonServiceExceptionOccurs() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("secondCrn", "secondInstanceName", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        LoadBalancerNotFoundException loadBalancerNotFoundException = LoadBalancerNotFoundException.builder()
                .message("One or more elastic lb not found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("errorCode").build())
                .build();
        when(loadBalancingClient.describeLoadBalancers(any())).thenThrow(loadBalancerNotFoundException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources));

        assertThat(cloudConnectorException).hasMessage("Metadata collection of load balancers failed");
        assertThat(cloudConnectorException).hasCauseReference(loadBalancerNotFoundException);

        verify(loadBalancingClient, times(1)).describeLoadBalancers(any());
    }

    @Test
    void collectLoadBalancerMetadataWhenWhenRuntimeExceptionOccurs() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("secondCrn", "secondInstanceName", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        RuntimeException runtimeException = new RuntimeException("Something really bad happened...");
        when(loadBalancingClient.describeLoadBalancers(any())).thenThrow(runtimeException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources));

        assertThat(cloudConnectorException)
                .hasMessage("Metadata collection of load balancers failed. Reason: Something really bad happened...");
        assertThat(cloudConnectorException).hasCauseReference(runtimeException);

        verify(loadBalancingClient, times(1)).describeLoadBalancers(any());
    }

    @Test
    void testCollectInstanceTypes() {
        List<String> instanceIds = List.of("instanceId1", "instanceId2");
        when(awsInstanceCommonService.collectInstanceTypes(eq(authenticatedContext), eq(instanceIds)))
                .thenReturn(new InstanceTypeMetadata(Map.of("instance1", "large", "instance2", "large")));
        InstanceTypeMetadata result = underTest.collectInstanceTypes(authenticatedContext, instanceIds);
        verify(awsInstanceCommonService, times(1)).collectInstanceTypes(eq(authenticatedContext), eq(instanceIds));
        Map<String, String> instanceTypes = result.getInstanceTypes();
        assertThat(instanceTypes).hasSize(2);
        assertThat(instanceTypes).containsEntry("instance1", "large");
        assertThat(instanceTypes).containsEntry("instance2", "large");
    }

    @Test
    void testCollectCdpInstances() {
        InstanceCheckMetadata instanceCheckMetadata1 = mock(InstanceCheckMetadata.class);
        InstanceCheckMetadata instanceCheckMetadata2 = mock(InstanceCheckMetadata.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<String> knownInstanceIds = mock(List.class);
        when(awsInstanceCommonService.collectCdpInstances(authenticatedContext, CRN, knownInstanceIds))
                .thenReturn(List.of(instanceCheckMetadata1, instanceCheckMetadata2));

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, CRN, cloudStack, knownInstanceIds);

        assertThat(result).containsExactlyInAnyOrder(instanceCheckMetadata1, instanceCheckMetadata2);
    }

    private CloudResource getCloudResource(String reference, String name, String instanceId, ResourceType resourceType) {
        return CloudResource.builder()
                .withType(resourceType)
                .withReference(reference)
                .withName(name)
                .withInstanceId(instanceId)
                .build();
    }

    private Instance getAnInstance(String instanceId) {
        return Instance.builder()
                .instanceId(instanceId)
                .instanceLifecycle(InstanceLifecycle.ON_DEMAND.name())
                .publicIpAddress("52.0.0.0")
                .privateIpAddress("10.0.0.0")
                .build();
    }

    private void verifyCloudInstance(List<CloudVmMetaDataStatus> metaDataStatuses, String instanceId, String subnetIdExpected,
            String availabilityZoneExpected) {
        Optional<CloudInstance> foundInstanceOptional = metaDataStatuses.stream()
                .map(vmMetaDataStatus -> vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance())
                .filter(cloudInstance -> instanceId.equals(cloudInstance.getInstanceId()))
                .findFirst();
        assertThat(foundInstanceOptional).isPresent();
        CloudInstance foundInstance = foundInstanceOptional.get();
        assertThat(foundInstance.getSubnetId()).isEqualTo(subnetIdExpected);
        assertThat(foundInstance.getAvailabilityZone()).isEqualTo(availabilityZoneExpected);
    }
}
