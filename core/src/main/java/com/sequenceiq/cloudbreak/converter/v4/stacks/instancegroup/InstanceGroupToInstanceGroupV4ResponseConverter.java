package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network.InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupToSecurityGroupResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.TemplateToInstanceTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.ScalabilityOption;

@Component
public class InstanceGroupToInstanceGroupV4ResponseConverter {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private TemplateToInstanceTemplateV4ResponseConverter templateToInstanceTemplateV4ResponseConverter;

    @Inject
    private InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter instanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter;

    @Inject
    private SecurityGroupToSecurityGroupResponseConverter securityGroupToSecurityGroupResponseConverter;

    @Inject
    private InstanceMetaDataToInstanceMetaDataV4ResponseConverter instanceMetaDataToInstanceMetaDataV4ResponseConverter;

    public InstanceGroupV4Response convert(InstanceGroup source) {
        InstanceGroupV4Response instanceGroupResponse = new InstanceGroupV4Response();
        instanceGroupResponse.setId(source.getId());
        if (source.getTemplate() != null) {
            instanceGroupResponse.setTemplate(templateToInstanceTemplateV4ResponseConverter
                    .convert(source.getTemplate()));
        }
        instanceGroupResponse.setMetadata(
                source.getNotTerminatedInstanceMetaDataSet().stream()
                .map(s -> instanceMetaDataToInstanceMetaDataV4ResponseConverter.convert(s))
                .collect(Collectors.toSet()));
        if (source.getSecurityGroup() != null) {
            instanceGroupResponse.setSecurityGroup(securityGroupToSecurityGroupResponseConverter.convert(source.getSecurityGroup()));
        }
        Json attributes = source.getAttributes();
        if (attributes != null) {
            providerParameterCalculator.parse(attributes.getMap(), instanceGroupResponse);
        }
        instanceGroupResponse.setNodeCount(source.getNodeCount());
        instanceGroupResponse.setName(source.getGroupName());
        instanceGroupResponse.setMinimumNodeCount(source.getMinimumNodeCount());
        instanceGroupResponse.setType(source.getInstanceGroupType());
        if (source.getInstanceGroupNetwork() != null) {
            instanceGroupResponse.setNetwork(
                    instanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter.convert(source.getInstanceGroupNetwork()));
        }
        instanceGroupResponse.setAvailabilityZones(source.getAvailabilityZones());
        instanceGroupResponse.setScalabilityOption(source.getScalabilityOption() == null ? ScalabilityOption.ALLOWED : source.getScalabilityOption());
        return instanceGroupResponse;
    }
}
