package com.sequenceiq.periscope.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class MockStackResponseGenerator {
    private MockStackResponseGenerator() {
    }

    public static StackV4Response getMockStackV4Response(String clusterCrn, String hostGroup, String fqdnBase, int currentHostGroupCount,
            int unhealthyInstancesCount) {
        List<InstanceGroupV4Response> instanceGroupV4Responses = new ArrayList<>();

        InstanceMetaDataV4Response master1 = new InstanceMetaDataV4Response();
        master1.setDiscoveryFQDN("master1");
        master1.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
        master1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        master1.setInstanceId("test_instanceid" + "master1");
        instanceGroupV4Responses.add(instanceGroup("master", awsTemplate(), Set.of(master1)));

        InstanceMetaDataV4Response worker1 = new InstanceMetaDataV4Response();
        worker1.setDiscoveryFQDN("worker1");
        worker1.setInstanceId("test_instanceid" + "worker1");
        worker1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaDataV4Response worker2 = new InstanceMetaDataV4Response();
        worker2.setDiscoveryFQDN("worker2");
        worker2.setInstanceId("test_instanceid" + "worker2");
        worker2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceGroupV4Responses.add(instanceGroup("worker", awsTemplate(), Set.of(worker1, worker2)));

        Set fqdnToInstanceIds = new HashSet();

        for (int i = 1; i <= unhealthyInstancesCount; i++) {
            InstanceMetaDataV4Response metadataResponse = new InstanceMetaDataV4Response();
            metadataResponse.setDiscoveryFQDN(fqdnBase + i);
            metadataResponse.setInstanceId("test_instanceid_" + hostGroup + i);
            metadataResponse.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
            fqdnToInstanceIds.add(metadataResponse);
        }

        for (int i = 1; i <= currentHostGroupCount - unhealthyInstancesCount; i++) {
            InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
            metadata1.setDiscoveryFQDN(fqdnBase + (unhealthyInstancesCount + i));
            metadata1.setInstanceId("test_instanceid_" + hostGroup + (unhealthyInstancesCount + i));
            metadata1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            fqdnToInstanceIds.add(metadata1);
        }

        instanceGroupV4Responses.add(instanceGroup(hostGroup, awsTemplate(), fqdnToInstanceIds));

        StackV4Response mockReponse = new StackV4Response();
        mockReponse.setCrn(clusterCrn);
        mockReponse.setInstanceGroups(instanceGroupV4Responses);
        mockReponse.setNodeCount(instanceGroupV4Responses.stream().flatMap(ig -> ig.getMetadata().stream()).collect(Collectors.toSet()).size());
        mockReponse.setCloudPlatform(CloudPlatform.AWS);
        return mockReponse;
    }

    public static StackV4Response getMockStackV4ResponseWithStoppedAndRunningNodes(String clusterCrn, String hostGroup, String fqdnBase,
            int runningHostGroupNodeCount, int stoppedHostGroupNodeCount) {
        List<InstanceGroupV4Response> instanceGroupV4Responses = new ArrayList<>();
        InstanceMetaDataV4Response master1 = new InstanceMetaDataV4Response();
        master1.setDiscoveryFQDN("master1");
        master1.setInstanceId("test_instanceid" + "master1");
        master1.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
        master1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceGroupV4Responses.add(instanceGroup("master", awsTemplate(), Set.of(master1)));

        InstanceMetaDataV4Response worker1 = new InstanceMetaDataV4Response();
        worker1.setDiscoveryFQDN("worker1");
        worker1.setInstanceId("test_instanceid" + "worker1");
        worker1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaDataV4Response worker2 = new InstanceMetaDataV4Response();
        worker2.setDiscoveryFQDN("worker2");
        worker2.setInstanceId("test_instanceid" + "worker2");
        worker2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceGroupV4Responses.add(instanceGroup("worker", awsTemplate(), Set.of(worker1, worker2)));

        Set<InstanceMetaDataV4Response> instanceMetadata = new HashSet<>();

        int i;
        for (i = 0; i < runningHostGroupNodeCount; ++i) {
            InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
            metadata1.setDiscoveryFQDN(fqdnBase + i);
            metadata1.setInstanceId("test_instanceid_" + hostGroup + i);
            metadata1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            instanceMetadata.add(metadata1);
        }

        for (i = 0; i < stoppedHostGroupNodeCount; ++i) {
            InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
            metadata1.setDiscoveryFQDN(fqdnBase + runningHostGroupNodeCount + i);
            metadata1.setInstanceId("test_instanceid_" + hostGroup + (runningHostGroupNodeCount + i));
            metadata1.setInstanceStatus(InstanceStatus.STOPPED);
            instanceMetadata.add(metadata1);
        }

        instanceGroupV4Responses.add(instanceGroup(hostGroup, awsTemplate(), instanceMetadata));

        StackV4Response mockResponse = new StackV4Response();
        mockResponse.setCrn(clusterCrn);
        mockResponse.setInstanceGroups(instanceGroupV4Responses);
        mockResponse.setNodeCount(instanceGroupV4Responses.stream().flatMap(ig -> ig.getMetadata().stream()).collect(Collectors.toSet()).size());
        mockResponse.setCloudPlatform(CloudPlatform.AWS);
        return mockResponse;
    }

    public static StackV4Response getBasicMockStackResponse(Status clusterStatus) {
        StackV4Response stackResponse = new StackV4Response();
        stackResponse.setStatus(clusterStatus);
        ClusterV4Response clusterResponse = new ClusterV4Response();
        try {
            clusterResponse.setExtendedBlueprintText(getTestBP());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clusterResponse.setStatus(clusterStatus);
        stackResponse.setCluster(clusterResponse);
        return stackResponse;
    }

    private static String getTestBP() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/customde-test.json");
    }

    public static StackV4Response getMockStackResponseWithDependentHostGroup(Status clusterStatus,
            Set<String> dependentHostGroups, InstanceStatus instanceStatus) {
        StackV4Response stackResponse = new StackV4Response();
        List<InstanceGroupV4Response> instanceGroupV4Responses = new ArrayList<>();
        dependentHostGroups.forEach(hg -> {
            InstanceMetaDataV4Response metaData = new InstanceMetaDataV4Response();
            metaData.setDiscoveryFQDN("fqdn-" + hg);
            metaData.setInstanceId("test_instanceid" + hg);
            metaData.setInstanceGroup(hg);
            metaData.setInstanceStatus(instanceStatus);
            metaData.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
            instanceGroupV4Responses.add(createInstanceGroupResponseFromMetaData(hg, Set.of(metaData)));
        });
        stackResponse.setInstanceGroups(instanceGroupV4Responses);
        stackResponse.setStatus(clusterStatus);
        ClusterV4Response clusterResponse = new ClusterV4Response();
        try {
            clusterResponse.setExtendedBlueprintText(getTestBP());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clusterResponse.setStatus(clusterStatus);
        stackResponse.setCluster(clusterResponse);
        return stackResponse;
    }

    public static InstanceGroupV4Response createInstanceGroupResponseFromMetaData(String hostGroupName,
            Set<InstanceMetaDataV4Response> instanceMetaDataV4Responses) {
        InstanceGroupV4Response instanceGroup = new InstanceGroupV4Response();
        instanceGroup.setName(hostGroupName);
        instanceGroup.setMetadata(instanceMetaDataV4Responses);
        return instanceGroup;
    }

    public static InstanceTemplateV4Response awsTemplate() {
        InstanceTemplateV4Response awsTemplate = new InstanceTemplateV4Response();
        awsTemplate.setCloudPlatform(CloudPlatform.AWS);
        return awsTemplate;
    }

    public static InstanceGroupV4Response instanceGroup(String hostGroupName, InstanceTemplateV4Response template,
            Set<InstanceMetaDataV4Response> instanceMetaDataV4Responses) {
        InstanceGroupV4Response instanceGroup = new InstanceGroupV4Response();
        instanceGroup.setTemplate(template);
        instanceGroup.setName(hostGroupName);
        instanceGroup.setMetadata(instanceMetaDataV4Responses);
        return instanceGroup;
    }
}
