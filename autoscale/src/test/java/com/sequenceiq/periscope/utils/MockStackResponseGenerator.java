package com.sequenceiq.periscope.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            boolean withUnhealthyInstances) {
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
        if (!withUnhealthyInstances) {
            for (int i = 1; i <= currentHostGroupCount; i++) {
                InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
                metadata1.setDiscoveryFQDN(fqdnBase + i);
                metadata1.setInstanceId("test_instanceid_" + hostGroup + i);
                metadata1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
                fqdnToInstanceIds.add(metadata1);
            }
        } else {
            // Add nodes with InstanceStatuses STOPPED and SERVICES_UNHEALTHY
            List<InstanceStatus> unhealthyStatuses = List.of(InstanceStatus.STOPPED, InstanceStatus.SERVICES_UNHEALTHY);
            for (InstanceStatus status: unhealthyStatuses) {
                InstanceMetaDataV4Response metadataResponse = new InstanceMetaDataV4Response();
                metadataResponse.setDiscoveryFQDN(fqdnBase + unhealthyStatuses.indexOf(status));
                metadataResponse.setInstanceId("test_instanceid_" + hostGroup + unhealthyStatuses.indexOf(status));
                metadataResponse.setInstanceStatus(status);
                fqdnToInstanceIds.add(metadataResponse);
            }
            // Add remaining healthy instances
            for (int i = unhealthyStatuses.size(); i < currentHostGroupCount; ++i) {
                InstanceMetaDataV4Response metadataResponse = new InstanceMetaDataV4Response();
                metadataResponse.setDiscoveryFQDN(fqdnBase + i);
                metadataResponse.setInstanceId("test_instanceid_" + hostGroup + i);
                metadataResponse.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
                fqdnToInstanceIds.add(metadataResponse);
            }
        }
        instanceGroupV4Responses.add(instanceGroup(hostGroup, awsTemplate(), fqdnToInstanceIds));

        StackV4Response mockReponse = new StackV4Response();
        mockReponse.setCrn(clusterCrn);
        mockReponse.setInstanceGroups(instanceGroupV4Responses);
        mockReponse.setCloudPlatform(CloudPlatform.AWS);
        return mockReponse;
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
