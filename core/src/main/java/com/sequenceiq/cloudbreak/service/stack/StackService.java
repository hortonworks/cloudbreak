package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway;
import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageNameUtil;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Service
@Transactional
public class StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    @Inject
    private StackRepository stackRepository;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private ImageService imageService;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private OrchestratorRepository orchestratorRepository;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private TerminationService terminationService;
    @Inject
    private ReactorFlowManager flowManager;
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
    private ServiceProviderConnectorAdapter connector;
    @Inject
    private ImageNameUtil imageNameUtil;
    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;
    @Inject
    private ComponentConfigProvider componentConfigProvider;
    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Value("${cb.nginx.port:9443}")
    private int nginxPort;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public Set<StackResponse> retrievePrivateStacks(CbUser user) {
        return convertStacks(stackRepository.findForUser(user.getUserId()));
    }

    private Set<StackResponse> convertStacks(Set<Stack> stacks) {
        return (Set<StackResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackResponse.class)));
    }

    public Set<StackResponse> retrieveAccountStacks(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return convertStacks(stackRepository.findAllInAccount(user.getAccount()));
        } else {
            return convertStacks(stackRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount()));
        }
    }

    public Set<Stack> retrieveAccountStacks(String account) {
        return stackRepository.findAllInAccount(account);
    }

    public Set<Stack> retrieveOwnerStacks(String owner) {
        return stackRepository.findForUser(owner);
    }

    public StackResponse getJsonById(Long id) {
        Stack stack = get(id);
        return conversionService.convert(stack, StackResponse.class);
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Stack get(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    public Stack findLazy(Long id) {
        Stack stack = stackRepository.findByIdLazy(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    public Stack getById(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return retStack;
    }

    public StackResponse getByIdJson(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return conversionService.convert(retStack, StackResponse.class);
    }

    public StackResponse get(String ambariAddress) {
        Stack stack = stackRepository.findByAmbari(ambariAddress);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack not found by Ambari address: '%s' not found", ambariAddress));
        }
        return conversionService.convert(stack, StackResponse.class);
    }

    public Stack getPrivateStack(String name, CbUser cbUser) {
        Stack stack = stackRepository.findByNameInUser(name, cbUser.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return stack;
    }

    public StackResponse getPrivateStackJson(String name, CbUser cbUser) {
        Stack stack = getPrivateStack(name, cbUser);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return conversionService.convert(stack, StackResponse.class);
    }

    public StackResponse getPrivateStackJsonByName(String name, CbUser cbUser) {
        Stack stack = stackRepository.findByNameInUser(name, cbUser.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return conversionService.convert(stack, StackResponse.class);
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public StackResponse getPublicStackJsonByName(String name, CbUser cbUser) {
        Stack stack = stackRepository.findOneByName(name, cbUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return conversionService.convert(stack, StackResponse.class);
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Stack getPublicStack(String name, CbUser cbUser) {
        Stack stack = stackRepository.findOneByName(name, cbUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return stack;
    }

    public void delete(String name, CbUser user) {
        Stack stack = stackRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        delete(stack, user);
    }

    public void forceDelete(String name, CbUser user) {
        Stack stack = stackRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        forceDelete(stack, user);
    }

    @Transactional(Transactional.TxType.NEVER)
    public Stack create(CbUser user, Stack stack, String ambariVersion, String hdpVersion, String imageCatalog) {
        Stack savedStack;
        stack.setOwner(user.getUserId());
        stack.setAccount(user.getAccount());
        stack.setGatewayPort(nginxPort);
        setPlatformVariant(stack);
        MDCBuilder.buildMdcContext(stack);
        try {
            if (stack.getOrchestrator() != null) {
                orchestratorRepository.save(stack.getOrchestrator());
            }
            savedStack = stackRepository.save(stack);
            MDCBuilder.buildMdcContext(savedStack);
            if (!"BYOS".equals(stack.cloudPlatform())) {
                instanceGroupRepository.save(savedStack.getInstanceGroups());
                tlsSecurityService.copyClientKeys(stack.getId());

                SecurityConfig securityConfig = tlsSecurityService.storeSSHKeys(stack.getId());
                securityConfig.setSaltPassword(PasswordUtil.generatePassword());
                securityConfig.setSaltBootPassword(PasswordUtil.generatePassword());
                securityConfig.setStack(stack);
                securityConfigRepository.save(securityConfig);
                stack.setSecurityConfig(securityConfig);

                imageService.create(savedStack, connector.getPlatformParameters(stack), ambariVersion, hdpVersion, imageCatalog);
                flowManager.triggerProvisioning(savedStack.getId());
            } else {
                savedStack.setStatus(Status.AVAILABLE);
                savedStack.setCreated(new Date().getTime());
                stackRepository.save(savedStack);
            }
            addCloudbreakDetailsForStack(stack);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.STACK, stack.getName(), ex);
        } catch (CloudbreakSecuritySetupException e) {
            LOGGER.error("Storing of security credentials failed", e);
            throw new CloudbreakApiException("Storing security credentials failed", e);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error("Cloudbreak Image not found", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }
        return savedStack;
    }

    private void setPlatformVariant(Stack stack) {
        stack.setPlatformVariant(connector.checkAndGetPlatformVariant(stack).value());
    }

    public void delete(Long id, CbUser user) {
        Stack stack = stackRepository.findByIdInAccount(id, user.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        delete(stack, user);
    }

    public void forceDelete(Long id, CbUser user) {
        Stack stack = stackRepository.findByIdInAccount(id, user.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        forceDelete(stack, user);
    }

    public void removeInstance(CbUser user, Long stackId, String instanceId) {
        Stack stack = get(stackId);
        InstanceMetaData instanceMetaData = instanceMetaDataRepository.findByInstanceId(stackId, instanceId);
        if (instanceMetaData == null) {
            throw new NotFoundException(String.format("Metadata for instance %s not found.", instanceId));
        }
        if (!stack.isPublicInAccount() && !stack.getOwner().equals(user.getUserId())) {
            throw new BadRequestException(String.format("Private stack (%s) only modifiable by the owner.", stackId));
        }
        flowManager.triggerStackRemoveInstance(stackId, instanceId);
    }

    @Transactional(Transactional.TxType.NEVER)
    public void updateStatus(Long stackId, StatusRequest status) {
        Stack stack = getById(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        }
        if ("BYOS".equals(stack.cloudPlatform())) {
            LOGGER.warn("The status of a 'Bring your own stack' type of infrastructure cannot be changed.");
            return;
        }
        switch (status) {
            case SYNC:
                sync(stack, status, false);
                break;
            case FULL_SYNC:
                sync(stack, status, true);
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

    private void sync(Stack stack, StatusRequest statusRequest, boolean full) {
        if (!stack.isDeleteInProgress() && !stack.isStackInDeletionPhase() && !stack.isModificationInProgress()) {
            if (full) {
                flowManager.triggerFullSync(stack.getId());
            } else {
                flowManager.triggerStackSync(stack.getId());
            }
        } else {
            LOGGER.warn("Stack could not be synchronized in {} state!", stack.getStatus());
        }
    }

    private void stop(Stack stack, Cluster cluster, StatusRequest statusRequest) {
        if (cluster != null && cluster.isStopInProgress()) {
            stackUpdater.updateStackStatus(stack.getId(), STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.");
            String message = cloudbreakMessagesService.getMessage(Msg.STACK_STOP_REQUESTED.code());
            eventService.fireCloudbreakEvent(stack.getId(), STOP_REQUESTED.name(), message);
        } else {
            StopRestrictionReason reason = stack.isInfrastructureStoppable();
            if (stack.isStopped()) {
                String statusDesc = cloudbreakMessagesService.getMessage(Msg.STACK_STOP_IGNORED.code());
                LOGGER.info(statusDesc);
                eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), statusDesc);
            } else if (reason != StopRestrictionReason.NONE) {
                throw new BadRequestException(
                        String.format("Cannot stop a stack '%s'. Reason: %s", stack.getId(), reason.getReason()));
            } else if (!stack.isAvailable() && !stack.isStopFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.getId()));
            } else if ((cluster != null && !cluster.isStopped()) && !stack.isStopFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.", stack.getId()));
            } else {
                stackUpdater.updateStackStatus(stack.getId(), STOP_REQUESTED);
                flowManager.triggerStackStop(stack.getId());
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
            flowManager.triggerStackStart(stack.getId());
        }
    }

    public void updateNodeCount(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson) {
        Stack stack = get(stackId);
        validateStackStatus(stack);
        validateInstanceGroup(stack, instanceGroupAdjustmentJson.getInstanceGroup());
        validateScalingAdjustment(instanceGroupAdjustmentJson, stack);
        if (instanceGroupAdjustmentJson.getWithClusterEvent()) {
            validateHostGroupAdjustment(instanceGroupAdjustmentJson, stack, instanceGroupAdjustmentJson.getScalingAdjustment());
        }
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            stackUpdater.updateStackStatus(stackId, UPDATE_REQUESTED);
            flowManager.triggerStackUpscale(stack.getId(), instanceGroupAdjustmentJson);
        } else {
            flowManager.triggerStackDownscale(stack.getId(), instanceGroupAdjustmentJson);
        }
    }

    public InstanceMetaData updateMetaDataStatus(Long id, String hostName, InstanceStatus status) {
        InstanceMetaData metaData = instanceMetaDataRepository.findHostInStack(id, hostName);
        if (metaData == null) {
            throw new NotFoundException(String.format("Metadata not found on stack:'%s' with hostname: '%s'.", id, hostName));
        }
        metaData.setInstanceStatus(status);
        return instanceMetaDataRepository.save(metaData);
    }

    public void validateStack(StackValidation stackValidation) {
        networkConfigurationValidator.validateNetworkForStack(stackValidation.getNetwork(), stackValidation.getInstanceGroups());
        blueprintValidator.validateBlueprintForStack(stackValidation.getBlueprint(), stackValidation.getHostGroups(), stackValidation.getInstanceGroups());
    }

    public void validateOrchestrator(Orchestrator orchestrator) {
        try {
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
            if (containerOrchestrator != null) {
                containerOrchestrator.validateApiEndpoint(new OrchestrationCredential(orchestrator.getApiEndpoint(), orchestrator.getAttributes().getMap()));
            }
        } catch (CloudbreakException e) {
            throw new BadRequestException(String.format("Invalid orchestrator type: %s", e.getMessage()));
        } catch (CloudbreakOrchestratorException e) {
            throw new BadRequestException(String.format("Error occurred when trying to reach orchestrator API: %s", e.getMessage()));
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    public Stack save(Stack stack) {
        return stackRepository.save(stack);
    }

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

    private void validateHostGroupAdjustment(final InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, Stack stack, Integer adjustment) {
        Blueprint blueprint = stack.getCluster().getBlueprint();
        HostGroup hostGroup = stack.getCluster().getHostGroups().stream().filter(input -> {
            // TODO: why instancegroups?
            return input.getConstraint().getInstanceGroup().getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup());
        }).findFirst().get();
        if (hostGroup == null) {
            throw new BadRequestException(String.format("Instancegroup '%s' not found or not part of stack '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
        blueprintValidator.validateHostGroupScalingRequest(blueprint, hostGroup, adjustment);
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
            throw new BadRequestException(String.format("Stack '%s' does not have an instanceGroup named '%s'.", stack.getId(), instanceGroupName));
        }
        if (isGateway(instanceGroup.getInstanceGroupType())) {
            throw new BadRequestException("The Ambari server instance group modification is not enabled.");
        }
    }

    private void delete(Stack stack, CbUser user) {
        LOGGER.info("Stack delete requested.");
        if (!user.getUserId().equals(stack.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Stacks can be deleted only by account admins or owners.");
        }
        if (!stack.isDeleteCompleted()) {
            if (!"BYOS".equals(stack.cloudPlatform())) {
                flowManager.triggerTermination(stack.getId());
            } else {
                terminationService.finalizeTermination(stack.getId(), false);
            }
        } else {
            LOGGER.info("Stack is already deleted.");
        }
    }

    private void forceDelete(Stack stack, CbUser user) {
        LOGGER.info("Stack forced delete requested.");
        if (!user.getUserId().equals(stack.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Stacks can be force deleted only by account admins or owners.");
        }
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerForcedTermination(stack.getId());
        } else {
            LOGGER.info("Stack is already deleted.");
        }
    }

    private enum Msg {
        STACK_STOP_IGNORED("stack.stop.ignored"),
        STACK_START_IGNORED("stack.start.ignored"),
        STACK_STOP_REQUESTED("stack.stop.requested");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    private void addCloudbreakDetailsForStack(Stack stack) {
        CloudbreakDetails cbDetails = new CloudbreakDetails(cbVersion);
        try {
            Component cbDetailsComponent = new Component(ComponentType.CLOUDBREAK_DETAILS, ComponentType.CLOUDBREAK_DETAILS.name(), new Json(cbDetails), stack);
            componentConfigProvider.store(cbDetailsComponent);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not create Cloudbreak details component.", e);
        }
    }
}
