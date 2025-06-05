package com.sequenceiq.cloudbreak.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstanceMetaDataServiceTest {

    private static final int INSTANCE_GROUP_COUNT = 1;

    private static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final String RACK_ID = "/default-rack";

    private static final String HOSTNAME_1 = "host-1.foo.org";

    private static final String HOSTNAME_2 = "host-2.foo.org";

    private static final String HOSTNAME_3 = "host-3.foo.org";

    @Mock
    private InstanceMetaDataRepository repository;

    @Mock
    private EnvironmentService environmentClientService;

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
                {"save=false, hostnames=[], subnetId=null, availabilityZone=null, rackId=RACK_ID,", false, List.of(), null, null, RACK_ID},
                {"save=false, hostnames=[HOSTNAME_1], subnetId=null, availabilityZone=null, rackId=RACK_ID,", false, List.of(HOSTNAME_1),
                        null, null, RACK_ID},
                {"save=false, hostnames=[HOSTNAME_1, HOSTNAME_2], subnetId=null, availabilityZone=null, rackId=RACK_ID,", false,
                        List.of(HOSTNAME_1, HOSTNAME_2), null, null, RACK_ID},
                {"save=false, hostnames=[HOSTNAME_1, HOSTNAME_2, HOSTNAME_3], subnetId=null, availabilityZone=null, rackId=RACK_ID,", false,
                        List.of(HOSTNAME_1, HOSTNAME_2, HOSTNAME_3), null, null, RACK_ID},
        };
    }

    static Object[][] saveInstanceAndGetUpdatedStackTestWhenStackSubnetIdAndStackAzExist() {
        return new Object[][]{
                // testCaseName save stackSubnetId stackAz rackId
                {"save=true, stackSubnetId=subnet1, stackAz=az1, rackId=/az1,", true, "subnet1", "az1", "/az1"},
                {"save=true, stackSubnetId=subnet1, stackAz=null, rackId=/subnet1,", true, "subnet1", null, "/subnet1"},
                {"save=true, stackSubnetId=null, stackAz=null, rackId=RACK_ID,", true, null, null, RACK_ID},
        };
    }

    static Stream<Arguments> supportedProvidersWithVolumeResource() {
        return Stream.of(
                arguments(CloudPlatform.AWS, ResourceType.AWS_VOLUMESET),
                arguments(CloudPlatform.AZURE, ResourceType.AZURE_VOLUMESET)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobin(String testCaseName, boolean save, List<String> hostnames,
            String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);
        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(0), 1), Map.of(groupName(0),
                new LinkedHashSet<>(hostnames)), save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, availabilityZone, rackId, null, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneNotNeededAndRackIdAndRoundRobin(String testCaseName, boolean save,
            List<String> hostnames, String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);
        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(0), 1), Map.of(groupName(0),
                new LinkedHashSet<>(hostnames)), save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, null, rackId, null, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);
    }

    private void verifyInstances(List<InstanceGroupDto> resultInstanceGroups, List<String> hostnames, String expectedSubnetId, String expectedAvailabilityZone,
            String expectedRackId, InstanceMetaData instanceToIgnore, int expectedInstanceCount) {
        AtomicInteger instanceCount = new AtomicInteger();
        resultInstanceGroups.forEach(instanceGroup -> {
            assertThat(instanceGroup.getInstanceMetadataViews()).isNotNull();
            String groupName = instanceGroup.getInstanceGroup().getGroupName();
            int idx = indexFromGroupName(groupName);
            assertThat(idx).overridingErrorMessage("Could not determine index from invalid group name '%s'", groupName).isGreaterThanOrEqualTo(0);
            instanceGroup.getInstanceMetadataViews().forEach(instance -> {
                if (instance != instanceToIgnore) {
                    instanceCount.incrementAndGet();
                    assertThat(instance.getInstanceStatus()).isEqualTo(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                    assertThat(instance.getInstanceGroupId()).isEqualTo(instanceGroup.getInstanceGroup().getId());
                    assertThat(instance.getDiscoveryFQDN()).isEqualTo(idx < hostnames.size() ? hostnames.get(idx) : null);
                    assertThat(instance.getSubnetId()).isEqualTo(expectedSubnetId);
                    assertThat(instance.getAvailabilityZone()).isEqualTo(expectedAvailabilityZone);
                    assertThat(instance.getRackId()).isEqualTo(expectedRackId);
                    assertThat(instance.getProviderInstanceType()).isEqualTo("large");
                }
            });
        });
        assertThat(instanceCount.get()).isEqualTo(expectedInstanceCount);
    }

    private void verifyRepositorySave(List<InstanceGroupDto> resultInstanceGroups, boolean save) {
        if (save) {
            verify(repository, times(INSTANCE_GROUP_COUNT)).save(instanceMetaDataCaptor.capture());
            List<InstanceMetaData> instancesCaptured = instanceMetaDataCaptor.getAllValues();
            assertThat(instancesCaptured).hasSize(INSTANCE_GROUP_COUNT);
            for (int i = 0; i < INSTANCE_GROUP_COUNT; i++) {
                InstanceMetaData instanceCaptured = instancesCaptured.get(i);
                InstanceGroupDto instanceGroup = findInstanceGroupByIndex(resultInstanceGroups, i);
                boolean found = instanceGroup.getInstanceMetadataViews().stream()
                        .anyMatch(instance -> instance == instanceCaptured);
                assertThat(found).overridingErrorMessage("Captured instance '%s' could not be found in group of index %d", instanceCaptured, i).isTrue();
            }
        } else {
            verify(repository, never()).save(any(InstanceMetaData.class));
        }
    }

    private InstanceGroupDto findInstanceGroupByIndex(List<InstanceGroupDto> instanceGroups, int idx) {
        String groupName = groupName(idx);
        InstanceGroupDto result = instanceGroups.stream()
                .filter(instanceGroup -> groupName.equals(instanceGroup.getInstanceGroup().getGroupName()))
                .findFirst().orElse(null);
        assertThat(result).overridingErrorMessage("Group '%s' not found in stack", groupName).isNotNull();
        return result;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinDataProvider")
    void saveInstanceAndGetUpdatedStackTestWhenSubnetAndAvailabilityZoneAndRackIdAndRoundRobinAndExistingInstance(String testCaseName, boolean save,
            List<String> hostnames, String subnetId, String availabilityZone, String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);

        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
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

        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), INSTANCE_GROUP_COUNT),
                Map.of(groupName(INSTANCE_GROUP_COUNT - 1), new LinkedHashSet<>(hostnames)), save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
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
        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(3), 1), Map.of(groupName(3), new LinkedHashSet<>(hostnames)),
                save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
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
        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
        Network network = new Network();
        network.setAttributes(Json.silent(subnetId == null ? Map.of() : Map.of("subnetId", subnetId)));
        stack.setNetwork(network);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), INSTANCE_GROUP_COUNT),
                Map.of(groupName(INSTANCE_GROUP_COUNT - 1), new LinkedHashSet<>(hostnames)), save, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, hostnames, subnetId, availabilityZone, rackId, null, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceAndGetUpdatedStackTestWhenStackSubnetIdAndStackAzExist")
    void saveInstanceAndGetUpdatedStackTestWhenStackSubnetIdAndStackAzExist(String testCaseName, boolean save, String stackSubnetId, String stackAz,
            String rackId) {
        Stack stack = stack(INSTANCE_GROUP_COUNT);
        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
        Network network = new Network();
        if (stackSubnetId != null) {
            network.setAttributes(Json.silent(Map.of("subnetId", stackSubnetId)));
        }
        stack.setNetwork(network);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        if (stackSubnetId != null) {
            environmentResponse.setNetwork(EnvironmentNetworkResponse.builder()
                    .withSubnetMetas(Map.of(stackSubnetId, new CloudSubnet.Builder().name("subnet1").availabilityZone(stackAz).build()))
                    .build());
        }
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environmentResponse);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), INSTANCE_GROUP_COUNT),
                Map.of(groupName(INSTANCE_GROUP_COUNT - 1), Set.of()), true, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);
        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(INSTANCE_GROUP_COUNT);
        verifyInstances(resultInstanceGroups, List.of(), stackSubnetId, stackAz, rackId, null, INSTANCE_GROUP_COUNT);
        verifyRepositorySave(resultInstanceGroups, save);
    }

    @Test
    void saveInstanceAndGetUpdatedStackTestWhenNoCloudInstances() {
        Stack stack = stack(1);

        when(repository.findLastPrivateIdForStack(any(), any())).thenReturn(new PageImpl<>(List.of(0L)));
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackDtoDelegate result = underTest.saveInstanceAndGetUpdatedStack(stack, Map.of(groupName(INSTANCE_GROUP_COUNT - 1), 0), Map.of(groupName(0),
                Set.of()), true, false, NetworkScaleDetails.getEmpty());

        assertThat(result).isSameAs(stack);

        List<InstanceGroupDto> resultInstanceGroups = result.getInstanceGroupDtos();
        assertThat(resultInstanceGroups).isNotNull();
        assertThat(resultInstanceGroups).hasSize(1);
        verifyInstances(resultInstanceGroups, List.of(), null, null, null, null, 0);
        verifyRepositorySave(resultInstanceGroups, false);
    }

    @ParameterizedTest
    @MethodSource("supportedProvidersWithVolumeResource")
    void testGetAzFromDiskOrNullIfRepairWhenRepairAndCloudPlatformSupported(CloudPlatform cloudPlatform, ResourceType supportedVolumeResourceType) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform(cloudPlatform.name());
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 10, "type");
        volumeSetAttributes.setDiscoveryFQDN("hostname");
        CloudResource cloudResource = CloudResource.builder()
                .withType(supportedVolumeResourceType)
                .withStatus(CommonStatus.DETACHED)
                .withName("name")
                .withParameters(Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, supportedVolumeResourceType, 1L, "ig"))
                .thenReturn(List.of(cloudResource));
        String actual = underTest.getAvailabilityZoneFromDiskIfRepair(stack, true, "ig", "hostname");
        assertThat(actual).isEqualTo("az");
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, supportedVolumeResourceType, 1L, "ig");
    }

    @Test
    void testGetAzFromDiskOrNullIfRepairWhenRepairwhenCloudPlatformNotSupported() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform(CloudPlatform.GCP.name());
        String actual = underTest.getAvailabilityZoneFromDiskIfRepair(stack, true, "ig", "hostname");
        assertThat(actual).isNull();
        verify(resourceRetriever, never()).findByStatusAndTypeAndStack(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L);
    }

    @Test
    void testGetAzFromDiskOrNullIfRepairWhenRepairAndCloudPlatformSupportedButNoVolumeWithHostName() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 10, "type");
        volumeSetAttributes.setDiscoveryFQDN("any-hostname");
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.AWS_VOLUMESET)
                .withStatus(CommonStatus.DETACHED)
                .withName("name")
                .withParameters(Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L, "ig"))
                .thenReturn(List.of(cloudResource));
        String actual = underTest.getAvailabilityZoneFromDiskIfRepair(stack, true, "ig", "hostname");
        assertThat(actual).isNull();
        verify(resourceRetriever).findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED, ResourceType.AWS_VOLUMESET, 1L, "ig");
    }

    @Test
    void testAnyInstanceStoppedAndZeroStopped() {
        when(repository.countStoppedForStack(any())).thenReturn(0L);
        boolean anyInstanceStopped = underTest.anyInstanceStopped(1L);
        Assertions.assertFalse(anyInstanceStopped);
    }

    @Test
    void testAnyInstanceStoppedAnd2Stopped() {
        when(repository.countStoppedForStack(any())).thenReturn(2L);
        boolean anyInstanceStopped = underTest.anyInstanceStopped(1L);
        Assertions.assertTrue(anyInstanceStopped);
    }

    @Test
    void testGetFirstValidPrivateId() {
        when(repository.findLastPrivateIdForStack(1L, Pageable.ofSize(1))).thenReturn(new PageImpl<>(List.of(420L)));
        long result = underTest.getFirstValidPrivateId(1L);
        Assertions.assertEquals(421L, result);
    }

    @Test
    void testGetFirstValidPrivateIdWhenNoExistingPrivateId() {
        when(repository.findLastPrivateIdForStack(1L, Pageable.ofSize(1))).thenReturn(new PageImpl<>(List.of()));
        long result = underTest.getFirstValidPrivateId(1L);
        Assertions.assertEquals(0L, result);
    }

    private Stack stack(int instanceGroupCount) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Set<InstanceGroup> instanceGroups = new HashSet<>(instanceGroupCount);
        for (int i = 0; i < instanceGroupCount; i++) {
            instanceGroups.add(instanceGroup(i));
        }
        stack.setInstanceGroups(instanceGroups);
        stack.setMultiAz(false);
        return stack;
    }

    private InstanceGroup instanceGroup(int idx) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(groupName(idx));
        instanceGroup.setId((long) idx);
        Template template = new Template();
        template.setInstanceType("large");
        instanceGroup.setTemplate(template);
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
