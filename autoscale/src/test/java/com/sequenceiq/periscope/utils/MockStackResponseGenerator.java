package com.sequenceiq.periscope.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class MockStackResponseGenerator {
    private MockStackResponseGenerator() {
    }

    public static StackV4Response getMockStackV4Response(String clusterCrn, String hostGroup, String fqdnBase, int currentHostGroupCount,
            int unhealthyInstancesCount) {
        List<InstanceGroupV4Response> instanceGroupV4Responses = new ArrayList<>();

        InstanceMetaDataV4Response master1 = new InstanceMetaDataV4Response();
        master1.setDiscoveryFQDN("master1");
        master1.setInstanceId("test_instanceid" + "master1");
        instanceGroupV4Responses.add(instanceGroup("master", awsTemplate(), Set.of(master1)));

        InstanceMetaDataV4Response worker1 = new InstanceMetaDataV4Response();
        worker1.setDiscoveryFQDN("worker1");
        worker1.setInstanceId("test_instanceid" + "worker1");
        InstanceMetaDataV4Response worker2 = new InstanceMetaDataV4Response();
        worker2.setDiscoveryFQDN("worker2");
        worker2.setInstanceId("test_instanceid" + "worker2");
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
        instanceGroupV4Responses.add(instanceGroup("master", awsTemplate(), Set.of(master1)));

        InstanceMetaDataV4Response worker1 = new InstanceMetaDataV4Response();
        worker1.setDiscoveryFQDN("worker1");
        worker1.setInstanceId("test_instanceid" + "worker1");
        InstanceMetaDataV4Response worker2 = new InstanceMetaDataV4Response();
        worker2.setDiscoveryFQDN("worker2");
        worker2.setInstanceId("test_instanceid" + "worker2");
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
