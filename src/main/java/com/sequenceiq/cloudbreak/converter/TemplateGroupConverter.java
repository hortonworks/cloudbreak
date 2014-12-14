package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateGroupJson;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Component
public class TemplateGroupConverter extends AbstractConverter<TemplateGroupJson, TemplateGroup> {
    @Autowired
    private TemplateRepository templateRepository;

    @Override
    public TemplateGroupJson convert(TemplateGroup entity) {
        TemplateGroupJson templateGroupJson = new TemplateGroupJson();
        templateGroupJson.setGroup(entity.getGroupName());
        templateGroupJson.setId(entity.getId());
        templateGroupJson.setNodeCount(Integer.parseInt(entity.getNodeCount().toString()));
        templateGroupJson.setTemplateId(entity.getTemplate().getId());
        return templateGroupJson;
    }

    @Override
    public TemplateGroup convert(TemplateGroupJson json) {
        TemplateGroup templateGroup = new TemplateGroup();
        templateGroup.setGroupName(json.getGroup());
        templateGroup.setNodeCount(Integer.valueOf(String.valueOf(json.getNodeCount())));
        try {
            templateGroup.setTemplate(templateRepository.findOne(Long.valueOf(json.getTemplateId())));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        return templateGroup;
    }

    public Set<TemplateGroup> convertAllJsonToEntity(Collection<TemplateGroupJson> jsonList, Stack stack) {
        Set<TemplateGroup> templateGroups = convertAllJsonToEntity(jsonList);
        for (TemplateGroup templateGroup : templateGroups) {
            templateGroup.setStack(stack);
        }
        return templateGroups;
    }
}
