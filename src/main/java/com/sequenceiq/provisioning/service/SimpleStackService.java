package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.StackJson;
import com.sequenceiq.provisioning.controller.json.StackResult;
import com.sequenceiq.provisioning.converter.StackConverter;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Template;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.StackRepository;
import com.sequenceiq.provisioning.repository.TemplateRepository;

@Service
public class SimpleStackService implements StackService {

    @Autowired
    private StackConverter stackConverter;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Resource
    private Map<CloudPlatform, ProvisionService> provisionServices;

    @Override
    public Set<StackJson> getAll(User user) {
        Set<StackJson> result = new HashSet<>();
        for (Stack cloudInstance : user.getStacks()) {
            CloudPlatform cp = cloudInstance.getTemplate().cloudPlatform();
            StackDescription description = provisionServices.get(cp).describeCloudInstance(user, cloudInstance);
            result.add(stackConverter.convert(cloudInstance, description));
        }
        return result;
    }

    @Override
    public StackJson get(User user, Long id) {
        Stack cloudInstance = stackRepository.findOne(id);
        if (cloudInstance == null) {
            throw new NotFoundException(String.format("CloudInstance '%s' not found", id));
        } else {
            CloudPlatform cp = cloudInstance.getTemplate().cloudPlatform();
            StackDescription description = provisionServices.get(cp).describeCloudInstanceWithResources(user, cloudInstance);
            return stackConverter.convert(cloudInstance, description);
        }
    }

    @Override
    public StackResult create(User user, StackJson stackRequest) {
        Template template = templateRepository.findOne(stackRequest.getTemplateId());
        if (template == null) {
            throw new EntityNotFoundException(String.format("Infrastructure '%s' not found", stackRequest.getTemplateId()));
        }
        Stack stack = stackConverter.convert(stackRequest);
        stack.setUser(user);
        stackRepository.save(stack);

        ProvisionService provisionService = provisionServices.get(template.cloudPlatform());
        return provisionService.createStack(user, stack);
    }

    @Override
    public void delete(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found.", id));
        }
        stackRepository.delete(stack);
    }

}
