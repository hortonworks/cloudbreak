package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.HashSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Component
public class InstanceGroupToInstanceGroupV4RequestConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupV4Request> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceGroupV4Request convert(InstanceGroup source) {
        InstanceGroupV4Request instanceGroupRequest = new InstanceGroupV4Request();
        providerParameterCalculator.parse(source.getAttributes().getMap(), instanceGroupRequest);
        instanceGroupRequest.setName(source.getGroupName());
        instanceGroupRequest.setNodeCount(source.getInitialNodeCount());
        instanceGroupRequest.setType(source.getInstanceGroupType());
        instanceGroupRequest.setRecipeNames(new HashSet<>());
        instanceGroupRequest.setMinimumNodeCount(source.getMinimumNodeCount());
        instanceGroupRequest.setTemplate(getConversionService().convert(source.getTemplate(), InstanceTemplateV4Request.class));
        instanceGroupRequest.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupV4Request.class));
        return instanceGroupRequest;
    }

}
