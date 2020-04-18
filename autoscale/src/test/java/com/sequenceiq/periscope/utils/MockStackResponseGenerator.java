package com.sequenceiq.periscope.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class MockStackResponseGenerator {
    private MockStackResponseGenerator() {
    }

    public static StackV4Response getMockStackV4Response(String clusterCrn, Map<String, String> hostGroupInstanceTypes,
            Set<InstanceMetaDataV4Response> instanceMetaDataV4Responses) {
        StackV4Response mockReponse = new StackV4Response();

        List<InstanceGroupV4Response> instanceGroupV4Responses = new ArrayList<>();
        hostGroupInstanceTypes.keySet().stream().forEach(hostGroup -> {
            instanceGroupV4Responses.add(instanceGroup(hostGroup, awsTemplate(hostGroupInstanceTypes.get(hostGroup)),
                    instanceMetaDataV4Responses));
        });

        mockReponse.setCrn(clusterCrn);
        mockReponse.setInstanceGroups(instanceGroupV4Responses);
        mockReponse.setCloudPlatform(CloudPlatform.AWS);
        return mockReponse;
    }

    public static InstanceTemplateV4Response awsTemplate(String vmType) {
        InstanceTemplateV4Response awsTemplate = new InstanceTemplateV4Response();
        awsTemplate.setInstanceType(vmType);
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
