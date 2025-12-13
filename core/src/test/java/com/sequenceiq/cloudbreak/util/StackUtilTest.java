package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.ResourceType;

class StackUtilTest {

    private static final String ENV_CRN = "envCrn";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:default:cluster:0ba0ca99-e961-4c8d-b7e9-da0587cd40d0";

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialClientService credentialClientService;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Captor
    private ArgumentCaptor<Set<Node>> nodesCaptor;

    @InjectMocks
    private final StackUtil stackUtil = new StackUtil();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetUptimeForClusterZero() {
        Cluster cluster = new Cluster();
        long uptime = stackUtil.getUptimeForCluster(cluster, true);
        assertEquals(0L, uptime);
    }

    @Test
    void testGetUptimeForClusterNoGetUpSince() {
        Cluster cluster = new Cluster();
        int minutes = 10;
        cluster.setUptime(Duration.ofMinutes(minutes).toString());
        long uptime = stackUtil.getUptimeForCluster(cluster, false);
        assertEquals(Duration.ofMinutes(minutes).toMillis(), uptime);
    }

    @Test
    void testGetUptimeForCluster() {
        Cluster cluster = new Cluster();
        int minutes = 10;
        cluster.setUptime(Duration.ofMinutes(minutes).toString());
        cluster.setUpSince(new Date().getTime());
        long uptime = stackUtil.getUptimeForCluster(cluster, true);
        assertTrue(uptime >= Duration.ofMinutes(minutes).toMillis());
    }

    @Test
    void testGetCloudCredential() {
        CloudCredential cloudCredential = new CloudCredential("123", "CloudCred", "account");

        when(credentialClientService.getByEnvironmentCrn(anyString())).thenReturn(Credential.builder().build());
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(cloudCredential);

        CloudCredential result = stackUtil.getCloudCredential(ENV_CRN);
        assertEquals(result.getId(), cloudCredential.getId());
        assertEquals(result.getName(), cloudCredential.getName());
    }

    @Test
    void testCreateInstanceToVolumeInfoMapWhenEveryVolumeSetAreAttachedToInstance() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId"));
        volumeSets.add(getVolumeSetResource("secInstanceId"));
        volumeSets.add(getVolumeSetResource("thirdInstanceId"));

        Map<String, Map<String, Object>> actual = stackUtil.createInstanceToVolumeInfoMap(volumeSets);

        assertEquals(volumeSets.size(), actual.size());
    }

    @Test
    void testCreateInstanceToVolumeInfoMapWhenNotEveryVolumeSetAreAttachedToInstance() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId"));
        volumeSets.add(getVolumeSetResource("secInstanceId"));
        volumeSets.add(getVolumeSetResource("thirdInstanceId"));
        volumeSets.add(getVolumeSetResource(null));
        volumeSets.add(getVolumeSetResource(null));

        Map<String, Map<String, Object>> actual = stackUtil.createInstanceToVolumeInfoMap(volumeSets);

        int numberOfVolumeSetsWithoutInstanceReference = 2;
        assertEquals(volumeSets.size() - numberOfVolumeSetsWithoutInstanceReference, actual.size());
    }

    @Test
    void collectAndCheckReachableNodes() throws NodesUnreachableException {
        StackDto stack = mock(StackDto.class);
        mockGetInstanceGroups(stack);
        ArrayList<String> necessaryNodes = new ArrayList<>();
        necessaryNodes.add("node1.example.com");
        necessaryNodes.add("node3.example.com");

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        nodes.add(new Node("1.1.1.3", "1.1.1.3", "3", "m5.xlarge", "node3.example.com", "worker"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.FALSE))).thenReturn(new NodeReachabilityResult(nodes, Set.of()));

        stackUtil.collectReachableAndCheckNecessaryNodes(stack, necessaryNodes);

        verify(hostOrchestrator).getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.FALSE));
        List<String> fqdns = nodesCaptor.getValue().stream().map(Node::getHostname).collect(Collectors.toList());
        assertTrue(fqdns.contains("node1.example.com"));
        assertFalse(fqdns.contains("node2.example.com"), "Terminated node should be filtered out");
        assertTrue(fqdns.contains("node3.example.com"));
    }

    @Test
    void collectAndCheckReachableNodesButSomeNodeMissing() {
        StackDto stack = mock(StackDto.class);
        ArrayList<String> necessaryNodes = new ArrayList<>();
        necessaryNodes.add("node1.example.com");
        necessaryNodes.add("node3.example.com");

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.FALSE))).thenReturn(new NodeReachabilityResult(nodes, Set.of()));

        NodesUnreachableException nodesUnreachableException = assertThrows(NodesUnreachableException.class,
                () -> stackUtil.collectReachableAndCheckNecessaryNodes(stack, necessaryNodes));

        assertEquals(1, nodesUnreachableException.getUnreachableNodes().size());
        assertEquals("node3.example.com", nodesUnreachableException.getUnreachableNodes().iterator().next());
    }

    @Test
    void collectReachableNodesTest() {
        StackDto stackDto = mock(StackDto.class);
        mockGetInstanceGroups(stackDto);
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.FALSE))).thenReturn(new NodeReachabilityResult(nodes, Set.of()));

        stackUtil.collectReachableNodes(stackDto);

        verify(hostOrchestrator).getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.FALSE));
        List<String> fqdns = nodesCaptor.getValue().stream().map(Node::getHostname).collect(Collectors.toList());
        assertTrue(fqdns.contains("node1.example.com"));
        assertFalse(fqdns.contains("node2.example.com"), "Terminated node should be filtered out");
        assertTrue(fqdns.contains("node3.example.com"));
    }

    @Test
    void collectReachableAndUnreachableNodesTest() {
        StackDto stackDto = mock(StackDto.class);
        mockGetInstanceGroups(stackDto);
        Set<Node> reachableNodes = Set.of(
                new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        Set<Node> unreachableNodes = Set.of(
                new Node("1.1.1.1", "1.1.1.1", "2", "m5.xlarge", "node3.example.com", "master"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.TRUE))).thenReturn(
                new NodeReachabilityResult(reachableNodes, unreachableNodes));

        NodeReachabilityResult nodeReachabilityResult = stackUtil.collectReachableAndUnreachableCandidateNodes(stackDto,
                Set.of("node1.example.com", "node3.example.com"));

        verify(hostOrchestrator).getResponsiveNodes(nodesCaptor.capture(), any(), eq(Boolean.TRUE));
        List<String> fqdns = nodesCaptor.getValue().stream().map(Node::getHostname).collect(Collectors.toList());
        assertTrue(fqdns.contains("node1.example.com"));
        assertFalse(fqdns.contains("node2.example.com"), "Terminated node should be filtered out");
        assertTrue(fqdns.contains("node3.example.com"));
        assertTrue(nodeReachabilityResult.getReachableNodes().stream().map(Node::getHostname).collect(Collectors.toSet()).contains("node1.example.com"));
        assertTrue(nodeReachabilityResult.getUnreachableNodes().stream().map(Node::getHostname).collect(Collectors.toSet()).contains("node3.example.com"));
    }

    private void mockGetInstanceGroups(StackDto stackDto) {
        InstanceMetaData instanceMetaData1 = getInstanceMetaData("node1.example.com");
        InstanceMetaData instanceMetaData2 = getInstanceMetaData("node2.example.com");
        instanceMetaData2.setInstanceStatus(InstanceStatus.DELETED_BY_PROVIDER);
        InstanceMetaData instanceMetaData3 = getInstanceMetaData("node3.example.com");
        instanceMetaData3.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        InstanceGroup instanceGroup = getInstanceGroup();

        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(instanceGroup,
                List.of(instanceMetaData1, instanceMetaData2, instanceMetaData3))));
    }

    @Test
    void testCollectGatewayNodes() {
        StackDto stack = spy(StackDto.class);
        InstanceGroup instanceGroup = getInstanceGroup();
        InstanceMetaData instanceMetaData1 = getInstanceMetaData("node1.example.com");
        instanceMetaData1.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaData2 = getInstanceMetaData("node2.example.com");
        instanceMetaData2.setInstanceStatus(InstanceStatus.DELETED_BY_PROVIDER);
        instanceMetaData2.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaData3 = getInstanceMetaData("node3.example.com");
        instanceMetaData3.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        instanceMetaData3.setInstanceGroup(instanceGroup);

        List<InstanceMetadataView> instanceMetaDataList = List.of(instanceMetaData1, instanceMetaData2, instanceMetaData3);
        when(stack.getInstanceGroupByInstanceGroupName(instanceGroup.getGroupName())).thenReturn(new InstanceGroupDto(instanceGroup, instanceMetaDataList));
        when(stack.getNotDeletedInstanceMetaData()).thenReturn(instanceMetaDataList);
        Set<Node> result = stackUtil.collectGatewayNodes(stack);
        assertThat(result).hasSize(1);
        Node node = result.stream().findFirst().get();
        assertThat(node.getHostname()).isEqualTo("node3.example.com");
    }

    @Test
    void testStopStartScalingEntitlementEnabledForMock() {
        ReflectionTestUtils.setField(stackUtil, "skipStartStopEntitlementCheckPlatforms", Set.of("MOCK"));
        StackView stackView = mock(StackView.class);
        when(stackView.getResourceCrn()).thenReturn(DATAHUB_CRN);
        when(stackView.getCloudPlatform()).thenReturn("MOCK");
        boolean entitlementEnabled = stackUtil.stopStartScalingEntitlementEnabled(stackView);
        assertTrue(entitlementEnabled);
    }

    private InstanceMetaData getInstanceMetaData(String fqdn) {
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN(fqdn);
        return instanceMetaData1;
    }

    private InstanceGroup getInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setInstanceType("m5.xlarge");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

    private Resource getVolumeSetResource(String instanceID) {
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AZURE_VOLUMESET);
        resource.setInstanceId(instanceID);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes.Builder()
                .build();
        resource.setAttributes(new Json(volumeSetAttributes));
        return resource;
    }

    @Test
    void testCreateInstanceToVolumeInfoMapShouldPreserveVolumeOrder() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("instanceId1", List.of(
                new VolumeSetAttributes.Volume("vol-1", "/dev/sda", 100, "type", null),
                new VolumeSetAttributes.Volume("vol-2", "/dev/sdb", 100, "type", null)
        )));
        volumeSets.add(getVolumeSetResource("instanceId2", List.of(
                new VolumeSetAttributes.Volume("vol-3", "/dev/sdc", 100, "type", null),
                new VolumeSetAttributes.Volume("vol-4", "/dev/sdd", 100, "type", null)
        )));

        Map<String, Map<String, Object>> result = stackUtil.createInstanceToVolumeInfoMap(volumeSets);

        assertThat(new ArrayList<>(result.keySet())).containsExactly("instanceId1", "instanceId2");
        assertThat(result.get("instanceId1").get("dataVolumes")).isEqualTo("/dev/sda /dev/sdb");
        assertThat(result.get("instanceId1").get("serialIds")).isEqualTo("vol-1 vol-2");
        assertThat(result.get("instanceId2").get("dataVolumes")).isEqualTo("/dev/sdc /dev/sdd");
        assertThat(result.get("instanceId2").get("serialIds")).isEqualTo("vol-3 vol-4");
    }

    private Resource getVolumeSetResource(String instanceID, List<VolumeSetAttributes.Volume> volumes) {
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AZURE_VOLUMESET);
        resource.setInstanceId(instanceID);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes.Builder()
                .withVolumes(volumes)
                .build();
        resource.setAttributes(new Json(volumeSetAttributes));
        return resource;
    }
}
