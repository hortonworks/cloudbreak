package com.sequenceiq.cloudbreak.service.stack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
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
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;

import reactor.core.Reactor;
import reactor.event.Event;

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
    private Map<CloudPlatform, CloudPlatformConnector> provisionServices;

    @Autowired
    private Reactor reactor;

    @Override
    public Set<StackJson> getAll(User user) {
        Set<StackJson> result = new HashSet<>();
        for (Stack stack : user.getStacks()) {
            if (stack.getDeleted().equals(Boolean.FALSE)) {
                result.add(stackConverter.convert(stack));
            }
        }
        return result;
    }

    @Override
    public StackJson get(User user, Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null || stack.getDeleted().equals(Boolean.TRUE)) {
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
        stack.setHash(generateHash(stack));
        stack = stackRepository.save(stack);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_REQUEST_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_REQUEST_EVENT, Event.wrap(new ProvisionRequest(template.cloudPlatform(), stack.getId())));
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
        stack.setDeleted(Boolean.TRUE);
        stackRepository.save(stack);
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
    public Set<InstanceMetaDataJson> getMetaData(User one, String hash) {
        Stack stack = stackRepository.findStackByHash(hash);
        if (stack != null) {
            if (!stack.isMetadataReady()) {
                throw new MetadataIncompleteException("Instance metadata is incomplete.");
            } else if (!stack.getInstanceMetaData().isEmpty()) {
                return metaDataConverter.convertAllEntityToJson(stack.getInstanceMetaData());
            }
        }
        throw new NotFoundException("Metadata not found on stack.");
    }

    private String generateHash(Stack stack) {
        int hashCode = HashCodeBuilder.reflectionHashCode(stack);
        return DigestUtils.md5DigestAsHex(String.valueOf(hashCode).getBytes());
    }

}
