package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Component
public class StackConverter extends AbstractConverter<StackJson, Stack> {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ClusterConverter clusterConverter;

    @Override
    public StackJson convert(Stack entity) {
        StackJson stackJson = new StackJson();
        stackJson.setTemplateId(entity.getTemplate().getId());
        stackJson.setNodeCount(entity.getNodeCount());
        stackJson.setName(entity.getName());
        stackJson.setId(entity.getId());
        stackJson.setCloudPlatform(entity.getTemplate().cloudPlatform());
        stackJson.setCredentialId(entity.getCredential().getId());
        stackJson.setStatus(entity.getStatus());
        stackJson.setAmbariServerIp(entity.getAmbariIp());
        if (entity.getCluster() != null) {
            stackJson.setCluster(clusterConverter.convert(entity.getCluster(), "{}"));
        }
        return stackJson;
    }

    public StackJson convert(Stack entity, StackDescription description) {
        StackJson stackJson = new StackJson();
        stackJson.setTemplateId(entity.getTemplate().getId());
        stackJson.setNodeCount(entity.getNodeCount());
        stackJson.setId(entity.getId());
        stackJson.setName(entity.getName());
        stackJson.setCredentialId(entity.getCredential().getId());
        stackJson.setCloudPlatform(entity.getTemplate().cloudPlatform());
        stackJson.setDescription(description);
        stackJson.setStatus(entity.getStatus());
        stackJson.setAmbariServerIp(entity.getAmbariIp());
        if (entity.getCluster() != null) {
            stackJson.setCluster(clusterConverter.convert(entity.getCluster(), "{}"));
        }
        return stackJson;
    }

    @Override
    public Stack convert(StackJson json) {
        Stack stack = new Stack();
        stack.setNodeCount(json.getNodeCount());
        stack.setName(json.getName());
        stack.setCredential(credentialRepository.findOne(json.getCredentialId()));
        stack.setTemplate(templateRepository.findOne(Long.valueOf(json.getTemplateId())));
        stack.setStatus(Status.CREATE_IN_PROGRESS);
        return stack;
    }
}
