package com.sequenceiq.cloudbreak.converter.v2.cli;

import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class InstanceGroupToInstanceGroupV2RequestConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupToInstanceGroupV2RequestConverter.class);

    @Override
    public InstanceGroupV2Request convert(InstanceGroup source) {
        InstanceGroupV2Request instanceGroupV2Request = new InstanceGroupV2Request();
        instanceGroupV2Request.setParameters(source.getAttributes().getMap());
        instanceGroupV2Request.setGroup(source.getGroupName());
        instanceGroupV2Request.setNodeCount(source.getNodeCount());
        instanceGroupV2Request.setType(source.getInstanceGroupType());
        instanceGroupV2Request.setRecipeNames(new HashSet<>());
        instanceGroupV2Request.setTemplate(getConversionService().convert(source.getTemplate(), TemplateV2Request.class));
        instanceGroupV2Request.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupV2Request.class));
        return instanceGroupV2Request;
    }

}
