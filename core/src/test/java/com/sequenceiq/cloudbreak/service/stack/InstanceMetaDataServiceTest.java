package com.sequenceiq.cloudbreak.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class InstanceMetaDataServiceTest {

    private static final int INSTANCE_GROUP_COUNT = 1;

    private static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final String RACK_ID = "/RACK_ID";

    private static final String HOSTNAME_1 = "host-1.foo.org";

    private static final String HOSTNAME_2 = "host-2.foo.org";

    private static final String HOSTNAME_3 = "host-3.foo.org";

    @Mock
    private InstanceMetaDataRepository repository;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @InjectMocks
    private InstanceMetaDataService underTest;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Captor
    private ArgumentCaptor<InstanceMetaData> instanceMetaDataCaptor;

    static Object[][] saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider() {
        return new Object[][]{
                // testCaseName save hostnames subnetId availabilityZone rackId
                {"save=false, hostnames=[], subnetId=SUBNET_ID, availabilityZone=null, rackId=null,", false, List.of(), SUBNET_ID, null, null},
                {"save=false, hostnames=[], subnetId=SUBNET_ID, availabilityZone=null, rackId=RACK_ID,", false, List.of(), SUBNET_ID, null, RACK_ID},
                {"save=false, hostnames=[], subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE, rackId=RACK_ID,", false, List.of(), SUBNET_ID,
                        AVAILABILITY_ZONE, RACK_ID},
                {"save=true, hostnames=[], subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE, rackId=RACK_ID,", true, List.of(), SUBNET_ID,
                        AVAILABILITY_ZONE, RACK_ID},
                {"save=false, hostnames=[HOSTNAME_1], subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE, rackId=RACK_ID,", false, List.of(HOSTNAME_1),
                        SUBNET_ID, AVAILABILITY_ZONE, RACK_ID},
                {"save=false, hostnames=[HOSTNAME_1, HOSTNAME_2], subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE, rackId=RACK_ID,", false,
                        List.of(HOSTNAME_1, HOSTNAME_2), SUBNET_ID, AVAILABILITY_ZONE, RACK_ID},
                {"save=false, hostnames=[HOSTNAME_1, HOSTNAME_2, HOSTNAME_3], subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE, rackId=RACK_ID,", false,
                        List.of(HOSTNAME_1, HOSTNAME_2, HOSTNAME_3), SUBNET_ID, AVAILABILITY_ZONE, RACK_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobin(String testCaseName, boolean save, List<String> hostnames,
            String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        Map<String, String> subnetAzPairs = Map.of();
        when(multiAzCalculatorService.prepareSubnetAzMap(environment)).thenReturn(subnetAzPairs);
        doAnswer(invocation -> {
            InstanceMetaData instanceMetaData = invocation.getArgument(2, InstanceMetaData.class);
            instanceMetaData.setSubnetId(subnetId);
            instanceMetaData.setAvailabilityZone(availabilityZone);
            return null;
        }).when(multiAzCalculatorService).calculateByRoundRobin(eq(subnetAzPairs), any(InstanceGroup.class), any(InstanceMetaData.class), any());
        when(multiAzCalculatorService.determineRackId(subnetId, availabilityZone)).thenReturn(rackId);

        Stack result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(0), 1), Map.of(groupName(0), new LinkedHashSet<>(hostnames)), save,
                false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        Set<InstanceGroup> resultInstanceGroups = result.getInstanceGroups();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, availabilityZone, rackId, null, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);
    }

    private void verifyInstances(Set<InstanceGroup> resultInstanceGroups, List<String> hostnames, String expectedSubnetId, String expectedAvailabilityZone,
            String expectedRackId, InstanceMetaData instanceToIgnore, int expectedInstanceCount) {
        AtomicInteger instanceCount = new AtomicInteger();
        resultInstanceGroups.forEach(instanceGroup -> {
            assertThat(instanceGroup.getAllInstanceMetaData()).isNotNull();
            String groupName = instanceGroup.getGroupName();
            int idx = indexFromGroupName(groupName);
            assertThat(idx).overridingErrorMessage("Could not determine index from invalid group name '%s'", groupName).isGreaterThanOrEqualTo(0);
            instanceGroup.getAllInstanceMetaData().forEach(instance -> {
                if (instance != instanceToIgnore) {
                    instanceCount.incrementAndGet();
                    assertThat(instance.getInstanceStatus()).isEqualTo(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                    assertThat(instance.getInstanceGroup()).isEqualTo(instanceGroup);
                    assertThat(instance.getDiscoveryFQDN()).isEqualTo(idx < hostnames.size() ? hostnames.get(idx) : null);
                    assertThat(instance.getSubnetId()).isEqualTo(expectedSubnetId);
                    assertThat(instance.getAvailabilityZone()).isEqualTo(expectedAvailabilityZone);
                    assertThat(instance.getRackId()).isEqualTo(expectedRackId);
                }
            });
        });
        assertThat(instanceCount.get()).isEqualTo(expectedInstanceCount);
    }

    private void verifyRepositorySave(Set<InstanceGroup> resultInstanceGroups, boolean save) {
        if (save) {
            verify(repository, times(INSTANCE_GROUP_COUNT)).save(instanceMetaDataCaptor.capture());
            List<InstanceMetaData> instancesCaptured = instanceMetaDataCaptor.getAllValues();
            assertThat(instancesCaptured).hasSize(INSTANCE_GROUP_COUNT);
            for (int i = 0; i < INSTANCE_GROUP_COUNT; i++) {
                InstanceMetaData instanceCaptured = instancesCaptured.get(i);
                InstanceGroup instanceGroup = findInstanceGroupByIndex(resultInstanceGroups, i);
                boolean found = instanceGroup.getAllInstanceMetaData().stream()
                        .anyMatch(instance -> instance == instanceCaptured);
                assertThat(found).overridingErrorMessage("Captured instance '%s' could not be found in group of index %d", instanceCaptured, i).isTrue();
            }
        } else {
            verify(repository, never()).save(any(InstanceMetaData.class));
        }
    }

    private InstanceGroup findInstanceGroupByIndex(Set<InstanceGroup> instanceGroups, int idx) {
        String groupName = groupName(idx);
        InstanceGroup result = instanceGroups.stream()
                .filter(instanceGroup -> groupName.equals(instanceGroup.getGroupName()))
                .findFirst().orElse(null);
        assertThat(result).overridingErrorMessage("Group '%s' not found in stack", groupName).isNotNull();
        return result;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinAndExistingInstance(String testCaseName, boolean save,
            List<String> hostnames, String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);

        InstanceMetaData existingInstance = new InstanceMetaData();
        existingInstance.setPrivateId(1234L);
        existingInstance.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING);
        existingInstance.setInstanceGroup(stack.getInstanceGroups().iterator().next());
        existingInstance.setDiscoveryFQDN("existing.foo.org");
        existingInstance.setSubnetId("subnetId-existing");
        existingInstance.setAvailabilityZone("availabilityZone-existing");
        existingInstance.setRackId("/rackId-existing");
        stack.getInstanceGroups().iterator().next().getAllInstanceMetaData().add(existingInstance);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        Map<String, String> subnetAzPairs = Map.of();
        when(multiAzCalculatorService.prepareSubnetAzMap(environment)).thenReturn(subnetAzPairs);
        doAnswer(invocation -> {
            InstanceMetaData instanceMetaData = invocation.getArgument(2, InstanceMetaData.class);
            instanceMetaData.setSubnetId(subnetId);
            instanceMetaData.setAvailabilityZone(availabilityZone);
            return null;
        }).when(multiAzCalculatorService).calculateByRoundRobin(eq(subnetAzPairs), any(InstanceGroup.class), any(InstanceMetaData.class), any());
        when(multiAzCalculatorService.determineRackId(subnetId, availabilityZone)).thenReturn(rackId);

        Stack result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), INSTANCE_GROUP_COUNT),
                Map.of(groupName(INSTANCE_GROUP_COUNT - 1), new LinkedHashSet<>(hostnames)), save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        Set<InstanceGroup> resultInstanceGroups = result.getInstanceGroups();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, availabilityZone, rackId, existingInstance, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);

        boolean foundExistingInstance = stack.getInstanceGroups().iterator().next().getAllInstanceMetaData().stream()
                .anyMatch(instance -> instance == existingInstance);
        assertThat(foundExistingInstance).overridingErrorMessage("Could not find existingInstance").isTrue();
        assertThat(existingInstance.getPrivateId()).isEqualTo(1234L);
        assertThat(existingInstance.getInstanceStatus()).isEqualTo(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING);
        assertThat(existingInstance.getInstanceGroup()).isEqualTo(stack.getInstanceGroups().iterator().next());
        assertThat(existingInstance.getDiscoveryFQDN()).isEqualTo("existing.foo.org");
        assertThat(existingInstance.getSubnetId()).isEqualTo("subnetId-existing");
        assertThat(existingInstance.getAvailabilityZone()).isEqualTo("availabilityZone-existing");
        assertThat(existingInstance.getRackId()).isEqualTo("/rackId-existing");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinAndCloudInstanceWithBadGroupName(String testCaseName,
            boolean save, List<String> hostnames, String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        Map<String, String> subnetAzPairs = Map.of();
        when(multiAzCalculatorService.prepareSubnetAzMap(environment)).thenReturn(subnetAzPairs);

        Stack result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(3), 1), Map.of(groupName(3), new LinkedHashSet<>(hostnames)),
                save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        Set<InstanceGroup> resultInstanceGroups = result.getInstanceGroups();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, availabilityZone, rackId, null, 0);
        verifyRepositorySave(resultInstanceGroups, false);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndStackFallback(String testCaseName, boolean save, List<String> hostnames,
            String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);
        Network network = new Network();
        network.setAttributes(Json.silent(subnetId == null ? Map.of() : Map.of("subnetId", subnetId)));
        stack.setNetwork(network);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        Map<String, String> subnetAzPairs = subnetId == null || availabilityZone == null ? Map.of() : Map.of(subnetId, availabilityZone);
        if (!hostnames.isEmpty()) {
            when(multiAzCalculatorService.prepareSubnetAzMap(environment, null)).thenReturn(subnetAzPairs);
        }
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(multiAzCalculatorService.prepareSubnetAzMap(environment)).thenReturn(subnetAzPairs);
        doNothing().when(multiAzCalculatorService).calculateByRoundRobin(eq(subnetAzPairs), any(InstanceGroup.class), any(InstanceMetaData.class), any());
        when(multiAzCalculatorService.determineRackId(subnetId, availabilityZone)).thenReturn(rackId);

        Stack result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), INSTANCE_GROUP_COUNT),
                Map.of(groupName(INSTANCE_GROUP_COUNT - 1), new LinkedHashSet<>(hostnames)), save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        Set<InstanceGroup> resultInstanceGroups = result.getInstanceGroups();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, availabilityZone, rackId, null, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);
    }

    @Test
    void saveInstanceAndGetUpdatedStackTestWhenNoCloudInstances() {
        Stack stack = stack(1);

        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(multiAzCalculatorService.prepareSubnetAzMap(environment)).thenReturn(Map.of());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        Stack result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), 0), Map.of(groupName(0), Set.of()), true,
                false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);

        Set<InstanceGroup> resultInstanceGroups = result.getInstanceGroups();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(1);
        verifyInstances(resultInstanceGroups, List.of(), null, null, null, null, 0);
        verifyRepositorySave(resultInstanceGroups, false);

        verify(multiAzCalculatorService, never()).calculateByRoundRobin(anyMap(), any(InstanceGroup.class), any(InstanceMetaData.class), any());
        verify(multiAzCalculatorService, never()).determineRackId(anyString(), anyString());
    }

    @Test
    public void testGetAzFromDiskOrNullIfRepairWhenRepairAndCloudPlatformSupported() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 10, "type");
        volumeSetAttributes.setDiscoveryFQDN("hostname");
        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.AWS_VOLUMESET)
                .status(CommonStatus.DETACHED)
                .name("name")
                .params(Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L, "ig"))
                .thenReturn(List.of(cloudResource));
        String actual = underTest.getAzFromDiskOrNullIfRepair(stack, true, "ig", "hostname");
        assertThat(actual).isEqualTo("az");
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L, "ig");
    }

    @Test
    public void testGetAzFromDiskOrNullIfRepairWhenRepairwhenCloudPlatformNotSupported() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform(CloudPlatform.GCP.name());
        String actual = underTest.getAzFromDiskOrNullIfRepair(stack, true, "ig", "hostname");
        assertThat(actual).isNull();
        verify(resourceRetriever, never()).findFirstByStatusAndTypeAndStack(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L);
    }

    @Test
    public void testGetAzFromDiskOrNullIfRepairWhenRepairAndCloudPlatformSupportedButNoVolumeWithHostName() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 10, "type");
        volumeSetAttributes.setDiscoveryFQDN("any-hostname");
        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.AWS_VOLUMESET)
                .status(CommonStatus.DETACHED)
                .name("name")
                .params(Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L, "ig"))
                .thenReturn(List.of(cloudResource));
        String actual = underTest.getAzFromDiskOrNullIfRepair(stack, true, "ig", "hostname");
        assertThat(actual).isNull();
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L, "ig");
    }

    private Stack stack(int instanceGroupCount) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Set<InstanceGroup> instanceGroups = new HashSet<>(instanceGroupCount);
        for (int i = 0; i < instanceGroupCount; i++) {
            instanceGroups.add(instanceGroup(i));
        }
        stack.setInstanceGroups(instanceGroups);
        return stack;
    }

    private InstanceGroup instanceGroup(int idx) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(groupName(idx));
        return instanceGroup;
    }

    private String groupName(int idx) {
        return "group-" + idx;
    }

    private int indexFromGroupName(String groupName) {
        int idx = -1;
        if (groupName.startsWith("group-")) {
            try {
                idx = Integer.parseInt(groupName.substring("group-".length()));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return idx;
    }

    private CloudInstance cloudInstance(int idx) {
        return new CloudInstance(instanceId(idx), instanceTemplate(idx), instanceAuthentication(), "dummySubnetId", "dummyAvailabilityZone");
    }

    private String instanceId(int idx) {
        return "instanceId-" + idx;
    }

    private InstanceTemplate instanceTemplate(int idx) {
        return new InstanceTemplate(null, groupName(idx), (long) idx, List.of(), com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED, Map.of(),
                (long) idx, null, null, 0L);
    }

    private InstanceAuthentication instanceAuthentication() {
        return new InstanceAuthentication(null, null, null);
    }

    private List<CloudInstance> cloudInstances(int instanceGroupCount) {
        return IntStream.range(0, instanceGroupCount)
                .mapToObj(this::cloudInstance)
                .collect(Collectors.toList());
    }

}