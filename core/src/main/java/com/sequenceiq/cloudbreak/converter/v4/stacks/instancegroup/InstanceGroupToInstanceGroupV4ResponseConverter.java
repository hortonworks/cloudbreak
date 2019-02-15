package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

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
        instanceGroupResponse.setMetadata(converterUtil.convertAllAsSet(source.getNotDeletedInstanceMetaDataSet(), InstanceMetaDataV4Response.class));
        if (source.getSecurityGroup() != null) {
            instanceGroupResponse.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupV4Response.class));
        }
        Json attributes = source.getAttributes();
        if (attributes != null) {
            providerParameterCalculator.parse(attributes.getMap(), instanceGroupResponse);
        }
        instanceGroupResponse.setNodeCount(source.getNodeCount());
        instanceGroupResponse.setName(source.getGroupName());
        return instanceGroupResponse;
    }
}
