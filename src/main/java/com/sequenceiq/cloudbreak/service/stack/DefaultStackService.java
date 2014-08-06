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
import com.sequenceiq.cloudbreak.converter.StackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.company.CompanyService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class DefaultStackService implements StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStackService.class);

    @Autowired
    private StackConverter stackConverter;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private Reactor reactor;

    @Override
    public Set<Stack> getAll(User user) {
        Set<Stack> legacyStacks = new HashSet<>();
        Set<Stack> terminatedStacks = new HashSet<>();
        Set<Stack> userStacks = user.getStacks();
        LOGGER.debug("User stacks: #{}", userStacks.size());

        if (user.getUserRoles().contains(UserRole.COMPANY_ADMIN)) {
            LOGGER.debug("Getting company user stacks for company admin; id: [{}]", user.getId());
            legacyStacks = getCompanyUserStacks(user);
        } else {
            LOGGER.debug("Getting company wide stacks for company user; id: [{}]", user.getId());
            legacyStacks = getCompanyStacks(user);
        }
        LOGGER.debug("Found #{} legacy stacks for user [{}]", legacyStacks.size(), user.getId());
        userStacks.addAll(legacyStacks);
        legacyStacks.clear();
        for (Stack stack : userStacks) {
            if (Boolean.FALSE.equals(stack.getTerminated())) {
                terminatedStacks.add(stack);
            }
        }
        return terminatedStacks;
    }

    private Set<Stack> getCompanyStacks(User user) {
        Set<Stack> companyStacks = new HashSet<>();
        User adminWithFilteredData = companyService.companyUserData(user.getCompany().getId(), user.getUserRoles().iterator().next());
        if (adminWithFilteredData != null) {
            companyStacks = adminWithFilteredData.getStacks();
        } else {
            LOGGER.debug("There's no company admin for user: [{}]", user.getId());
        }
        return companyStacks;
    }

    private Set<Stack> getCompanyUserStacks(User user) {
        Set<Stack> companyUserStacks = new HashSet<>();
        Set<User> companyUsers = companyService.companyUsers(user.getCompany().getId());
        companyUsers.remove(user);
        for (User cUser : companyUsers) {
            LOGGER.debug("Adding blueprints of company user: [{}]", cUser.getId());
            companyUserStacks.addAll(cUser.getStacks());
        }
        return companyUserStacks;
    }

    @Override
    public Stack get(User user, Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null || Boolean.TRUE.equals(stack.getTerminated())) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    @Override
    public Stack create(User user, Stack stack) {
        Template template = templateRepository.findOne(stack.getTemplate().getId());
        stack.setUser(user);
        stack.setHash(generateHash(stack));
        stack = stackRepository.save(stack);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_REQUEST_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_REQUEST_EVENT, Event.wrap(new ProvisionRequest(template.cloudPlatform(), stack.getId())));
        return stack;
    }

    @Override
    public void delete(User user, Long id) {
        LOGGER.info("Stack delete requested. [StackId: {}]", id);
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.DELETE_REQUEST_EVENT, stack.getId());
        reactor.notify(ReactorConfig.DELETE_REQUEST_EVENT, Event.wrap(new StackDeleteRequest(stack.getTemplate().cloudPlatform(), stack.getId())));
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
    public StackDescription getStackDescription(User user, Stack stack) {
        CloudPlatform cp = stack.getTemplate().cloudPlatform();
        LOGGER.debug("Getting stack description for cloud platform: {} ...", cp);
        StackDescription description = cloudPlatformConnectors.get(cp).describeStackWithResources(user, stack, stack.getCredential());
        LOGGER.debug("Found stack description {}", description.getClass());
        return description;
    }

    @Override
    public Set<InstanceMetaData> getMetaData(User one, String hash) {
        Stack stack = stackRepository.findStackByHash(hash);
        if (stack != null) {
            if (!stack.isMetadataReady()) {
                throw new MetadataIncompleteException("Instance metadata is incomplete.");
            } else if (!stack.getInstanceMetaData().isEmpty()) {
                return stack.getInstanceMetaData();
            }
        }
        throw new NotFoundException("Metadata not found on stack.");
    }

    private String generateHash(Stack stack) {
        int hashCode = HashCodeBuilder.reflectionHashCode(stack);
        return DigestUtils.md5DigestAsHex(String.valueOf(hashCode).getBytes());
    }
}
