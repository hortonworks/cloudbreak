package com.sequenceiq.cloudbreak.service.multiaz;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class DataLakeMultiAzCalculatorServiceTest {

    @Mock
    private MultiAzValidator multiAzValidator;

    @Spy
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @InjectMocks
    private DataLakeMultiAzCalculatorService dataLakeMultiAzCalculatorService;

    private String cloudSubnetAz(int i) {
        return "az-" + i;
    }

    private String cloudSubnetName(int i) {
        return "subnet-" + i;
    }

    private CloudSubnet cloudSubnet(int i) {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setId(String.valueOf(i));
        cloudSubnet.setName(cloudSubnetName(i));
        cloudSubnet.setAvailabilityZone(cloudSubnetAz(i));
        return cloudSubnet;
    }

    @BeforeEach
    void before() {
        dataLakeMultiAzCalculatorService = spy(new DataLakeMultiAzCalculatorService());
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(multiAzCalculatorService, "multiAzValidator", multiAzValidator);
    }

    @Test
    public void calculateByRoundRobinTestWhenSubnetAndAZAssignmentForEnterpriseThenShouldDistributeAuxiliaryGwNodesAcrossAzs() throws IOException {

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2),
                cloudSubnetName(3), cloudSubnet(3)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.ENTERPRISE));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("subnet-1", "subnet-2", "subnet-3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup gatewayGroup = getARequestGroup("gateway", 2, InstanceGroupType.GATEWAY);
        gatewayGroup.setInstanceGroupNetwork(network);
        InstanceGroup coreGroup = getARequestGroup("core", 3, InstanceGroupType.CORE);
        coreGroup.setInstanceGroupNetwork(network);
        InstanceGroup auxiliaryGroup = getARequestGroup("auxiliary", 1, InstanceGroupType.CORE);
        auxiliaryGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, auxiliaryGroup, idbrokerGroup, gatewayGroup));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        when(blueprintUtils.isEnterpriseDatalake(any(Stack.class))).thenReturn(true);

        Map<String, String> subnetAzPairs = multiAzCalculatorService.prepareSubnetAzMap(detailedEnvironmentResponse);

        dataLakeMultiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);

        List<Set<InstanceMetaData>> instanceMetaData = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> (instanceGroup.getGroupName().equalsIgnoreCase(InstanceGroupName.AUXILIARY.getName()) ||
                        instanceGroup.getGroupName().equalsIgnoreCase(InstanceGroupName.MASTER.getName())))
                .collect(Collectors.toList())
                .stream().map(InstanceGroup::getInstanceMetaData)
                .collect(Collectors.toList());

        List<InstanceMetaData> instanceMetaDataList = new ArrayList<>();
        instanceMetaData.forEach(data -> {
            instanceMetaDataList.addAll(data);
        });
        Assert.assertEquals(3, instanceMetaDataList.stream().map(InstanceMetaData::getAvailabilityZone)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertTrue(instanceMetaDataList.stream().map(InstanceMetaData::getAvailabilityZone)
                .distinct()
                .collect(Collectors.toList()).containsAll(List.of("az-1", "az-2", "az-3")));

        Assert.assertEquals(3, instanceMetaDataList.stream().map(InstanceMetaData::getSubnetId)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertTrue(instanceMetaDataList.stream().map(InstanceMetaData::getSubnetId)
                .distinct()
                .collect(Collectors.toList()).containsAll(List.of("subnet-1", "subnet-2", "subnet-3")));

        Assert.assertEquals(3, instanceMetaDataList.stream().map(InstanceMetaData::getRackId)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertTrue(instanceMetaDataList.stream().map(InstanceMetaData::getRackId)
                .distinct()
                .collect(Collectors.toList()).containsAll(List.of("/az-1", "/az-2", "/az-3")));

        verify(multiAzCalculatorService, times(4)).calculateByRoundRobin(anyMap(), any(InstanceGroupNetwork.class), anySet());
        verify(dataLakeMultiAzCalculatorService, times(1)).calculateByRoundRobinTreatingAuxiliaryAndMasterAsOne(any(), any(), any(), any());
        reset(dataLakeMultiAzCalculatorService);
        reset(multiAzCalculatorService);

        stack.setInstanceGroups(Set.of(auxiliaryGroup, gatewayGroup));
        dataLakeMultiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);
        ArgumentCaptor<Set<InstanceMetaData>> capture = ArgumentCaptor.forClass(Set.class);
        verify(multiAzCalculatorService, times(2)).calculateByRoundRobin(anyMap(), any(InstanceGroupNetwork.class), capture.capture());
        Assert.assertEquals(1, capture.getValue().size());
        capture.getValue().stream().allMatch(metaData -> metaData.getId() == 1L);
        reset(dataLakeMultiAzCalculatorService);
        reset(multiAzCalculatorService);

        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, idbrokerGroup, gatewayGroup));
        dataLakeMultiAzCalculatorService.calculateByRoundRobinTreatingAuxiliaryAndMasterAsOne(stack, subnetAzPairs, "sub1", "az-1");
        capture = ArgumentCaptor.forClass(Set.class);
        verify(multiAzCalculatorService, times(4)).calculateByRoundRobin(anyMap(), any(InstanceGroupNetwork.class), capture.capture());
        verify(multiAzCalculatorService, times(4)).prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(eq("sub1"),
                eq("az-1"), any(InstanceGroup.class), eq(stack));
        reset(dataLakeMultiAzCalculatorService);
        reset(multiAzCalculatorService);

        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, idbrokerGroup, auxiliaryGroup));
        dataLakeMultiAzCalculatorService.calculateByRoundRobinTreatingAuxiliaryAndMasterAsOne(stack, subnetAzPairs, "sub1", "az-1");
        capture = ArgumentCaptor.forClass(Set.class);
        verify(multiAzCalculatorService, times(3)).calculateByRoundRobin(anyMap(), any(InstanceGroupNetwork.class), capture.capture());
        verify(multiAzCalculatorService, times(4)).prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(eq("sub1"),
                eq("az-1"), any(InstanceGroup.class), eq(stack));
        reset(dataLakeMultiAzCalculatorService);
        reset(multiAzCalculatorService);

        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, idbrokerGroup));
        dataLakeMultiAzCalculatorService.calculateByRoundRobinTreatingAuxiliaryAndMasterAsOne(stack, subnetAzPairs, "sub1", "az-1");
        capture = ArgumentCaptor.forClass(Set.class);
        verify(multiAzCalculatorService, times(3)).calculateByRoundRobin(anyMap(), any(InstanceGroupNetwork.class), capture.capture());
        verify(multiAzCalculatorService, times(3)).prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(eq("sub1"),
                eq("az-1"), any(InstanceGroup.class), eq(stack));

    }

    @Test
    public void calculateByRoundRobinTestForEnterpriseThenShouldDistributeGwNodesAcrossAzs() throws IOException {

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2),
                cloudSubnetName(3), cloudSubnet(3)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.ENTERPRISE));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("subnet-1", "subnet-2", "subnet-3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup gatewayGroup = getARequestGroup("gateway", 2, InstanceGroupType.GATEWAY);
        gatewayGroup.setInstanceGroupNetwork(network);
        InstanceGroup coreGroup = getARequestGroup("core", 3, InstanceGroupType.CORE);
        coreGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, idbrokerGroup, gatewayGroup));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        when(blueprintUtils.isEnterpriseDatalake(any(Stack.class))).thenReturn(true);
        Map<String, String> subnetAzPairs = multiAzCalculatorService.prepareSubnetAzMap(detailedEnvironmentResponse);

        dataLakeMultiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);

        List<Set<InstanceMetaData>> instanceMetaData = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> (instanceGroup.getGroupName().equalsIgnoreCase(InstanceGroupName.AUXILIARY.getName()) ||
                        instanceGroup.getGroupName().equalsIgnoreCase(InstanceGroupName.MASTER.getName())))
                .collect(Collectors.toList())
                .stream().map(InstanceGroup::getInstanceMetaData)
                .collect(Collectors.toList());

        List<InstanceMetaData> instanceMetaDataList = new ArrayList<>();
        instanceMetaData.forEach(data -> {
            instanceMetaDataList.addAll(data);
        });
        Assert.assertEquals(2, instanceMetaDataList.stream().map(InstanceMetaData::getAvailabilityZone)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertEquals(2, instanceMetaDataList.stream().map(InstanceMetaData::getSubnetId)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertEquals(2, instanceMetaDataList.stream().map(InstanceMetaData::getRackId)
                .distinct()
                .collect(Collectors.toList()).size());
    }

    @Test
    public void calculateByRoundRobinTestWhenSubnetAndAZAssignmentForEnterpriseThenShouldDistributeAuxiliaryNodesAcrossAzs() throws IOException {

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2),
                cloudSubnetName(3), cloudSubnet(3)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.ENTERPRISE));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("subnet-1", "subnet-2", "subnet-3"))));
        InstanceGroup masterGroup = getARequestGroup(InstanceGroupName.MASTER.getName(), 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup auxiliaryGroup = getARequestGroup(InstanceGroupName.AUXILIARY.getName(), 1, InstanceGroupType.CORE);
        auxiliaryGroup.setInstanceGroupNetwork(network);
        InstanceGroup coreGroup = getARequestGroup(InstanceGroupName.CORE.getName(), 3, InstanceGroupType.CORE);
        coreGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup(InstanceGroupName.IDBROKER.getName(), 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, coreGroup, idbrokerGroup, auxiliaryGroup));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        when(blueprintUtils.isEnterpriseDatalake(any(Stack.class))).thenReturn(true);

        Map<String, String> subnetAzPairs = multiAzCalculatorService.prepareSubnetAzMap(detailedEnvironmentResponse);

        dataLakeMultiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);

        List<Set<InstanceMetaData>> instanceMetaData = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> (instanceGroup.getGroupName().equalsIgnoreCase(InstanceGroupName.AUXILIARY.getName()) ||
                        instanceGroup.getGroupName().equalsIgnoreCase(InstanceGroupName.MASTER.getName())))
                .collect(Collectors.toList())
                .stream().map(InstanceGroup::getInstanceMetaData)
                .collect(Collectors.toList());

        List<InstanceMetaData> instanceMetaDataList = new ArrayList<>();
        instanceMetaData.forEach(data -> {
            instanceMetaDataList.addAll(data);
        });
        Assert.assertEquals(3, instanceMetaDataList.stream().map(InstanceMetaData::getAvailabilityZone)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertEquals(3, instanceMetaDataList.stream().map(InstanceMetaData::getSubnetId)
                .distinct()
                .collect(Collectors.toList()).size());
        Assert.assertEquals(3, instanceMetaDataList.stream().map(InstanceMetaData::getRackId)
                .distinct()
                .collect(Collectors.toList()).size());
    }

    @Test
    void calculateByRoundRobinTestWhenSubnetAndAvZAndRackIdAndRoundRobinForLightDutyDL() throws IOException {

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2),
                cloudSubnetName(3), cloudSubnet(3)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.LIGHT_DUTY));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("subnet-1", "subnet-2", "subnet-3"))));
        InstanceGroup masterGroup = getARequestGroup(InstanceGroupName.MASTER.getName(), 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup(InstanceGroupName.IDBROKER.getName(), 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, idbrokerGroup));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        when(blueprintUtils.isEnterpriseDatalake(any(Stack.class))).thenReturn(false);

        Map<String, String> subnetAzPairs = multiAzCalculatorService.prepareSubnetAzMap(detailedEnvironmentResponse);
        dataLakeMultiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);
        verify(dataLakeMultiAzCalculatorService, times(0)).calculateByRoundRobinTreatingAuxiliaryAndMasterAsOne(any(), any(), anyString(), any());
        verify(multiAzCalculatorService, times(2)).calculateByRoundRobin(any(), any(InstanceGroupNetwork.class), any(Set.class));
        stack.getInstanceGroupsAsList().stream().forEach(instanceGroup -> {
            Assert.assertTrue(instanceGroup.getInstanceMetaData().iterator().next().getRackId() != null);
            Assert.assertTrue(instanceGroup.getInstanceMetaData().iterator().next().getSubnetId() != null);
            Assert.assertTrue(instanceGroup.getInstanceMetaData().iterator().next().getAvailabilityZone() != null);
        });
    }

    @Test
    void calculateByRoundRobinTestWhenSubnetAndAvZAndRackIdAndRoundRobinForMediumDutyDL() throws IOException {

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2),
                cloudSubnetName(3), cloudSubnet(3)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster(SdxClusterShape.MEDIUM_DUTY_HA));
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("subnet-1", "subnet-2", "subnet-3"))));
        InstanceGroup masterGroup = getARequestGroup("master", 2, InstanceGroupType.CORE);
        masterGroup.setInstanceGroupNetwork(network);
        InstanceGroup idbrokerGroup = getARequestGroup("idbroker", 2, InstanceGroupType.CORE);
        idbrokerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(masterGroup, idbrokerGroup));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        when(blueprintUtils.isEnterpriseDatalake(any(Stack.class))).thenReturn(false);
        Map<String, String> subnetAzPairs = multiAzCalculatorService.prepareSubnetAzMap(detailedEnvironmentResponse);
        dataLakeMultiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);
        verify(dataLakeMultiAzCalculatorService, times(0)).calculateByRoundRobinTreatingAuxiliaryAndMasterAsOne(any(), any(), anyString(), any());
        verify(multiAzCalculatorService, times(2)).calculateByRoundRobin(any(), any(InstanceGroupNetwork.class), any(Set.class));
        stack.getInstanceGroupsAsList().stream().forEach(instanceGroup -> {
            instanceGroup.getInstanceMetaData().stream().forEach(entry -> {
                Assert.assertTrue(entry.getRackId() != null);
            });
            instanceGroup.getInstanceMetaData().stream().forEach(entry -> {
                Assert.assertTrue(entry.getSubnetId() != null);
            });
            instanceGroup.getInstanceMetaData().stream().forEach(entry -> {
                Assert.assertTrue(entry.getAvailabilityZone() != null);
            });
        });
    }

    private Cluster createSdxCluster(SdxClusterShape shape) throws IOException {
        String template = null;
        Blueprint blueprint = new Blueprint();
        switch (shape) {
            case LIGHT_DUTY:
                template = "cdp-sdx";
                blueprint.setDescription("7.2.17 - SDX template with Atlas, HMS, Ranger and other services they are dependent on");
                break;
            case MEDIUM_DUTY_HA:
                template = "cdp-sdx-medium-ha";
                blueprint.setDescription(".2.17 - Medium SDX template with Atlas, HMS, Ranger and other services they are dependent on." +
                        "  Services like HDFS, HBASE, RANGER, HMS have HA");
                break;
            case ENTERPRISE:
                template = "cdp-sdx-enterprise";
                blueprint.setDescription(".2.17 - Enterprise SDX template with Atlas, HMS, Ranger and other services they are dependent on. " +
                        " Services like HDFS, HBASE, RANGER, HMS have HA");
                break;
            case MICRO_DUTY:
                template = "cdp-sdx-micro";
                blueprint.setDescription("7.2.17 - Micro SDX template with Atlas, HMS, Ranger and other services they are dependent on");
                break;
            default:
                template = "cdp-sdx";
        }
        blueprint.setBlueprintText(
                FileReaderUtils.readFileFromPath(
                        Path.of(
                                String.format("../core/src/main/resources/defaults/blueprints/7.2.17/%s.bp", template))));

        Cluster sdxCluster = new Cluster();
        sdxCluster.setBlueprint(blueprint);
        return sdxCluster;
    }

    private InstanceGroup getARequestGroup(String hostGroup, int numOfNodes, InstanceGroupType hostGroupType) {
        InstanceGroup requestHostGroup = new InstanceGroup();
        requestHostGroup.setGroupName(hostGroup);
        requestHostGroup.setInstanceGroupType(hostGroupType);
        requestHostGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
        Set<InstanceMetaData> instanceMetadata = new HashSet<>();
        IntStream.range(0, numOfNodes).forEach(count -> instanceMetadata.add(new InstanceMetaData()));
        if ("gateway".equals(hostGroup) || "auxiliary".equals(hostGroup)) {
            instanceMetadata.forEach(metadata -> metadata.setId(1L));
        }
        instanceMetadata.stream()
                .forEach(metadata -> {
                    metadata.setInstanceStatus(SERVICES_RUNNING);
                });
        requestHostGroup.setInstanceMetaData(instanceMetadata);
        return requestHostGroup;
    }
}
