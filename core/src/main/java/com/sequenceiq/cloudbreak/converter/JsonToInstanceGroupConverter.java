package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway;

import java.util.Objects;

import javax.inject.Inject;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Component
public class JsonToInstanceGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupRequest, InstanceGroup> {

    @Inject
    private TemplateService templateService;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private TemplateValidator templateValidator;

    @Override
    public InstanceGroup convert(InstanceGroupRequest json) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(json.getGroup());
        instanceGroup.setNodeCount(json.getNodeCount());
        instanceGroup.setInstanceGroupType(json.getType());
        if (isGateway(instanceGroup.getInstanceGroupType()) && !Objects.equals(instanceGroup.getNodeCount(),
                instanceGroup.getInstanceGroupType().getFixedNodeCount())) {
            throw new BadRequestException(String.format("Gateway has to be exactly %s node.", instanceGroup.getInstanceGroupType().getFixedNodeCount()));
        }
        try {
            if (json.getSecurityGroupId() != null) {
                instanceGroup.setSecurityGroup(securityGroupService.get(json.getSecurityGroupId()));
            }
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to securitygroup '%s' is denied or securitygroup doesn't exist.",
                    json.getSecurityGroupId()), e);
        }
        try {
            if (json.getTemplateId() != null) {
                instanceGroup.setTemplate(templateService.get(json.getTemplateId()));
            }
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        if (json.getTemplate() != null) {
            Template template = getConversionService().convert(json.getTemplate(), Template.class);
            templateValidator.validateTemplateRequest(template);
            instanceGroup.setTemplate(template);
        }
        if (json.getSecurityGroup() != null) {
            instanceGroup.setSecurityGroup(getConversionService().convert(json.getSecurityGroup(), SecurityGroup.class));
        }
        try {
            Json jsonProperties = new Json(json.getParameters());
            instanceGroup.setAttributes(jsonProperties);
        } catch (JsonProcessingException e) {
            instanceGroup.setAttributes(null);
        }

        return instanceGroup;
    }
}
