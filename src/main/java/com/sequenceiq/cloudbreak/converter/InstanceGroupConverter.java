package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Component
public class InstanceGroupConverter extends AbstractConverter<InstanceGroupJson, InstanceGroup> {
    @Autowired
    private TemplateRepository templateRepository;

    @Override
    public InstanceGroupJson convert(InstanceGroup entity) {
        InstanceGroupJson instanceGroupJson = new InstanceGroupJson();
        instanceGroupJson.setGroup(entity.getGroupName());
        instanceGroupJson.setId(entity.getId());
        instanceGroupJson.setNodeCount(entity.getNodeCount());
        instanceGroupJson.setTemplateId(entity.getTemplate().getId());
        return instanceGroupJson;
    }

    @Override
    public InstanceGroup convert(InstanceGroupJson json) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(json.getGroup());
        instanceGroup.setNodeCount(json.getNodeCount());
        if (!json.getGroup().contains("slave") && json.getNodeCount() != 1) {
            throw new BadRequestException("If you have a master hostgroup than the count of the nodes has to be 1 on master group.");
        }
        try {
            instanceGroup.setTemplate(templateRepository.findOne(json.getTemplateId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        return instanceGroup;
    }

    public Set<InstanceGroup> convertAllJsonToEntity(Collection<InstanceGroupJson> jsonList, Stack stack) {
        Set<InstanceGroup> instanceGroups = convertAllJsonToEntity(jsonList);
        for (InstanceGroup instanceGroup : instanceGroups) {
            instanceGroup.setStack(stack);
        }
        return instanceGroups;
    }
}
