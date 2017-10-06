package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.StatusRequest.FULL_SYNC;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
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
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterService;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Service
@Transactional
public class StackService {

    private static final String SSH_USER_CENT = "centos";

    private static final String SSH_USER_CB = "cloudbreak";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ImageService imageService;

    @Inject
    private AmbariClusterService ambariClusterService;

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
    private ReactorFlowManager flowManager;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private NetworkConfigurationValidator networkConfigurationValidator;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private Decorator<StackResponse> stackResponseDecorator;

    @Inject
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;

    @Value("${cb.nginx.port:9443}")
    private Integer nginxPort;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private AuthorizationService authorizationService;

    public Set<StackResponse> retrievePrivateStacks(IdentityUser user) {
        return convertStacks(stackRepository.findForUser(user.getUserId()));
    }

    public Set<StackResponse> retrieveAccountStacks(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
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

    public StackResponse getJsonById(Long id, Set<String> entry) {
        Stack stack = get(id);
        StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
        stackResponse = stackResponseDecorator.decorate(stackResponse, entry, stack);
        return stackResponse;
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Stack get(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Stack getForAutoscale(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        Set<Stack> aliveOnes = stackRepository.findAliveOnes();
        return convertStacksForAutoscale(aliveOnes);
    }

    public Stack findLazy(Long id) {
        Stack stack = stackRepository.findByIdLazy(id);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return stack;
    }

    public Set<Stack> findClustersConnectedToDatalake(Long stackId) {
        return stackRepository.findEphemeralClusters(stackId);
    }

    public Stack getById(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        return retStack;
    }

    public StackResponse get(String ambariAddress) {
        Stack stack = stackRepository.findByAmbari(ambariAddress);
        if (stack == null) {
            throw new NotFoundException(String.format("Stack not found by Ambari address: '%s' not found", ambariAddress));
        }
        return conversionService.convert(stack, StackResponse.class);
    }

    public Stack getPrivateStack(String name, IdentityUser identityUser) {
        Stack stack = stackRepository.findByNameInUser(name, identityUser.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return stack;
    }

    public StackResponse getPrivateStackJsonByName(String name, IdentityUser identityUser, Set<String> entries) {
        Stack stack = stackRepository.findByNameInUser(name, identityUser.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
        stackResponse = stackResponseDecorator.decorate(stackResponse, entries, stack);
        return stackResponse;
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public StackResponse getPublicStackJsonByName(String name, IdentityUser identityUser, Set<String> entries) {
        Stack stack = stackRepository.findOneByName(name, identityUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
        stackResponse = stackResponseDecorator.decorate(stackResponse, entries, stack);
        return stackResponse;
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Stack getPublicStack(String name, IdentityUser identityUser) {
        Stack stack = stackRepository.findOneByName(name, identityUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        return stack;
    }

    public void delete(String name, IdentityUser user, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        delete(stack, deleteDependencies);
    }

    public void forceDelete(String name, IdentityUser user, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", name));
        }
        forceDelete(stack, deleteDependencies);
    }

    @Transactional(TxType.NEVER)
    public Stack create(IdentityUser user, Stack stack, String ambariVersion, String hdpVersion, String imageCatalog, Optional<String> customImage) {
        Stack savedStack;
        stack.setOwner(user.getUserId());
        stack.setAccount(user.getAccount());
        stack.setGatewayPort(nginxPort);
        setPlatformVariant(stack);
        MDCBuilder.buildMdcContext(stack);
        try {
            if (!stack.getStackAuthentication().passwordAuthenticationRequired() && !Strings.isNullOrEmpty(stack.getStackAuthentication().getPublicKey())) {
                rsaPublicKeyValidator.validate(stack.getStackAuthentication().getPublicKey());
            }
            if (stack.getOrchestrator() != null) {
                orchestratorRepository.save(stack.getOrchestrator());
            }
            if (stack.getCredential().getAttributes().getMap().get("keystoneVersion") != null) {
                stack.getStackAuthentication().setLoginUserName(SSH_USER_CENT);
            } else {
                stack.getStackAuthentication().setLoginUserName(SSH_USER_CB);
            }
            String template = connector.getTemplate(stack);
            savedStack = stackRepository.save(stack);
            addTemplateForStack(stack, template);
            addCloudbreakDetailsForStack(stack);
            MDCBuilder.buildMdcContext(savedStack);
            instanceGroupRepository.save(savedStack.getInstanceGroups());

            if (!BYOS.equals(savedStack.cloudPlatform())) {
                SecurityConfig securityConfig = tlsSecurityService.storeSSHKeys();
                securityConfig.setSaltPassword(PasswordUtil.generatePassword());
                securityConfig.setSaltBootPassword(PasswordUtil.generatePassword());
                securityConfig.setKnoxMasterSecret(PasswordUtil.generatePassword());
                securityConfig.setStack(stack);
                securityConfigRepository.save(securityConfig);
                savedStack.setSecurityConfig(securityConfig);
                imageService.create(savedStack, connector.getPlatformParameters(stack), ambariVersion, hdpVersion, imageCatalog, customImage);
                flowManager.triggerProvisioning(savedStack.getId());
            } else {
                savedStack = stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISIONED);
                savedStack.setCreated(new Date().getTime());
                save(savedStack);
                savedStack = stackRepository.findById(savedStack.getId());
            }
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

    public void delete(Long id, IdentityUser user, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByIdInAccount(id, user.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        delete(stack, deleteDependencies);
    }

    public void forceDelete(Long id, IdentityUser user, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByIdInAccount(id, user.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format("Stack '%s' not found", id));
        }
        forceDelete(stack, deleteDependencies);
    }

    public void removeInstance(IdentityUser user, Long stackId, String instanceId) {
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

    @Transactional(TxType.NEVER)
    public void updateStatus(Long stackId, StatusRequest status, boolean updateCluster) {
        Stack stack = getById(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        }
        if (BYOS.equals(stack.cloudPlatform()) && status.equals(FULL_SYNC)) {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISIONED);
            return;
        } else if (BYOS.equals(stack.cloudPlatform())) {
            LOGGER.warn("The status of a 'Bring your own stack' type of infrastructure cannot be changed.");
            return;
        }
        switch (status) {
            case SYNC:
                sync(stack, false);
                break;
            case FULL_SYNC:
                sync(stack, true);
                break;
            case REPAIR_FAILED_NODES:
                repairFailedNodes(stack);
                break;
            case STOPPED:
                stop(stack, cluster, updateCluster);
                break;
            case STARTED:
                start(stack, cluster, updateCluster);
                break;
            default:
                throw new BadRequestException("Cannot update the status of stack because status request not valid.");
        }
    }

    private Set<StackResponse> convertStacks(Set<Stack> stacks) {
        return (Set<StackResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackResponse.class)));
    }

    private Set<AutoscaleStackResponse> convertStacksForAutoscale(Set<Stack> stacks) {
        return (Set<AutoscaleStackResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(AutoscaleStackResponse.class)));
    }

    private void repairFailedNodes(Stack stack) {
        LOGGER.warn("Received request to replace failed nodes: " + stack.getId());
        flowManager.triggerManualRepairFlow(stack.getId());
    }

    private void sync(Stack stack, boolean full) {
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

    private void stop(Stack stack, Cluster cluster, boolean updateCluster) {
        if (cluster != null && cluster.isStopInProgress()) {
            setStackStatusToStopRequested(stack);
        } else {
            triggerStackStopIfNeeded(stack, cluster, updateCluster);
        }
    }

    private void triggerStackStopIfNeeded(Stack stack, Cluster cluster, boolean updateCluster) {
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
            if (updateCluster) {
                setStackStatusToStopRequested(stack);
                ambariClusterService.updateStatus(stack.getId(), StatusRequest.STOPPED);
            } else {
                throw new BadRequestException("Cannot update the status of stack '1' to STOPPED, because the cluster is not in STOPPED state.");
            }
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED);
            flowManager.triggerStackStop(stack.getId());
        }
    }

    private void setStackStatusToStopRequested(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.");
        String message = cloudbreakMessagesService.getMessage(Msg.STACK_STOP_REQUESTED.code());
        eventService.fireCloudbreakEvent(stack.getId(), STOP_REQUESTED.name(), message);
    }

    private void start(Stack stack, Cluster cluster, boolean updateCluster) {
        if (stack.isAvailable()) {
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.STACK_START_IGNORED.code());
            LOGGER.info(statusDesc);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), statusDesc);
        } else if ((!stack.isStopped() || (cluster != null && !cluster.isStopped())) && !stack.isStartFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of stack '%s' to STARTED, because it isn't in STOPPED state.", stack.getId()));
        } else if (stack.isStopped() || stack.isStartFailed()) {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
            flowManager.triggerStackStart(stack.getId());
            if (updateCluster) {
                ambariClusterService.updateStatus(stack.getId(), StatusRequest.STARTED);
            }
        }
    }

    public void updateNodeCount(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, boolean withClusterEvent) {
        Stack stack = get(stackId);
        validateStackStatus(stack);
        validateInstanceGroup(stack, instanceGroupAdjustmentJson.getInstanceGroup());
        validateScalingAdjustment(instanceGroupAdjustmentJson, stack);
        if (withClusterEvent) {
            validateHostGroupAdjustment(instanceGroupAdjustmentJson, stack, instanceGroupAdjustmentJson.getScalingAdjustment());
        }
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPSCALE_REQUESTED);
            flowManager.triggerStackUpscale(stack.getId(), instanceGroupAdjustmentJson, withClusterEvent);
        } else {
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DOWNSCALE_REQUESTED);
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

    public void validateStack(StackValidation stackValidation, boolean validateBlueprint) {
        if (stackValidation.getNetwork() != null) {
            networkConfigurationValidator.validateNetworkForStack(stackValidation.getNetwork(), stackValidation.getInstanceGroups());
        }
        if (validateBlueprint) {
            blueprintValidator.validateBlueprintForStack(stackValidation.getBlueprint(), stackValidation.getHostGroups(), stackValidation.getInstanceGroups());
        }
    }

    public void validateOrchestrator(Orchestrator orchestrator) {
        try {
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
            containerOrchestrator.validateApiEndpoint(new OrchestrationCredential(orchestrator.getApiEndpoint(), orchestrator.getAttributes().getMap()));
        } catch (CloudbreakException e) {
            throw new BadRequestException(String.format("Invalid orchestrator type: %s", e.getMessage()));
        } catch (CloudbreakOrchestratorException e) {
            throw new BadRequestException(String.format("Error occurred when trying to reach orchestrator API: %s", e.getMessage()));
        }
    }

    @Transactional(TxType.NEVER)
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

    private void validateHostGroupAdjustment(InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, Stack stack, Integer adjustment) {
        Blueprint blueprint = stack.getCluster().getBlueprint();
        Optional<HostGroup> hostGroup = stack.getCluster().getHostGroups().stream()
                .filter(input -> input.getConstraint().getInstanceGroup().getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup())).findFirst();
        if (!hostGroup.isPresent()) {
            throw new BadRequestException(String.format("Instancegroup '%s' not found or not part of stack '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
        blueprintValidator.validateHostGroupScalingRequest(blueprint, hostGroup.get(), adjustment);
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
    }

    private void delete(Stack stack, Boolean deleteDependencies) {
        authorizationService.hasWritePermission(stack);
        LOGGER.info("Stack delete requested.");
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerTermination(stack.getId(), deleteDependencies);
        } else {
            LOGGER.info("Stack is already deleted.");
        }
    }

    private void forceDelete(Stack stack, Boolean deleteDependencies) {
        authorizationService.hasWritePermission(stack);
        LOGGER.info("Stack forced delete requested.");
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerForcedTermination(stack.getId(), deleteDependencies);
        } else {
            LOGGER.info("Stack is already deleted.");
        }
    }

    private enum Msg {
        STACK_STOP_IGNORED("stack.stop.ignored"),
        STACK_START_IGNORED("stack.start.ignored"),
        STACK_STOP_REQUESTED("stack.stop.requested");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    private void addTemplateForStack(Stack stack, String template) {
        StackTemplate stackTemplate = new StackTemplate(template, cbVersion);
        try {
            Component stackTemplateComponent = new Component(ComponentType.STACK_TEMPLATE, ComponentType.STACK_TEMPLATE.name(), new Json(stackTemplate), stack);
            componentConfigProvider.store(stackTemplateComponent);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not create Cloudbreak details component.", e);
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
