package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateWay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Component
public class JsonToInstanceGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupJson, InstanceGroup> {

    @Autowired
    private TemplateRepository templateRepository;

    @Override
    public InstanceGroup convert(InstanceGroupJson json) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(json.getGroup());
        instanceGroup.setNodeCount(json.getNodeCount());
        instanceGroup.setInstanceGroupType(json.getType());
        if (isGateWay(instanceGroup.getInstanceGroupType()) && instanceGroup.getNodeCount() != instanceGroup.getInstanceGroupType().getFixedNodeCount()) {
            throw new BadRequestException(String.format("Gateway has to be exactly %s node.", instanceGroup.getInstanceGroupType().getFixedNodeCount()));
        }
        try {
            instanceGroup.setTemplate(templateRepository.findOne(json.getTemplateId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        return instanceGroup;
    }
}
