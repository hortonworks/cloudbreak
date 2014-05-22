package com.sequenceiq.provisioning.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.StackJson;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.repository.TemplateRepository;

@Component
public class StackConverter extends AbstractConverter<StackJson, Stack> {

    @Autowired
    private TemplateRepository templateRepository;

    @Override
    public StackJson convert(Stack entity) {
        StackJson stackJson = new StackJson();
        stackJson.setTemplateId(entity.getTemplate().getId());
        stackJson.setClusterSize(entity.getClusterSize());
        stackJson.setCloudName(entity.getName());
        stackJson.setId(entity.getId());
        stackJson.setCloudPlatform(entity.getTemplate().cloudPlatform());
        return stackJson;
    }

    public StackJson convert(Stack entity, StackDescription description) {
        StackJson cloudInstanceJson = new StackJson();
        cloudInstanceJson.setTemplateId(entity.getTemplate().getId());
        cloudInstanceJson.setClusterSize(entity.getClusterSize());
        cloudInstanceJson.setId(entity.getId());
        cloudInstanceJson.setCloudPlatform(entity.getTemplate().cloudPlatform());
        cloudInstanceJson.setDescription(description);
        return cloudInstanceJson;
    }

    @Override
    public Stack convert(StackJson json) {
        Stack stack = new Stack();
        stack.setClusterSize(json.getClusterSize());
        stack.setName(json.getCloudName());
        stack.setTemplate(templateRepository.findOne(Long.valueOf(json.getTemplateId())));
        return stack;
    }
}
