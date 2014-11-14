package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.DescribeContext;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class DefaultStackService implements StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStackService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private DescribeContext describeContext;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Override
    public Set<Stack> retrievePrivateStacks(CbUser user) {
        return stackRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Stack> retrieveAccountStacks(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return stackRepository.findAllInAccount(user.getAccount());
        } else {
            return stackRepository.findPublicsInAccount(user.getAccount());
        }
    }

    @Override
    public Stack get(Long id) {
        Stack stack = stackRepository.findOne(id);
        MDCBuilder.buildMdcContext(stack);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    @Override
    public Stack get(String ambariAddress) {
        Stack stack = stackRepository.findByAmbari(ambariAddress);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack not found by Ambari address: '%s' not found", ambariAddress));
        }
        return stack;
    }

    @Override
    public Stack create(CbUser user, Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        Stack savedStack = null;
        Template template = templateRepository.findOne(stack.getTemplate().getId());
        stack.setOwner(user.getUserId());
        stack.setAccount(user.getAccount());
        stack.setHash(generateHash(stack));
        Optional<String> result = provisionSetups.get(stack.getTemplate().cloudPlatform()).preProvisionCheck(stack);
        if (result.isPresent()) {
            throw new BadRequestException(result.orNull());
        } else {
            try {
                savedStack = stackRepository.save(stack);
                LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_REQUEST_EVENT, stack.getId());
                reactor.notify(ReactorConfig.PROVISION_REQUEST_EVENT, Event.wrap(new ProvisionRequest(template.cloudPlatform(), stack.getId())));
            } catch (DataIntegrityViolationException ex) {
                throw new DuplicateKeyValueException(stack.getName(), ex);
            }
            return savedStack;
        }
    }

    @Override
    public void delete(Long id) {
        Stack stack = stackRepository.findOne(id);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Stack delete requested.");
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        LOGGER.info("Publishing {} event.", ReactorConfig.DELETE_REQUEST_EVENT);
        reactor.notify(ReactorConfig.DELETE_REQUEST_EVENT, Event.wrap(new StackDeleteRequest(stack.getTemplate().cloudPlatform(), stack.getId())));
    }

    @Override
    public void updateStatus(Long stackId, StatusRequest status) {
        Stack stack = stackRepository.findOne(stackId);
        MDCBuilder.buildMdcContext(stack);
        Status stackStatus = stack.getStatus();
        if (status.equals(StatusRequest.STARTED)) {
            if (!Status.STOPPED.equals(stackStatus)) {
                throw new BadRequestException(String.format("Cannot update the status of stack '%s' to STARTED, because it isn't in STOPPED state.", stackId));
            }
            stackUpdater.updateStackStatus(stackId, Status.START_IN_PROGRESS);
            LOGGER.info("Publishing {} event", ReactorConfig.STACK_STATUS_UPDATE_EVENT);
            reactor.notify(ReactorConfig.STACK_STATUS_UPDATE_EVENT,
                    Event.wrap(new StackStatusUpdateRequest(stack.getTemplate().cloudPlatform(), stack.getId(), status)));
        } else {
            Status clusterStatus = clusterRepository.findOneWithLists(stack.getCluster().getId()).getStatus();
            if (Status.STOP_IN_PROGRESS.equals(clusterStatus)) {
                stackUpdater.updateStackStatus(stackId, Status.STOP_REQUESTED);
            } else {
                if (!Status.AVAILABLE.equals(stackStatus)) {
                    throw new BadRequestException(
                            String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stackId));
                }
                if (!Status.STOPPED.equals(clusterStatus)) {
                    throw new BadRequestException(
                            String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.", stackId));
                }
                LOGGER.info("Publishing {} event.", ReactorConfig.STACK_STATUS_UPDATE_EVENT);
                reactor.notify(ReactorConfig.STACK_STATUS_UPDATE_EVENT,
                        Event.wrap(new StackStatusUpdateRequest(stack.getTemplate().cloudPlatform(), stack.getId(), status)));
            }
        }
    }

    @Override
    public void updateNodeCount(Long stackId, Integer scalingAdjustment) {
        Stack stack = stackRepository.findOne(stackId);
        MDCBuilder.buildMdcContext(stack);
        if (!Status.AVAILABLE.equals(stack.getStatus())) {
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.", stackId,
                    stack.getStatus()));
        }
        if (0 == scalingAdjustment) {
            throw new BadRequestException(String.format("Requested scaling adjustment on stack '%s' is 0. Nothing to do.", stackId));
        }
        if (0 > scalingAdjustment) {
            if (-1 * scalingAdjustment > stack.getNodeCount()) {
                throw new BadRequestException(String.format("There are %s instances in stack '%s'. Cannot remove %s instances.", stack.getNodeCount(), stackId,
                        -1 * scalingAdjustment));
            }
            int removeableHosts = 0;
            for (InstanceMetaData metadataEntry : stack.getInstanceMetaData()) {
                if (metadataEntry.isRemovable()) {
                    removeableHosts++;
                }
            }
            if (removeableHosts < -1 * scalingAdjustment) {
                throw new BadRequestException(
                        String.format("There are %s removable hosts on stack '%s' but %s were requested. Decomission nodes from the cluster first!",
                                removeableHosts, stackId, scalingAdjustment * -1));
            }
        }
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS);
        LOGGER.info("Publishing {} event [scalingAdjustment: '{}']", ReactorConfig.UPDATE_INSTANCES_REQUEST_EVENT, scalingAdjustment);
        reactor.notify(ReactorConfig.UPDATE_INSTANCES_REQUEST_EVENT,
                Event.wrap(new UpdateInstancesRequest(stack.getTemplate().cloudPlatform(), stack.getId(), scalingAdjustment)));
    }

    @Override
    public StackDescription getStackDescription(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        CloudPlatform cp = stack.getTemplate().cloudPlatform();
        LOGGER.debug("Getting stack description for cloud platform: {} ...", cp);
        StackDescription description = describeContext.describeStackWithResources(stack);
        LOGGER.debug("Found stack description {}", description.getClass());
        return description;
    }

    @Override
    public Set<InstanceMetaData> getMetaData(String hash) {
        Stack stack = stackRepository.findStackByHash(hash);
        if (stack != null) {
            if (!stack.isMetadataReady()) {
                throw new MetadataIncompleteException("Instance metadata is incomplete.");
            }
            if (!stack.getInstanceMetaData().isEmpty()) {
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
