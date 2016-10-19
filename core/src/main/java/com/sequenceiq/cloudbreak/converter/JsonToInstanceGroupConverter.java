package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway;

import java.util.Objects;

import javax.inject.Inject;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Component
public class JsonToInstanceGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupRequest, InstanceGroup> {

    @Inject
    private TemplateService templateService;

    @Inject
    private SecurityGroupService securityGroupService;

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
            instanceGroup.setTemplate(templateService.get(json.getTemplateId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        return instanceGroup;
    }
}
