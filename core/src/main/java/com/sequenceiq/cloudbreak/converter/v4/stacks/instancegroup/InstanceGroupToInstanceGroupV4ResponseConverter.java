package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.network.InstanceGroupNetworkV4Response;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.ScalabilityOption;

@Component
public class InstanceGroupToInstanceGroupV4ResponseConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupV4Response> {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceGroupV4Response convert(InstanceGroup source) {
        InstanceGroupV4Response instanceGroupResponse = new InstanceGroupV4Response();
        instanceGroupResponse.setId(source.getId());
        if (source.getTemplate() != null) {
            instanceGroupResponse.setTemplate(getConversionService().convert(source.getTemplate(), InstanceTemplateV4Response.class));
        }
        instanceGroupResponse.setMetadata(converterUtil.convertAllAsSet(source.getNotTerminatedInstanceMetaDataSet(), InstanceMetaDataV4Response.class));
        if (source.getSecurityGroup() != null) {
            instanceGroupResponse.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupV4Response.class));
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
                    getConversionService().convert(source.getInstanceGroupNetwork(), InstanceGroupNetworkV4Response.class));
        }
        instanceGroupResponse.setAvailabilityZones(source.getAvailabilityZones());
        instanceGroupResponse.setScalabilityOption(source.getScalabilityOption() == null ? ScalabilityOption.ALLOWED : source.getScalabilityOption());
        return instanceGroupResponse;
    }
}
