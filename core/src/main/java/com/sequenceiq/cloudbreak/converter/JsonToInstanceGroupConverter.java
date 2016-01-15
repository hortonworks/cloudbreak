package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway;

import javax.inject.Inject;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Component
public class JsonToInstanceGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupJson, InstanceGroup> {

    @Inject
    private TemplateService templateService;

    @Override
    public InstanceGroup convert(InstanceGroupJson json) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(json.getGroup());
        instanceGroup.setNodeCount(json.getNodeCount());
        instanceGroup.setInstanceGroupType(json.getType());
        if (isGateway(instanceGroup.getInstanceGroupType()) && instanceGroup.getNodeCount() != instanceGroup.getInstanceGroupType().getFixedNodeCount()) {
            throw new BadRequestException(String.format("Gateway has to be exactly %s node.", instanceGroup.getInstanceGroupType().getFixedNodeCount()));
        }
        try {
            instanceGroup.setTemplate(templateService.get(json.getTemplateId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        return instanceGroup;
    }
}
