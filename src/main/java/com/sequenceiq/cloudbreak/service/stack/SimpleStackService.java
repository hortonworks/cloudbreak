package com.sequenceiq.cloudbreak.service.stack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.MetaDataJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.converter.MetaDataConverter;
import com.sequenceiq.cloudbreak.converter.StackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Service
public class SimpleStackService implements StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStackService.class);

    @Autowired
    private StackConverter stackConverter;

    @Autowired
    private MetaDataConverter metaDataConverter;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Resource
    private Map<CloudPlatform, ProvisionService> provisionServices;

    @Override
    public Set<StackJson> getAll(User user) {
        Set<StackJson> result = new HashSet<>();
        for (Stack stack : user.getStacks()) {
            CloudPlatform cp = stack.getTemplate().cloudPlatform();
            StackDescription description = provisionServices.get(cp).describeStack(user, stack, stack.getCredential());
            result.add(stackConverter.convert(stack, description));
        }
        return result;
    }

    @Override
    public StackJson get(User user, Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        CloudPlatform cp = stack.getTemplate().cloudPlatform();
        StackDescription description = provisionServices.get(cp).describeStackWithResources(user, stack, stack.getCredential());
        return stackConverter.convert(stack, description);
    }

    @Override
    public IdJson create(User user, StackJson stackRequest) {
        LOGGER.info("Stack creation requested. [CredentialId: {}, Name: {}, Node count: {}, TemplateId: {}]",
                stackRequest.getCredentialId(),
                stackRequest.getName(),
                stackRequest.getNodeCount(),
                stackRequest.getTemplateId());
        Stack stack = stackConverter.convert(stackRequest);
        Template template = templateRepository.findOne(stackRequest.getTemplateId());
        stack.setUser(user);
        stack = stackRepository.save(stack);
        ProvisionService provisionService = provisionServices.get(template.cloudPlatform());
        provisionService.createStack(user, stack, stack.getCredential());
        return new IdJson(stack.getId());
    }

    @Override
    public void delete(User user, Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        CloudPlatform cp = stack.getTemplate().cloudPlatform();
        provisionServices.get(cp).deleteStack(user, stack, stack.getCredential());
        stackRepository.delete(id);
    }

    @Override
    public Boolean startAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public Set<MetaDataJson> getMetaData(User one, String hash) {
        Stack stack = stackRepository.findStackByHash(hash);
        if (stack != null && !stack.getMetaData().isEmpty()) {
            return metaDataConverter.convertAllEntityToJson(stack.getMetaData());
        }
        throw new NotFoundException("Metadata not found on stack.");
    }

}
