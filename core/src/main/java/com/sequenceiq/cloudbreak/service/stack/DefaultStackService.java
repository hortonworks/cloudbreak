package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateway;

import java.util.List;
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

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

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
    private ClusterRepository clusterRepository;

    @Autowired
    private AmbariClusterConnector ambariClusterConnector;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private FlowManager flowManager;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Autowired
    private BlueprintValidator blueprintValidator;

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
    public Stack get(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
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
        Stack savedStack = null;
        stack.setOwner(user.getUserId());
        stack.setAccount(user.getAccount());
        String result = provisionSetups.get(stack.cloudPlatform()).preProvisionCheck(stack);
        if (null != result) {
            throw new BadRequestException(result);
        } else {
            try {
                savedStack = stackRepository.save(stack);
                flowManager.triggerProvisioning(new ProvisionRequest(savedStack.cloudPlatform(), savedStack.getId()));
            } catch (DataIntegrityViolationException ex) {
                throw new DuplicateKeyValueException(APIResourceType.STACK, stack.getName(), ex);
            }
            return savedStack;
        }
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
    public void updateStatus(Long stackId, StatusRequest status) {
        Stack stack = stackRepository.findOne(stackId);
        Status stackStatus = stack.getStatus();
        if (status.equals(StatusRequest.STARTED)) {
            Status clusterStatus = clusterRepository.findOneWithLists(stack.getCluster().getId()).getStatus();
            if (!Status.STOPPED.equals(stackStatus) && !(Status.AVAILABLE.equals(stackStatus) && Status.START_REQUESTED.equals(clusterStatus))) {
                throw new BadRequestException(String.format("Cannot update the status of stack '%s' to STARTED, because it isn't in STOPPED state.", stackId));
            }
            if (!Status.AVAILABLE.equals(stackStatus)) {
                stackUpdater.updateStackStatus(stackId, Status.START_IN_PROGRESS, "Cluster infrastructure is now starting.");
                flowManager.triggerStackStart(new StackStatusUpdateRequest(stack.cloudPlatform(), stack.getId(), status));
            }
        } else {
            Status clusterStatus = clusterRepository.findOneWithLists(stack.getCluster().getId()).getStatus();
            String stopRequestedMsg = "Stopping of cluster infrastructure has been requested.";
            if (Status.STOP_IN_PROGRESS.equals(clusterStatus)) {
                stackUpdater.updateStackStatus(stackId, Status.STOP_REQUESTED, stopRequestedMsg);
            } else {
                if (!Status.AVAILABLE.equals(stackStatus)) {
                    throw new BadRequestException(
                            String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stackId));
                }
                if (!Status.STOPPED.equals(clusterStatus)) {
                    throw new BadRequestException(
                            String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.", stackId));
                }
                stackUpdater.updateStackStatus(stackId, Status.STOP_REQUESTED, stopRequestedMsg);
                flowManager.triggerStackStop(new StackStatusUpdateRequest(stack.cloudPlatform(), stack.getId(), status));
            }
        }
    }

    @Override
    public void updateNodeCount(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson) {
        Stack stack = stackRepository.findOne(stackId);
        validateStackStatus(stack);
        validateInstanceGroup(stack, instanceGroupAdjustmentJson.getInstanceGroup());
        validateScalingAdjustment(instanceGroupAdjustmentJson, stack);
        int absScalingAdjustment = Math.abs(instanceGroupAdjustmentJson.getScalingAdjustment());

        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            UpdateInstancesRequest updateInstancesRequest = new UpdateInstancesRequest(stack.cloudPlatform(), stack.getId(),
                    instanceGroupAdjustmentJson.getScalingAdjustment(), instanceGroupAdjustmentJson.getInstanceGroup(),
                    instanceGroupAdjustmentJson.getWithClusterEvent() ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK);
            String statusMessage = "Adding '%s' new instance(s) to the cluster infrastructure.";
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, String.format(statusMessage, absScalingAdjustment));
            flowManager.triggerStackUpscale(updateInstancesRequest);
        } else {
            UpdateInstancesRequest updateInstancesRequest = new UpdateInstancesRequest(stack.cloudPlatform(), stack.getId(),
                    instanceGroupAdjustmentJson.getScalingAdjustment(), instanceGroupAdjustmentJson.getInstanceGroup(),
                    ScalingType.DOWNSCALE_ONLY_STACK);
            String statusMessage = "Removing '%s' instance(s) from the cluster infrastructure.";
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, String.format(statusMessage, absScalingAdjustment));
            flowManager.triggerStackDownscale(updateInstancesRequest);
        }
    }

    @Override
    public void updateAllowedSubnets(Long stackId, List<Subnet> subnetList) {
        Stack stack = stackRepository.findOne(stackId);
        if (!Status.AVAILABLE.equals(stack.getStatus())) {
            throw new BadRequestException(String.format("Stack is currently in '%s' state. Security constraints cannot be updated.", stack.getStatus()));
        }
        stackUpdater.updateStackStatus(stackId, Status.UPDATE_IN_PROGRESS, "Updating allowed subnets");
        flowManager.triggerUpdateAllowedSubnets(new UpdateAllowedSubnetsRequest(stack.cloudPlatform(), stackId, subnetList));
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
        blueprintValidator.validateBlueprintForStack(stackValidation.getBlueprint(), stackValidation.getHostGroups(), stackValidation.getInstanceGroups());
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
        if (!Status.AVAILABLE.equals(stack.getStatus())) {
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.", stack.getId(),
                    stack.getStatus()));
        }
    }

    private void validateInstanceGroup(Stack stack, String instanceGroup) {
        if (isGateway(stack.getInstanceGroupByInstanceGroupName(instanceGroup).getInstanceGroupType())) {
            throw new BadRequestException("The Ambari server instancegroup modification is not enabled.");
        }
        if (stack.getInstanceGroupByInstanceGroupName(instanceGroup) == null) {
            throw new BadRequestException(String.format("Stack '%s' does not have an instanceGroup named '%s'.", stack.getId(), instanceGroup));
        }
    }

    private void delete(Stack stack, CbUser user) {
        LOGGER.info("Stack delete requested.");
        if (!user.getUserId().equals(stack.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Stacks can be deleted only by account admins or owners.");
        }
        if (!Status.DELETE_COMPLETED.equals(stack.getStatus())) {
            flowManager.triggerTermination(new StackDeleteRequest(stack.cloudPlatform(), stack.getId()));
        } else {
            LOGGER.info("Stack is already deleted.");
        }
    }

    private String generateHash(Stack stack) {
        int hashCode = HashCodeBuilder.reflectionHashCode(stack);
        return DigestUtils.md5DigestAsHex(String.valueOf(hashCode).getBytes());
    }

}
