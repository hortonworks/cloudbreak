package com.sequenceiq.freeipa.service.multiaz;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.AvailabilityZoneConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.CredentialService;

@ExtendWith(MockitoExtension.class)
class MultiAzCalculatorServiceTest {

    private static final String SUB_1 = "SUB1";

    private static final String SUB_2 = "SUB2";

    private static final Map<String, String> SUBNET_AZ_PAIRS = Map.of(SUB_1, "AZ1", SUB_2, "AZ2", "ONLYINAZMAP", "AZ3");

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private AvailabilityZoneConverter availabilityZoneConverter;

    @InjectMocks
    private MultiAzCalculatorService underTest;

    @Test
    public void testCalculateCurrentSubnetUsage() {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of(SUB_1, SUB_2, "ONLYINIG"))));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(SUB_1);
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceGroup.setInstanceMetaData(Set.of(createInstanceMetadata(SUB_1), createInstanceMetadata(SUB_1), createInstanceMetadata(SUB_2),
                createInstanceMetadata(null), createInstanceMetadata(" "), createInstanceMetadata("IGNORED"), deletedInstance));

        Map<String, Integer> result = underTest.calculateCurrentSubnetUsage(SUBNET_AZ_PAIRS, instanceGroup);

        assertEquals(2, result.size());
        assertEquals(2, result.get(SUB_1));
        assertEquals(1, result.get(SUB_2));
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligible() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.TRUE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(Map.of(SUB_1, 2, SUB_2, 1)), instanceMetaData, instanceGroup);

        assertEquals(SUB_2, instanceMetaData.getSubnetId());
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligibleValidatorReturnFalse() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.FALSE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(Map.of(SUB_1, 2, SUB_2, 1)), instanceMetaData, instanceGroup);

        assertNull(instanceMetaData.getSubnetId());
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligibleSubnetUsageEmpty() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(), instanceMetaData, instanceGroup);

        assertNull(instanceMetaData.getSubnetId());
    }

    @Test
    public void testUpdateSubnetIdForSingleInstanceIfEligibleDontModifyExisting() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.TRUE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setSubnetId("ASDF");

        underTest.updateSubnetIdForSingleInstanceIfEligible(SUBNET_AZ_PAIRS, new HashMap<>(Map.of(SUB_1, 2, SUB_2, 1)), instanceMetaData, instanceGroup);

        assertEquals("ASDF", instanceMetaData.getSubnetId());
    }

    @Test
    public void testCalculateRoundRobin() {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of(SUB_1, SUB_2, "ONLYINIG"))));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(SUB_1);
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceGroup.setInstanceMetaData(Set.of(createInstanceMetadata(null), createInstanceMetadata(null), createInstanceMetadata(null),
                createInstanceMetadata(SUB_2), createInstanceMetadata(null), createInstanceMetadata(" "), createInstanceMetadata("IGNORED"),
                deletedInstance));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)).thenReturn(Boolean.TRUE);

        underTest.calculateByRoundRobin(SUBNET_AZ_PAIRS, instanceGroup);

        assertEquals(3, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> SUB_1.equals(im.getSubnetId()) && "AZ1".equals(im.getAvailabilityZone())).count());
        assertEquals(2, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> SUB_2.equals(im.getSubnetId()) && "AZ2".equals(im.getAvailabilityZone())).count());
        assertEquals(3, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> SUB_2.equals(im.getSubnetId())).count());
        assertEquals(1, instanceGroup.getNotDeletedInstanceMetaDataSet().stream().filter(im -> "IGNORED".equals(im.getSubnetId())).count());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testPopulateAvailabilityZonesNonMultiAz() {
        AvailabilityZoneConnector availabilityZoneConnector = mock(AvailabilityZoneConnector.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        setUpForPopulateAvailabilityZones(stack, instanceGroup, availabilityZoneConnector, detailedEnvironmentResponse);
        stack.setMultiAz(false);

        underTest.populateAvailabilityZones(stack, detailedEnvironmentResponse, instanceGroup);

        verify(availabilityZoneConnector, times(0)).getAvailabilityZones(any(), any(), any(), any());
        verify(availabilityZoneConverter, times(0)).getAvailabilityZonesFromJsonAttributes(any());

        assertNull(instanceGroup.getInstanceGroupNetwork().getAttributes());
        assertNull(instanceGroup.getAvailabilityZones());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testPopulateAvailabilityZonesWithEnvironmentZonesNotAvailable() {
        AvailabilityZoneConnector availabilityZoneConnector = mock(AvailabilityZoneConnector.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.setMultiAz(true);
        stack.setPlatformvariant(AzureConstants.VARIANT.value());
        stack.setCloudPlatform(AzureConstants.PLATFORM.value());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        when(environmentNetworkResponse.getAvailabilityZones(CloudPlatform.AZURE)).thenReturn(Set.of());
        when(detailedEnvironmentResponse.getNetwork()).thenReturn(environmentNetworkResponse);
        when(detailedEnvironmentResponse.getName()).thenReturn("test-env");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.populateAvailabilityZones(stack, detailedEnvironmentResponse, instanceGroup));
        assertEquals("MultiAz is enabled but Availability Zones are not configured for environment test-env." +
                "Please modify the environment and configure Availability Zones", badRequestException.getMessage());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testPopulateAvailabilityZonesWithZonesinRequest() {
        AvailabilityZoneConnector availabilityZoneConnector = mock(AvailabilityZoneConnector.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        setUpForPopulateAvailabilityZones(stack, instanceGroup, availabilityZoneConnector, detailedEnvironmentResponse);
        when(availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(any())).thenReturn(Set.of("1", "2"));

        underTest.populateAvailabilityZones(stack, detailedEnvironmentResponse, instanceGroup);

        verify(availabilityZoneConnector, times(0)).getAvailabilityZones(any(), any(), any(), any());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testPopulateAvailabilityZonesWithAvailabilityZoneConnectorNotExist() {
        AvailabilityZoneConnector availabilityZoneConnector = mock(AvailabilityZoneConnector.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        setUpForPopulateAvailabilityZones(stack, instanceGroup, availabilityZoneConnector, detailedEnvironmentResponse);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(null);

        underTest.populateAvailabilityZones(stack, detailedEnvironmentResponse, instanceGroup);

        assertNull(instanceGroup.getInstanceGroupNetwork().getAttributes());
        assertNull(instanceGroup.getAvailabilityZones());
    }

    @Test
    public void testPopulateAvailabilityZonesWithZonesLessThanMinZones() {
        AvailabilityZoneConnector availabilityZoneConnector = mock(AvailabilityZoneConnector.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        setUpForPopulateAvailabilityZones(stack, instanceGroup, availabilityZoneConnector, detailedEnvironmentResponse);
        instanceGroup.getTemplate().setInstanceType("instance0");
        when(availabilityZoneConnector.getAvailabilityZones(any(), any(), any(), any())).thenReturn(Set.of());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.populateAvailabilityZones(stack, detailedEnvironmentResponse, instanceGroup));

        assertEquals("The westus2 region does not support Multi AZ configuration. " +
                "Please check https://learn.microsoft.com/en-us/azure/reliability/availability-zones-service-support " +
                "for more details. It is also possible that the given instance0 instances " +
                "on master group are not supported in any specified [1, 2, 3] zones.", badRequestException.getMessage());
        verify(availabilityZoneConnector).getAvailabilityZones(any(), any(), any(), any());
    }

    @Test
    public void testPopulateAvailabilityZones() {
        AvailabilityZoneConnector availabilityZoneConnector = mock(AvailabilityZoneConnector.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        setUpForPopulateAvailabilityZones(stack, instanceGroup, availabilityZoneConnector, detailedEnvironmentResponse);
        Json expectedAttribute = new Json(Map.of(NetworkConstants.AVAILABILITY_ZONES, Set.of("1", "2", "3")));
        when(availabilityZoneConverter.getJsonAttributesWithAvailabilityZones(Set.of("1", "2", "3"), null)).thenReturn(expectedAttribute);

        underTest.populateAvailabilityZones(stack, detailedEnvironmentResponse, instanceGroup);

        verify(availabilityZoneConnector).getAvailabilityZones(any(), any(), any(), any());

        assertEquals(expectedAttribute.getMap(), instanceGroup.getInstanceGroupNetwork().getAttributes().getMap());
        assertEquals(Set.of("1", "2", "3"),
                instanceGroup.getAvailabilityZones().stream().map(InstanceGroupAvailabilityZone::getAvailabilityZone).collect(Collectors.toSet()));
    }

    @Test
    public void testPopulateAvailabilityZonesForInstancesNoMultiAz() {
        Stack stack = new Stack();
        stack.setMultiAz(false);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroup.setTemplate(new Template());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(null, "instance-deleted");
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        Set<InstanceMetaData> instances = new HashSet<>();
        instances.add(deletedInstance);
        IntStream.range(1, 8).boxed().forEach(instanceId -> {
            instances.add(createInstanceMetadata(null, "instance" + instanceId));
        });
        instanceGroup.setInstanceMetaData(instances);
        underTest.populateAvailabilityZonesForInstances(stack, instanceGroup);
        verify(availabilityZoneConverter, times(0)).getAvailabilityZonesFromJsonAttributes(any());
        assertEquals(0, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(instance -> instance.getAvailabilityZone() != null)
                .count());
        assertNull(deletedInstance.getAvailabilityZone());
    }

    @Test
    public void testPopulateAvailabilityZonesForInstancesWithOnlyDeletedInstances() {
        Stack stack = new Stack();
        stack.setMultiAz(true);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        when(availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(null)).thenReturn(Set.of("1", "2", "3"));
        instanceGroup.setTemplate(new Template());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(null, "instance-deleted");
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        Set<InstanceMetaData> instances = new HashSet<>();
        instances.add(deletedInstance);
        instanceGroup.setInstanceMetaData(instances);
        underTest.populateAvailabilityZonesForInstances(stack, instanceGroup);
        assertEquals(0, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(instance -> instance.getAvailabilityZone() != null)
                .count());
        assertNull(deletedInstance.getAvailabilityZone());
    }

    @Test
    public void testPopulateAvailabilityZonesForInstancesWithFewInstancesHaveAzPopulated() {
        Stack stack = new Stack();
        stack.setMultiAz(true);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        when(availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(null)).thenReturn(Set.of("1", "2", "3"));
        instanceGroup.setTemplate(new Template());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(null, "instance-deleted");
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        Set<InstanceMetaData> instances = new HashSet<>();
        instances.add(deletedInstance);
        InstanceMetaData instanceMetaData0 = createInstanceMetadata(null, "instance-0");
        instanceMetaData0.setAvailabilityZone("1");
        instances.add(instanceMetaData0);
        InstanceMetaData instanceMetaData1 = createInstanceMetadata(null, "instance-0");
        instanceMetaData1.setAvailabilityZone("2");
        instances.add(instanceMetaData1);
        instances.add(createInstanceMetadata(null, "instance-2"));
        instances.add(createInstanceMetadata(null, "instance-3"));
        instances.add(createInstanceMetadata(null, "instance-4"));
        instances.add(createInstanceMetadata(null, "instance-5"));
        instanceGroup.setInstanceMetaData(instances);
        underTest.populateAvailabilityZonesForInstances(stack, instanceGroup);
        Map<String, Long> zoneToNodeCountMap = instanceGroup.getNotDeletedInstanceMetaDataSet().stream().map(instance -> instance.getAvailabilityZone()).collect(
                Collectors.groupingBy(
                        Function.identity(), Collectors.counting()
                )
        );
        assertEquals(6, instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .filter(instance -> instance.getAvailabilityZone() != null)
                .count());
        assertEquals(3, zoneToNodeCountMap.size());
        assertEquals(3, zoneToNodeCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() == 2).count());
        assertNull(deletedInstance.getAvailabilityZone());
    }

    static List<Object[]> numZonesAndNumInstances() {
        List<Object []> data = new ArrayList<>();
        IntStream.range(1, 4).forEach(zone -> {
            IntStream.range(0, 10).forEach(numInstance -> {
                data.add(new Object[]{zone, numInstance});
            });
        });
        return data;
    }

    @ParameterizedTest(name = "testPopulateAvailabilityZonesForInstancesWithNumZones{0}NumInstances{1}")
    @MethodSource("numZonesAndNumInstances")
    public void testPopulateAvailabilityZonesForInstances(final Integer numZones, final Integer numInstances) {
        Stack stack = new Stack();
        stack.setMultiAz(true);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        Set<String> availabilityZones = IntStream.range(1, numZones + 1).boxed()
                .map(zone -> String.valueOf(zone)).collect(Collectors.toSet());
        when(availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(null)).thenReturn(availabilityZones);
        instanceGroup.setTemplate(new Template());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(null, "instance-deleted");
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        Set<InstanceMetaData> instances = new HashSet<>();
        instances.add(deletedInstance);
        IntStream.range(1, numInstances + 1).boxed().forEach(instanceId -> {
            instances.add(createInstanceMetadata(null, "instance" + instanceId));
        });
        instanceGroup.setInstanceMetaData(instances);

        underTest.populateAvailabilityZonesForInstances(stack, instanceGroup);

        Map<String, Long> zoneToNodeCountMap = instanceGroup.getNotDeletedInstanceMetaDataSet().stream().map(instance -> instance.getAvailabilityZone()).collect(
                Collectors.groupingBy(
                        Function.identity(), Collectors.counting()
                )
        );
        assertEquals(Math.min(availabilityZones.size(), instanceGroup.getNotDeletedInstanceMetaDataSet().size()),
                zoneToNodeCountMap.size());
        if (numInstances >= numZones) {
            assertEquals(numZones - numInstances % numZones, zoneToNodeCountMap.entrySet().stream()
                    .filter(entry -> entry.getValue() == numInstances / numZones).count());
        }
        assertEquals(numInstances % numZones, zoneToNodeCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() == numInstances / numZones + 1).count());
        assertNull(deletedInstance.getAvailabilityZone());
    }

    private InstanceMetaData createInstanceMetadata(String subnetId, String instanceId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setSubnetId(subnetId);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    private InstanceMetaData createInstanceMetadata(String subnetId) {
        return createInstanceMetadata(subnetId, null);
    }

    private void setUpForPopulateAvailabilityZones(Stack stack, InstanceGroup instanceGroup, AvailabilityZoneConnector availabilityZoneConnector,
            DetailedEnvironmentResponse detailedEnvironmentResponse) {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroup.setTemplate(new Template());
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        InstanceMetaData deletedInstance = createInstanceMetadata(null);
        deletedInstance.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceGroup.setInstanceMetaData(Set.of(createInstanceMetadata(null), createInstanceMetadata(null), createInstanceMetadata(null),
                createInstanceMetadata(null), createInstanceMetadata(null), createInstanceMetadata(null), createInstanceMetadata(null),
                deletedInstance));
        stack.setMultiAz(true);
        stack.setPlatformvariant(AzureConstants.VARIANT.value());
        stack.setCloudPlatform(AzureConstants.PLATFORM.value());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(availabilityZoneConnector.getAvailabilityZones(any(), any(), any(), any())).thenReturn(Set.of("1", "2", "3"));
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        when(environmentNetworkResponse.getAvailabilityZones(CloudPlatform.AZURE)).thenReturn(Set.of("1", "2", "3"));
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(detailedEnvironmentResponse.getLocation()).thenReturn(locationResponse);
        when(locationResponse.getName()).thenReturn("westus2");

        when(detailedEnvironmentResponse.getNetwork()).thenReturn(environmentNetworkResponse);
    }
}
