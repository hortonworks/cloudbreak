package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.HashSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Component
public class InstanceGroupToInstanceGroupV4RequestConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupV4Request> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceGroupV4Request convert(InstanceGroup source) {
        InstanceGroupV4Request instanceGroupV2Request = new InstanceGroupV4Request();
        providerParameterCalculator.to(source.getAttributes().getMap(), instanceGroupV2Request);
        instanceGroupV2Request.setName(source.getGroupName());
        instanceGroupV2Request.setCount(source.getNodeCount());
        instanceGroupV2Request.setType(source.getInstanceGroupType());
        instanceGroupV2Request.setRecipeNames(new HashSet<>());
        instanceGroupV2Request.setTemplate(getConversionService().convert(source.getTemplate(), InstanceTemplateV4Request.class));
        instanceGroupV2Request.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupV4Request.class));
        return instanceGroupV2Request;
    }

}
