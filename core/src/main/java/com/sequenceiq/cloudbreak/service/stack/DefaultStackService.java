package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.isGateway;
import static com.sequenceiq.cloudbreak.common.type.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.common.type.Status.STOPPED;
import static com.sequenceiq.cloudbreak.common.type.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.common.type.Status.UPDATE_REQUESTED;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.common.type.StatusRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.controller.validation.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

@Service
public class DefaultStackService implements StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStackService.class);

    @Inject
    private StackRepository stackRepository;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private SecurityConfigRepository securityConfigRepository;
    @Inject
    private FlowManager flowManager;
    @Inject
    private BlueprintValidator blueprintValidator;
    @Inject
    private NetworkConfigurationValidator networkConfigurationValidator;
    @Inject
    private SecurityRuleRepository securityRuleRepository;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;
    @Inject
    private CloudPlatformResolver platformResolver;

    private enum Msg {
        STACK_STOP_IGNORED("stack.stop.ignored"),
        STACK_START_IGNORED("stack.start.ignored");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    @Override
    public Set<Stack> retrievePrivateStacks(CbUser user) {
        return stackRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Stack> retrieveAccountStacks(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return stackRepository.findAllInAccount(user.getAccount());
        } else {
            return stackRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @Override
    public Set<Stack> retrieveAccountStacks(String account) {
        return stackRepository.findAllInAccount(account);
    }

    @Override
    public Set<Stack> retrieveOwnerStacks(String owner) {
        return stackRepository.findForUser(owner);
    }

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    public Stack get(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    @Override
    public Stack findLazy(Long id) {
        Stack stack = stackRepository.findByIdLazy(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    @Override
    public byte[] getCertificate(Long id) {
        String cert = securityConfigRepository.getServerCertByStackId(id);
        if (cert == null) {
            throw new NotFoundException("Stack doesn't exist, or certificate was not found for stack.");
        }
        return Base64.decodeBase64(cert);
    }

    @Override
    public Stack getById(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return retStack;
    }

    @Override
    public Stack get(String ambariAddress) {
        Stack stack = stackRepository.findByAmbari(ambariAddress);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack not found by Ambari address: '%s' not found", ambariAddress));
        }
        return stack;
    }

    public Stack getPrivateStack(String name, CbUser cbUser) {
        Stack stack = stackRepository.findByNameInUser(name, cbUser.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return stack;
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Stack getPublicStack(String name, CbUser cbUser) {
        Stack stack = stackRepository.findOneByName(name, cbUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return stack;
    }

    @Override
    public void delete(String name, CbUser user) {
        Stack stack = stackRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        delete(stack, user);
    }

    @Override
    public Stack create(CbUser user, Stack stack) {
        checkCloudPlatform(stack, "create");
        Stack savedStack = null;
        stack.setOwner(user.getUserId());
        stack.setAccount(user.getAccount());
        setPlatformVariant(stack);
        MDCBuilder.buildMdcContext(stack);
        String result = platformResolver.provisioning(stack.cloudPlatform()).preProvisionCheck(stack);
        if (null != result) {
            throw new BadRequestException(result);
        } else {
            try {
                savedStack = stackRepository.save(stack);
                instanceGroupRepository.save(savedStack.getInstanceGroups());
                MDCBuilder.buildMdcContext(savedStack);
                flowManager.triggerProvisioning(new ProvisionRequest(savedStack.cloudPlatform(), savedStack.getId()));
            } catch (DataIntegrityViolationException ex) {
                throw new DuplicateKeyValueException(APIResourceType.STACK, stack.getName(), ex);
            }
            return savedStack;
        }
    }

    private void setPlatformVariant(Stack stack) {
        stack.setPlatformVariant(platformResolver.connector(stack.cloudPlatform()).checkAndGetPlatformVariant(stack));
    }

    @Override
    public void delete(Long id, CbUser user) {
        Stack stack = stackRepository.findByIdInAccount(id, user.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        delete(stack, user);
    }

    @Override
    public void removeInstance(CbUser user, Long stackId, String instanceId) {
        Stack stack = get(stackId);
        checkCloudPlatform(stack, "removeInstance");
        InstanceMetaData instanceMetaData = instanceMetaDataRepository.findByInstanceId(stackId, instanceId);
        if (instanceMetaData == null) {
            throw new NotFoundException(String.format("Metadata for instance %s not found.", instanceId));
        }
        if (!stack.isPublicInAccount() && !stack.getOwner().equals(user.getUserId())) {
            throw new BadRequestException(String.format("Private stack (%s) only modifiable by the owner.", stackId));
        }
        flowManager.triggerStackRemoveInstance(new RemoveInstanceRequest(stack.cloudPlatform(), stack.getId(), instanceId));
    }

    @Override
    public void updateStatus(Long stackId, StatusRequest status) {
        Stack stack = get(stackId);
        checkCloudPlatform(stack, "updateStatus: " + status.name());
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        }
        switch (status) {
            case SYNC:
                sync(stack, status);
                break;
            case STOPPED:
                stop(stack, cluster, status);
                break;
            case STARTED:
                start(stack, cluster, status);
                break;
            default:
                throw new BadRequestException("Cannot update the status of stack because status request not valid.");
        }
    }

    private void sync(Stack stack, StatusRequest statusRequest) {
        flowManager.triggerStackSync(new StackStatusUpdateRequest(stack.cloudPlatform(), stack.getId(), statusRequest));
    }

    private void stop(Stack stack, Cluster cluster, StatusRequest statusRequest) {
        if (cluster != null && cluster.isStopInProgress()) {
            flowManager.triggerStackStopRequested(new StackStatusUpdateRequest(stack.cloudPlatform(), stack.getId(), statusRequest));
        } else {
            if (stack.isStopped()) {
                String statusDesc = cloudbreakMessagesService.getMessage(Msg.STACK_STOP_IGNORED.code());
                LOGGER.info(statusDesc);
                eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), statusDesc);
            } else if (stack.infrastructureIsEphemeral()) {
                throw new BadRequestException(
                        String.format("Cannot stop a stack if the volumeType is Ephemeral.", stack.getId()));
            } else if (!stack.isAvailable() && !stack.isStopFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.getId()));
            } else if ((cluster != null && !cluster.isStopped()) && !stack.isStopFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.", stack.getId()));
            } else {
                stackUpdater.updateStackStatus(stack.getId(), STOP_REQUESTED);
                flowManager.triggerStackStop(new StackStatusUpdateRequest(stack.cloudPlatform(), stack.getId(), statusRequest));
            }
        }
    }

    private void start(Stack stack, Cluster cluster, StatusRequest statusRequest) {
        if (stack.isAvailable()) {
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.STACK_START_IGNORED.code());
            LOGGER.info(statusDesc);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), statusDesc);
        } else if ((!stack.isStopped() || (cluster != null && !cluster.isStopped())) && !stack.isStartFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of stack '%s' to STARTED, because it isn't in STOPPED state.", stack.getId()));
        } else if (stack.isStopped() || stack.isStartFailed()) {
            stackUpdater.updateStackStatus(stack.getId(), START_REQUESTED);
            flowManager.triggerStackStart(new StackStatusUpdateRequest(stack.cloudPlatform(), stack.getId(), statusRequest));
        }
    }

    @Override
    public void updateNodeCount(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson) {
        Stack stack = get(stackId);
        checkCloudPlatform(stack, "updateNodeCount");
        validateStackStatus(stack);
        validateInstanceGroup(stack, instanceGroupAdjustmentJson.getInstanceGroup());
        validateScalingAdjustment(instanceGroupAdjustmentJson, stack);
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            UpdateInstancesRequest updateInstancesRequest = new UpdateInstancesRequest(stack.cloudPlatform(), stack.getId(),
                    instanceGroupAdjustmentJson.getScalingAdjustment(), instanceGroupAdjustmentJson.getInstanceGroup(),
                    instanceGroupAdjustmentJson.getWithClusterEvent() ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK);
            stackUpdater.updateStackStatus(stackId, UPDATE_REQUESTED);
            flowManager.triggerStackUpscale(updateInstancesRequest);
        } else {
            UpdateInstancesRequest updateInstancesRequest = new UpdateInstancesRequest(stack.cloudPlatform(), stack.getId(),
                    instanceGroupAdjustmentJson.getScalingAdjustment(), instanceGroupAdjustmentJson.getInstanceGroup(),
                    ScalingType.DOWNSCALE_ONLY_STACK);
            flowManager.triggerStackDownscale(updateInstancesRequest);
        }
    }

    @Override
    public void updateAllowedSubnets(Long stackId, List<SecurityRule> securityRuleList) {
        Stack stack = get(stackId);
        checkCloudPlatform(stack, "updateAllowedSubnets");
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format("Stack is currently in '%s' state. Security constraints cannot be updated.", stack.getStatus()));
        }
        flowManager.triggerUpdateAllowedSubnets(new UpdateAllowedSubnetsRequest(stack.cloudPlatform(), stackId, securityRuleList));
    }

    @Override
    public InstanceMetaData updateMetaDataStatus(Long id, String hostName, InstanceStatus status) {
        InstanceMetaData metaData = instanceMetaDataRepository.findHostInStack(id, hostName);
        if (metaData == null) {
            throw new NotFoundException(String.format("Metadata not found on stack:'%s' with hostname: '%s'.", id, hostName));
        }
        metaData.setInstanceStatus(status);
        return instanceMetaDataRepository.save(metaData);
    }

    @Override
    public void validateStack(StackValidation stackValidation) {
        networkConfigurationValidator.validateNetworkForStack(stackValidation.getNetwork(), stackValidation.getInstanceGroups());
        blueprintValidator.validateBlueprintForStack(stackValidation.getBlueprint(), stackValidation.getHostGroups(), stackValidation.getInstanceGroups());
    }

    @Override
    public Stack save(Stack stack) {
        return stackRepository.save(stack);
    }

    @Override
    public List<Stack> getAllAlive() {
        return stackRepository.findAllAlive();
    }

    private void validateScalingAdjustment(InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, Stack stack) {
        if (0 == instanceGroupAdjustmentJson.getScalingAdjustment()) {
            throw new BadRequestException(String.format("Requested scaling adjustment on stack '%s' is 0. Nothing to do.", stack.getId()));
        }
        if (0 > instanceGroupAdjustmentJson.getScalingAdjustment()) {
            InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupAdjustmentJson.getInstanceGroup());
            if (-1 * instanceGroupAdjustmentJson.getScalingAdjustment() > instanceGroup.getNodeCount()) {
                throw new BadRequestException(String.format("There are %s instances in instance group '%s'. Cannot remove %s instances.",
                        instanceGroup.getNodeCount(), instanceGroup.getGroupName(),
                        -1 * instanceGroupAdjustmentJson.getScalingAdjustment()));
            }
            int removableHosts = instanceMetaDataRepository.findRemovableInstances(stack.getId(), instanceGroupAdjustmentJson.getInstanceGroup()).size();
            if (removableHosts < -1 * instanceGroupAdjustmentJson.getScalingAdjustment()) {
                throw new BadRequestException(
                        String.format("There are %s unregistered instances in instance group '%s' but %s were requested. Decommission nodes from the cluster!",
                                removableHosts, instanceGroup.getGroupName(), instanceGroupAdjustmentJson.getScalingAdjustment() * -1));
            }
        }
    }

    private void validateStackStatus(Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.", stack.getId(),
                    stack.getStatus()));
        }
    }

    private void validateInstanceGroup(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(String.format("Stack '%s' does not have an instanceGroup named '%s'.", stack.getId(), instanceGroup));
        }
        if (isGateway(instanceGroup.getInstanceGroupType())) {
            throw new BadRequestException("The Ambari server instancegroup modification is not enabled.");
        }
    }

    private void delete(Stack stack, CbUser user) {
        LOGGER.info("Stack delete requested.");
        if (!user.getUserId().equals(stack.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Stacks can be deleted only by account admins or owners.");
        }
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerTermination(new StackDeleteRequest(stack.cloudPlatform(), stack.getId()));
        } else {
            LOGGER.info("Stack is already deleted.");
        }
    }

    // TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
    private void checkCloudPlatform(Stack stack, String operation) {
        if (stack.getCredential().cloudPlatform() == CloudPlatform.AZURE) {
            throw new BadRequestException(String.format("Unsupported operation: %s, on old azure clusters the only supported operation is the termination.",
                    operation));
        }
    }
}
