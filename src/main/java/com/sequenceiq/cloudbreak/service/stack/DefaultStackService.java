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
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.converter.StackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.AddNodeRequest;
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
    private AccountService accountService;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Override
    public Set<Stack> getAll(User user) {
        Set<Stack> legacyStacks = new HashSet<>();
        Set<Stack> terminatedStacks = new HashSet<>();
        Set<Stack> userStacks = user.getStacks();
        LOGGER.debug("User stacks: #{}", userStacks.size());

        if (user.getUserRoles().contains(UserRole.ACCOUNT_ADMIN)) {
            LOGGER.debug("Getting company user stacks for company admin; id: [{}]", user.getId());
            legacyStacks = getCompanyUserStacks(user);
        } else if (user.getUserRoles().contains(UserRole.ACCOUNT_USER)) {
            LOGGER.debug("Getting company wide stacks for company user; id: [{}]", user.getId());
            legacyStacks = getCompanyStacks(user);
        }
        LOGGER.debug("Found #{} legacy stacks for user [{}]", legacyStacks.size(), user.getId());
        userStacks.addAll(legacyStacks);
        return userStacks;
    }

    private Set<Stack> getCompanyStacks(User user) {
        Set<Stack> companyStacks = new HashSet<>();
        User adminWithFilteredData = accountService.accountUserData(user.getAccount().getId(), user.getUserRoles().iterator().next());
        if (adminWithFilteredData != null) {
            companyStacks = adminWithFilteredData.getStacks();
        } else {
            LOGGER.debug("There's no company admin for user: [{}]", user.getId());
        }
        return companyStacks;
    }

    private Set<Stack> getCompanyUserStacks(User user) {
        Set<Stack> companyUserStacks = new HashSet<>();
        Set<User> companyUsers = accountService.accountUsers(user.getAccount().getId());
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
        if (stack == null) {
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
    public void updateStatus(User user, Long stackId, StatusRequest status) {
        // TODO implement start/stop
    }

    @Override
    public void updateNodeCount(User user, Long stackId, Integer nodeCount) {
        Stack stack = stackRepository.findOne(stackId);
        if (!Status.AVAILABLE.equals(stack.getStatus())) {
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.", stackId,
                    stack.getStatus()));
        }
        if (stack.getNodeCount() == nodeCount) {
            throw new BadRequestException(String.format("Stack '%s' already has exactly %s nodes. Nothing to do.", stackId, nodeCount));
        }
        if (stack.getNodeCount() > nodeCount) {
            throw new BadRequestException(
                    String.format("Requested node count (%s) on stack '%s' is lower than the current node count (%s). "
                            + "Decommisioning nodes is not yet supported by the Cloudbreak API.",
                            nodeCount, stackId, stack.getNodeCount()));
        }
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.ADD_NODE_REQUEST_EVENT, stack.getId());
        reactor.notify(ReactorConfig.ADD_NODE_REQUEST_EVENT,
                Event.wrap(new AddNodeRequest(stack.getTemplate().cloudPlatform(), stack.getId(), nodeCount - stack.getNodeCount())));
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
    public Set<InstanceMetaData> getMetaData(String hash) {
        Stack stack = stackRepository.findStackByHash(hash);
        if (stack != null) {
            if (!Status.UPDATE_IN_PROGRESS.equals(stack.getStatus())) {
                if (!stack.isMetadataReady()) {
                    throw new MetadataIncompleteException("Instance metadata is incomplete.");
                } else if (!stack.getInstanceMetaData().isEmpty()) {
                    return stack.getInstanceMetaData();
                }
            } else {
                throw new MetadataIncompleteException("Instance metadata is incomplete.");
            }
        }
        throw new NotFoundException("Metadata not found on stack.");
    }

    private String generateHash(Stack stack) {
        int hashCode = HashCodeBuilder.reflectionHashCode(stack);
        return DigestUtils.md5DigestAsHex(String.valueOf(hashCode).getBytes());
    }

}
