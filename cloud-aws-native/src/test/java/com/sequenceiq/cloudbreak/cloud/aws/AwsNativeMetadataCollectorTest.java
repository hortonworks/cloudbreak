package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeMetadataCollector.LOAD_BALANCER_NOT_FOUND_ERROR_CODE;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceLifecycle;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeLbMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsNativeMetadataCollectorTest {

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

    @InjectMocks
    private AwsNativeMetadataCollector underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "instanceFetchMaxBatchSize", 5);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-central-1")))
                .withAccountId("account")
                .build();
        Map<String, Object> credentialParams = Map.of("aws",
                Map.of("keyBased", Map.of("accessKey", "mo", "secretKey", "su")));
        CloudCredential credential = new CloudCredential("id", "alma", credentialParams, false);
        when(authenticatedContext.getCloudContext()).thenReturn(context);
        when(authenticatedContext.getCloudCredential()).thenReturn(credential);
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
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        InstanceTemplate otherInstanceTemplate =
                new InstanceTemplate("flavor", "alma", 2L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        CloudInstance secondCloudInstance = new CloudInstance(secondInstanceId, otherInstanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance, secondCloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);

        Instance anInstance = getAnInstance(anInstanceId);
        Instance secondInstance = getAnInstance(secondInstanceId);
        Reservation reservation = new Reservation().withInstances(anInstance, secondInstance);
        List<Reservation> reservations = List.of(reservation);
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservations);
        when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult);

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
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        InstanceTemplate secondInstanceTemplate =
                new InstanceTemplate("flavor", "alma", 2L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance secondCloudInstance = new CloudInstance(secondInstanceId, secondInstanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance, secondCloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        String instancesNotFoundMessage = String.format("Instance with id could not be found: '%s, %s'", anInstanceId, secondInstanceId);
        AmazonServiceException amazonServiceException = new AmazonServiceException(instancesNotFoundMessage);
        amazonServiceException.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
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
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        InstanceTemplate secondInstanceTemplate =
                new InstanceTemplate("flavor", "alma", 2L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance secondCloudInstance = new CloudInstance(secondInstanceId, secondInstanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance, secondCloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        Instance anInstance = getAnInstance(anInstanceId);
        Reservation reservation = new Reservation().withInstances(anInstance);
        List<Reservation> reservations = List.of(reservation);
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservations);
        String instancesNotFoundMessage = String.format("Instance with ID could not be found: '%s'", secondInstanceId);
        AmazonServiceException amazonServiceException = new AmazonServiceException(instancesNotFoundMessage);
        amazonServiceException.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        when(ec2Client.describeInstances(any())).thenThrow(amazonServiceException).thenReturn(describeInstancesResult);

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
                    new InstanceTemplate("flavor", "alma", (long) i, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
            resources.add(getCloudResource(String.valueOf(i), "instanceName" + i, anInstanceId, AWS_INSTANCE));
            cloudInstances.add(new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1"));
            ec2Instances.add(getAnInstance(anInstanceId));
        }
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        DescribeInstancesResult describeInstancesResult1 = new DescribeInstancesResult()
                .withReservations(List.of(new Reservation().withInstances(ec2Instances.subList(0, 5))));
        DescribeInstancesResult describeInstancesResult2 = new DescribeInstancesResult()
                .withReservations(List.of(new Reservation().withInstances(ec2Instances.subList(5, 10))));
        DescribeInstancesResult describeInstancesResult3 = new DescribeInstancesResult()
                .withReservations(List.of(new Reservation().withInstances(ec2Instances.subList(10, ec2Instances.size()))));

        when(ec2Client.describeInstances(any()))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2)
                .thenReturn(describeInstancesResult3);

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
                    new InstanceTemplate("flavor", "alma", (long) i, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
            resources.add(getCloudResource(String.valueOf(i), "instanceName" + i, anInstanceId, AWS_INSTANCE));
            cloudInstances.add(new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1"));
            ec2Instances.add(getAnInstance(anInstanceId));
        }
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        AmazonServiceException amazonServiceException = new AmazonServiceException("Instance with id could not be found: 'anInstanceId0'");
        amazonServiceException.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        DescribeInstancesResult describeInstancesResult1 = new DescribeInstancesResult()
                .withReservations(List.of(new Reservation().withInstances(ec2Instances.subList(1, 5))));
        AmazonServiceException amazonServiceException2 = new AmazonServiceException("Instance with id could not be found: 'anInstanceId5'");
        amazonServiceException2.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        DescribeInstancesResult describeInstancesResult2 = new DescribeInstancesResult()
                .withReservations(List.of(new Reservation().withInstances(ec2Instances.subList(6, 10))));
        DescribeInstancesResult describeInstancesResult3 = new DescribeInstancesResult()
                .withReservations(List.of(new Reservation().withInstances(ec2Instances.subList(10, ec2Instances.size()))));

        when(ec2Client.describeInstances(any()))
                .thenThrow(amazonServiceException)
                .thenReturn(describeInstancesResult1)
                .thenThrow(amazonServiceException2)
                .thenReturn(describeInstancesResult2)
                .thenReturn(describeInstancesResult3);

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
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance cloudInstance = new CloudInstance(anInstanceId, instanceTemplate, null, "subnet-123", "az1");
        List<CloudInstance> cloudInstances = List.of(cloudInstance);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(ec2Client);
        AmazonServiceException amazonServiceException = new AmazonServiceException("Something unexpected happened...");
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
                new InstanceTemplate("flavor", "alma", 1L, Set.of(), CREATED, Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
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
        LoadBalancerNotFoundException loadBalancerNotFoundException = new LoadBalancerNotFoundException("One or more elastic lb not found");
        loadBalancerNotFoundException.setErrorCode(LOAD_BALANCER_NOT_FOUND_ERROR_CODE);
        when(loadBalancingClient.describeLoadBalancers(any())).thenThrow(loadBalancerNotFoundException);

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources);

        verify(loadBalancingClient, times(1)).describeLoadBalancers(any());
        Assertions.assertTrue(cloudLoadBalancerMetadata.isEmpty());
    }

    @Test
    void collectLoadBalancerMetadataWhenOneOfSpecifiedArnsDoNotExist() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("aCrn", "lbname", null, ELASTIC_LOAD_BALANCER);
        CloudResource secondCloudResource = getCloudResource("secondCrn", "lbnamesecond", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource, secondCloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        LoadBalancerNotFoundException loadBalancerNotFoundException = new LoadBalancerNotFoundException("One or more elastic lb not found");
        loadBalancerNotFoundException.setErrorCode(LOAD_BALANCER_NOT_FOUND_ERROR_CODE);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setScheme(LoadBalancerSchemeEnum.Internal);
        when(loadBalancingClient.describeLoadBalancers(any()))
                .thenReturn(new DescribeLoadBalancersResult().withLoadBalancers(loadBalancer))
                .thenThrow(loadBalancerNotFoundException);

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources);

        verify(loadBalancingClient, times(2)).describeLoadBalancers(any());
        assertFalse(cloudLoadBalancerMetadata.isEmpty());
    }

    @Test
    void collectLoadBalancerMetadataWhenTheSpecifiedArnsExists() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("aCrn", "lbname", null, ELASTIC_LOAD_BALANCER);
        CloudResource secondCloudResource = getCloudResource("secondCrn", "lbnamesecond", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource, secondCloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setScheme(LoadBalancerSchemeEnum.Internal);
        when(loadBalancingClient.describeLoadBalancers(any()))
                .thenReturn(new DescribeLoadBalancersResult().withLoadBalancers(loadBalancer));

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadBalancerTypes, cloudResources);

        verify(loadBalancingClient, times(2)).describeLoadBalancers(any());
        assertFalse(cloudLoadBalancerMetadata.isEmpty());
        assertEquals(cloudResources.size(), cloudLoadBalancerMetadata.size());
    }

    @Test
    void collectLoadBalancerMetadataWhenWhenNotExpectedAmazonServiceExceptionOccurs() {
        List<LoadBalancerType> loadBalancerTypes = List.of();
        CloudResource cloudResource = getCloudResource("secondCrn", "secondInstanceName", null, ELASTIC_LOAD_BALANCER);
        List<CloudResource> cloudResources = List.of(cloudResource);
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(loadBalancingClient);
        LoadBalancerNotFoundException loadBalancerNotFoundException = new LoadBalancerNotFoundException("One or more elastic lb not found");
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

        assertThat(cloudConnectorException).hasMessage("Metadata collection of load balancers failed");
        assertThat(cloudConnectorException).hasCauseReference(runtimeException);

        verify(loadBalancingClient, times(1)).describeLoadBalancers(any());
    }

    private CloudResource getCloudResource(String reference, String name, String instanceId, ResourceType resourceType) {
        return new CloudResource.Builder()
                .type(resourceType)
                .reference(reference)
                .name(name)
                .instanceId(instanceId)
                .build();
    }

    private Instance getAnInstance(String instanceId) {
        return new Instance()
                .withInstanceId(instanceId)
                .withInstanceLifecycle(InstanceLifecycle.OnDemand.name())
                .withPublicIpAddress("52.0.0.0")
                .withPrivateIpAddress("10.0.0.0");
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