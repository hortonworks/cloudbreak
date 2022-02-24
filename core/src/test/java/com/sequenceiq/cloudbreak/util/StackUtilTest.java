package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.common.api.type.ResourceType;

public class StackUtilTest {

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Captor
    private ArgumentCaptor<Set<Node>> nodesCaptor;

    @InjectMocks
    private final StackUtil stackUtil = new StackUtil();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetUptimeForClusterZero() {
        Cluster cluster = new Cluster();
        long uptime = stackUtil.getUptimeForCluster(cluster, true);
        assertEquals(0L, uptime);
    }

    @Test
    public void testGetUptimeForClusterNoGetUpSince() {
        Cluster cluster = new Cluster();
        int minutes = 10;
        cluster.setUptime(Duration.ofMinutes(minutes).toString());
        long uptime = stackUtil.getUptimeForCluster(cluster, false);
        assertEquals(Duration.ofMinutes(minutes).toMillis(), uptime);
    }

    @Test
    public void testGetUptimeForCluster() {
        Cluster cluster = new Cluster();
        int minutes = 10;
        cluster.setUptime(Duration.ofMinutes(minutes).toString());
        cluster.setUpSince(new Date().getTime());
        long uptime = stackUtil.getUptimeForCluster(cluster, true);
        assertTrue(uptime >= Duration.ofMinutes(minutes).toMillis());
    }

    @Test
    public void testGetCloudCredential() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("envCrn");
        CloudCredential cloudCredential = new CloudCredential("123", "CloudCred", "account");

        when(credentialClientService.getByEnvironmentCrn(anyString())).thenReturn(Credential.builder().build());
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(cloudCredential);

        CloudCredential result = stackUtil.getCloudCredential(stack);
        assertEquals(result.getId(), cloudCredential.getId());
        assertEquals(result.getName(), cloudCredential.getName());
    }

    @Test
    public void testCreateInstanceToVolumeInfoMapWhenEveryVolumeSetAreAttachedToInstance() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId"));
        volumeSets.add(getVolumeSetResource("secInstanceId"));
        volumeSets.add(getVolumeSetResource("thirdInstanceId"));

        Map<String, Map<String, Object>> actual = stackUtil.createInstanceToVolumeInfoMap(volumeSets);

        assertEquals(volumeSets.size(), actual.size());
    }

    @Test
    public void testCreateInstanceToVolumeInfoMapWhenNotEveryVolumeSetAreAttachedToInstance() {
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
    public void collectAndCheckReachableNodes() throws NodesUnreachableException {
        Stack stack = new Stack();
        Set<InstanceGroup> instanceGroupSet = new HashSet<>();
        InstanceGroup instanceGroup = new InstanceGroup();
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceGroup(instanceGroup);
        instanceMetaData1.setDiscoveryFQDN("node1.example.com");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaData2.setInstanceGroup(instanceGroup);
        instanceMetaData2.setDiscoveryFQDN("node2.example.com");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceGroup(instanceGroup);
        instanceMetaData3.setDiscoveryFQDN("node3.example.com");
        instanceMetaDataSet.add(instanceMetaData1);
        instanceMetaDataSet.add(instanceMetaData2);
        instanceMetaDataSet.add(instanceMetaData3);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
        Template template = new Template();
        template.setInstanceType("m5.xlarge");
        instanceGroup.setTemplate(template);
        instanceGroupSet.add(instanceGroup);
        stack.setInstanceGroups(instanceGroupSet);
        ArrayList<String> necessaryNodes = new ArrayList<>();
        necessaryNodes.add("node1.example.com");
        necessaryNodes.add("node3.example.com");

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        nodes.add(new Node("1.1.1.3", "1.1.1.3", "3", "m5.xlarge", "node3.example.com", "worker"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any())).thenReturn(new NodeReachabilityResult(nodes, Set.of()));

        stackUtil.collectAndCheckReachableNodes(stack, necessaryNodes);

        verify(hostOrchestrator).getResponsiveNodes(nodesCaptor.capture(), any());
        List<String> fqdns = nodesCaptor.getValue().stream().map(Node::getHostname).collect(Collectors.toList());
        assertTrue(fqdns.contains("node1.example.com"));
        assertFalse("Terminated node should be filtered out", fqdns.contains("node2.example.com"));
        assertTrue(fqdns.contains("node3.example.com"));
    }

    @Test
    public void collectAndCheckReachableNodesButSomeNodeMissing() {
        Stack stack = new Stack();
        Set<InstanceGroup> instanceGroupSet = new HashSet<>();
        InstanceGroup instanceGroup = new InstanceGroup();
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceGroup(instanceGroup);
        instanceMetaData1.setDiscoveryFQDN("node1.example.com");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaData2.setInstanceGroup(instanceGroup);
        instanceMetaData2.setDiscoveryFQDN("node2.example.com");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceGroup(instanceGroup);
        instanceMetaData3.setDiscoveryFQDN("node3.example.com");
        instanceMetaDataSet.add(instanceMetaData1);
        instanceMetaDataSet.add(instanceMetaData2);
        instanceMetaDataSet.add(instanceMetaData3);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
        Template template = new Template();
        template.setInstanceType("m5.xlarge");
        instanceGroup.setTemplate(template);
        instanceGroupSet.add(instanceGroup);
        stack.setInstanceGroups(instanceGroupSet);
        ArrayList<String> necessaryNodes = new ArrayList<>();
        necessaryNodes.add("node1.example.com");
        necessaryNodes.add("node3.example.com");

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any())).thenReturn(new NodeReachabilityResult(nodes, Set.of()));

        NodesUnreachableException nodesUnreachableException = Assertions.assertThrows(NodesUnreachableException.class,
                () -> stackUtil.collectAndCheckReachableNodes(stack, necessaryNodes));

        assertEquals(1, nodesUnreachableException.getUnreachableNodes().size());
        assertEquals("node3.example.com", nodesUnreachableException.getUnreachableNodes().iterator().next());
    }

    @Test
    public void collectReachableNodesTest() {
        Stack stack = new Stack();
        Set<InstanceGroup> instanceGroupSet = new HashSet<>();
        InstanceGroup instanceGroup = new InstanceGroup();
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceGroup(instanceGroup);
        instanceMetaData1.setDiscoveryFQDN("node1.example.com");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaData2.setInstanceGroup(instanceGroup);
        instanceMetaData2.setDiscoveryFQDN("node2.example.com");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceGroup(instanceGroup);
        instanceMetaData3.setDiscoveryFQDN("node3.example.com");
        instanceMetaDataSet.add(instanceMetaData1);
        instanceMetaDataSet.add(instanceMetaData2);
        instanceMetaDataSet.add(instanceMetaData3);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
        Template template = new Template();
        template.setInstanceType("m5.xlarge");
        instanceGroup.setTemplate(template);
        instanceGroupSet.add(instanceGroup);
        stack.setInstanceGroups(instanceGroupSet);

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("1.1.1.1", "1.1.1.1", "1", "m5.xlarge", "node1.example.com", "worker"));
        when(hostOrchestrator.getResponsiveNodes(nodesCaptor.capture(), any())).thenReturn(new NodeReachabilityResult(nodes, Set.of()));

        stackUtil.collectReachableNodes(stack);

        verify(hostOrchestrator).getResponsiveNodes(nodesCaptor.capture(), any());
        List<String> fqdns = nodesCaptor.getValue().stream().map(Node::getHostname).collect(Collectors.toList());
        assertTrue(fqdns.contains("node1.example.com"));
        assertFalse("Terminated node should be filtered out", fqdns.contains("node2.example.com"));
        assertTrue(fqdns.contains("node3.example.com"));
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
}
