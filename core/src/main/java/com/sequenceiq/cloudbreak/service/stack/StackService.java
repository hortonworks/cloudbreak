package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.view.StackView;
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
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterService;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Service
public class StackService {

    private static final String SSH_USER_CB = "cloudbreak";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final String STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT = "Stack '%s' has not found";

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackViewRepository stackViewRepository;

    @Inject
    private StackStatusRepository stackStatusRepository;

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
    private StackResponseDecorator stackResponseDecorator;

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

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private TransactionService transactionService;

    public Set<StackResponse> retrievePrivateStacks(IdentityUser user) {
        return convertStacks(stackRepository.findForUserWithLists(user.getUserId()));
    }

    public Set<StackResponse> retrieveAccountStacks(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? convertStacks(stackRepository.findAllInAccountWithLists(user.getAccount()))
                : convertStacks(stackRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount()));
    }

    public Set<Stack> retrieveAccountStacks(String account) {
        return stackRepository.findAllInAccount(account);
    }

    public Set<Stack> retrieveOwnerStacks(String owner) {
        return stackRepository.findForUser(owner);
    }

    public StackResponse getJsonById(Long id, Collection<String> entry) {
        Stack stack = getByIdWithLists(id);
        authorizationService.hasReadPermission(stack);
        StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
        stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entry);
        return stackResponse;
    }

    public Stack get(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, id));
        }
        authorizationService.hasReadPermission(stack);
        return stack;
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Stack getForAutoscale(Long id) {
        Stack stack = stackRepository.findOne(id);
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, id));
        }
        return stack;
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        Set<Stack> aliveOnes = stackRepository.findAliveOnes();
        return convertStacksForAutoscale(aliveOnes);
    }

    public Set<Stack> findClustersConnectedToDatalake(Long stackId) {
        return stackRepository.findEphemeralClusters(stackId);
    }

    public Stack getByIdWithLists(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, id));
        }
        return retStack;
    }

    public Stack getById(Long id) {
        Stack retStack = stackRepository.findOne(id);
        if (retStack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, id));
        }
        return retStack;
    }

    public StackView getByIdView(Long id) {
        StackView retStack = stackViewRepository.findOne(id);
        if (retStack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, id));
        }
        return retStack;
    }

    public StackStatus getCurrentStatusByStackId(long stackId) {
        return stackStatusRepository.findFirstByStackIdOrderByCreatedDesc(stackId);
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
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, name));
        }
        return stack;
    }

    public StackResponse getPrivateStackJsonByName(String name, IdentityUser identityUser, Collection<String> entries) {
        Stack stack = stackRepository.findByNameInUserWithLists(name, identityUser.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, name));
        }
        StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
        stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entries);
        return stackResponse;
    }

    public StackResponse getPublicStackJsonByName(String name, IdentityUser identityUser, Collection<String> entries) {
        Stack stack = stackRepository.findByNameInAccountWithLists(name, identityUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, name));
        }
        authorizationService.hasReadPermission(stack);
        StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
        stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entries);
        return stackResponse;
    }

    public StackV2Request getStackRequestByName(String name, IdentityUser identityUser) {
        Stack stack = stackRepository.findByNameInAccountWithLists(name, identityUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, name));
        }
        authorizationService.hasReadPermission(stack);
        return conversionService.convert(stack, StackV2Request.class);
    }

    public Stack getPublicStack(String name, IdentityUser identityUser) {
        Stack stack = stackRepository.findByNameInAccount(name, identityUser.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, name));
        }
        authorizationService.hasReadPermission(stack);
        return stack;
    }

    public void delete(String name, IdentityUser user, Boolean forced, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByNameInAccountOrOwner(name, user.getAccount(), user.getUserId());
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, name));
        }
        delete(stack, forced, deleteDependencies);
    }

    public Stack create(IdentityUser user, Stack stack, String platformString, StatedImage imgFromCatalog) {
        stack.setOwner(user.getUserId());
        stack.setAccount(user.getAccount());
        stack.setGatewayPort(nginxPort);
        setPlatformVariant(stack);
        String stackName = stack.getName();
        MDCBuilder.buildMdcContext(stack);
        try {
            return transactionService.required(() -> {
                Stack savedStack;
                try {
                    if (!stack.getStackAuthentication().passwordAuthenticationRequired()
                            && !Strings.isNullOrEmpty(stack.getStackAuthentication().getPublicKey())) {
                        long start = System.currentTimeMillis();
                        rsaPublicKeyValidator.validate(stack.getStackAuthentication().getPublicKey());
                        LOGGER.info("RSA key has been validated in {} ms fot stack {}", System.currentTimeMillis() - start, stackName);
                    }
                    if (stack.getOrchestrator() != null) {
                        orchestratorRepository.save(stack.getOrchestrator());
                    }
                    stack.getStackAuthentication().setLoginUserName(SSH_USER_CB);

                    long start = System.currentTimeMillis();
                    String template = connector.getTemplate(stack);
                    LOGGER.info("Get cluster template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    savedStack = stackRepository.save(stack);
                    LOGGER.info("Stackrepository save took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    addTemplateForStack(savedStack, template);
                    LOGGER.info("Save cluster template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    addCloudbreakDetailsForStack(savedStack);
                    LOGGER.info("Add Cloudbreak template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    MDCBuilder.buildMdcContext(savedStack);

                    start = System.currentTimeMillis();
                    instanceGroupRepository.save(savedStack.getInstanceGroups());
                    LOGGER.info("Instance groups saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    instanceMetaDataRepository.save(savedStack.getInstanceMetaDataAsList());
                    LOGGER.info("Instance metadatas saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    SecurityConfig securityConfig = tlsSecurityService.storeSSHKeys();
                    LOGGER.info("Generating SSH keys took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    start = System.currentTimeMillis();
                    securityConfig.setSaltPassword(PasswordUtil.generatePassword());
                    securityConfig.setSaltBootPassword(PasswordUtil.generatePassword());
                    securityConfig.setKnoxMasterSecret(PasswordUtil.generatePassword());
                    LOGGER.info("Generating salt passwords took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                    securityConfig.setStack(savedStack);
                    start = System.currentTimeMillis();
                    securityConfigRepository.save(securityConfig);
                    LOGGER.info("Security config save took {} ms for stack {}", System.currentTimeMillis() - start, stackName);
                    savedStack.setSecurityConfig(securityConfig);

                    start = System.currentTimeMillis();
                    imageService.create(savedStack, platformString, connector.getPlatformParameters(savedStack), imgFromCatalog);
                    LOGGER.info("Image creation took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

                } catch (DataIntegrityViolationException ex) {
                    String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.STACK, getProperSqlErrorMessage(ex));
                    throw new BadRequestException(msg);
                } catch (CloudbreakImageNotFoundException e) {
                    LOGGER.error("Cloudbreak Image not found", e);
                    throw new CloudbreakApiException(e.getMessage(), e);
                } catch (CloudbreakImageCatalogException e) {
                    LOGGER.error("Cloudbreak Image Catalog error", e);
                    throw new CloudbreakApiException(e.getMessage(), e);
                }
                return savedStack;
            });
        } catch (TransactionExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private void setPlatformVariant(Stack stack) {
        stack.setPlatformVariant(connector.checkAndGetPlatformVariant(stack).value());
    }

    public void delete(Long id, IdentityUser user, Boolean forced, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByIdInAccount(id, user.getAccount());
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT, id));
        }
        delete(stack, forced, deleteDependencies);
    }

    public void removeInstance(@Nonnull IdentityUser user, Long stackId, String instanceId) {
        Stack stack = get(stackId);
        InstanceMetaData metaData = instanceMetaDataRepository.findByInstanceId(stackId, instanceId);
        if (metaData == null) {
            throw new NotFoundException(String.format("Metadata for instance %s has not found.", instanceId));
        } else {
            downscaleValidatorService.checkInstanceIsTheAmbariServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
            downscaleValidatorService.checkUserHasRightToTerminateInstance(stack.isPublicInAccount(), stack.getOwner(), user.getUserId(), stackId);
            flowManager.triggerStackRemoveInstance(stackId, metaData.getInstanceGroupName(), metaData.getPrivateId());
        }
    }

    public void removeInstances(IdentityUser user, Long stackId, Set<String> instanceIds) {
        Stack stack = get(stackId);
        Map<String, Set<Long>> groupToDiscoveryPrivateIdMap = new HashMap<>();
        for (String instanceId: instanceIds) {
            InstanceMetaData metadata = instanceMetaDataRepository.findByInstanceId(stackId, instanceId);
            if (metadata == null) {
                throw new NotFoundException(String.format("Metadata for instance %s not found.", instanceId));
            }
            if (!stack.isPublicInAccount() && !stack.getOwner().equals(user.getUserId())) {
                throw new BadRequestException(String.format("Private stack (%s) only modifiable by the owner.", stackId));
            }
            if (groupToDiscoveryPrivateIdMap.containsKey(metadata.getInstanceGroupName())) {
                Set<Long> privateIds = groupToDiscoveryPrivateIdMap.get(metadata.getInstanceGroupName());
                privateIds.add(metadata.getPrivateId());
                groupToDiscoveryPrivateIdMap.put(metadata.getInstanceGroupName(), privateIds);
            } else {
                Set<Long> privateIds = new LinkedHashSet<>();
                privateIds.add(metadata.getPrivateId());
                groupToDiscoveryPrivateIdMap.put(metadata.getInstanceGroupName(), privateIds);
            }
        }
        groupToDiscoveryPrivateIdMap.forEach((k, v) -> flowManager.triggerStackRemoveInstances(stackId, k, v));
    }

    public void updateStatus(Long stackId, StatusRequest status, boolean updateCluster) {
        Stack stack = getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
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
        if (!isStopNeeded(stack)) {
            return;
        }
        if (cluster != null && !cluster.isStopped() && !stack.isStopFailed()) {
            if (!updateCluster) {
                throw new BadRequestException(String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.",
                        stack.getName()));
            } else if (cluster.isClusterReadyForStop() || cluster.isStopFailed()) {
                setStackStatusToStopRequested(stack);
                ambariClusterService.updateStatus(stack.getId(), StatusRequest.STOPPED);
            } else {
                throw new BadRequestException(String.format("Cannot update the status of cluster '%s' to STOPPED, because the cluster's state is %s.",
                        cluster.getName(), cluster.getStatus()));
            }
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED);
            flowManager.triggerStackStop(stack.getId());
        }
    }

    private boolean isStopNeeded(Stack stack) {
        boolean result = true;
        StopRestrictionReason reason = stack.isInfrastructureStoppable();
        if (stack.isStopped()) {
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.STACK_STOP_IGNORED.code());
            LOGGER.info(statusDesc);
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), statusDesc);
            result = false;
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    String.format("Cannot stop a stack '%s'. Reason: %s", stack.getName(), reason.getReason()));
        } else if (!stack.isAvailable() && !stack.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.getName()));
        }
        return result;
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
                    String.format("Cannot update the status of stack '%s' to STARTED, because it isn't in STOPPED state.", stack.getName()));
        } else if (stack.isStopped() || stack.isStartFailed()) {
            Stack startStack = stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
            flowManager.triggerStackStart(stack.getId());
            if (updateCluster && cluster != null) {
                ambariClusterService.updateStatus(startStack, StatusRequest.STARTED);
            }
        }
    }

    public void updateNodeCount(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, boolean withClusterEvent) {
        Stack stack = getByIdWithLists(stackId);
        validateStackStatus(stack);
        validateInstanceGroup(stack, instanceGroupAdjustmentJson.getInstanceGroup());
        validateScalingAdjustment(instanceGroupAdjustmentJson, stack);
        if (withClusterEvent) {
            validateClusterStatus(stack);
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

    public void updateMetaDataStatusIfFound(Long id, String hostName, InstanceStatus status) {
        InstanceMetaData metaData = instanceMetaDataRepository.findHostInStack(id, hostName);
        if (metaData == null) {
            LOGGER.warn("Metadata not found on stack:'{}' with hostname: '{}'.", id, hostName);
        } else {
            metaData.setInstanceStatus(status);
            instanceMetaDataRepository.save(metaData);
        }
    }

    public List<String> getHostNamesForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        return getInstanceMetaDataForPrivateIds(instanceMetaDataList, privateIds).stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toList());
    }

    public List<String> getInstanceIdsForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        List<InstanceMetaData> instanceMetaDataForPrivateIds = getInstanceMetaDataForPrivateIds(instanceMetaDataList, privateIds);
        return instanceMetaDataForPrivateIds.stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toList());
    }

    public List<InstanceMetaData> getInstanceMetaDataForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateIds.contains(instanceMetaData.getPrivateId()))
                .collect(Collectors.toList());
    }

    public Set<Long> getPrivateIdsForHostNames(List<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
        return getInstanceMetadatasForHostNames(instanceMetaDataList, hostNames).stream()
                .map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getInstanceMetadatasForHostNames(List<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> hostNames.contains(instanceMetaData.getDiscoveryFQDN()))
                .collect(Collectors.toSet());
    }

    public Optional<InstanceMetaData> getInstanceMetadata(List<InstanceMetaData> instanceMetaDataList, Long privateId) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateId.equals(instanceMetaData.getPrivateId()))
                .findFirst();
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

    public Stack save(Stack stack) {
        return stackRepository.save(stack);
    }

    public List<Stack> getAllAlive() {
        return stackRepository.findAllAlive();
    }

    private void validateScalingAdjustment(InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, Stack stack) {
        if (0 == instanceGroupAdjustmentJson.getScalingAdjustment()) {
            throw new BadRequestException(String.format("Requested scaling adjustment on stack '%s' is 0. Nothing to do.", stack.getName()));
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
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.",
                    stack.getName(), stack.getStatus()));
        }
    }

    private void validateClusterStatus(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null && !cluster.isAvailable()) {
            throw new BadRequestException(String.format("Cluster '%s' is currently in '%s' state. Node count can only be updated if it's running.",
                    cluster.getName(), cluster.getStatus()));
        }
    }

    private void validateInstanceGroup(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(String.format("Stack '%s' does not have an instanceGroup named '%s'.", stack.getName(), instanceGroupName));
        }
    }

    public void delete(Stack stack) throws TransactionExecutionException {
        transactionService.required(() -> {
            stackRepository.delete(stack.getId());
            return null;
        });
    }

    private void delete(Stack stack, Boolean forced, Boolean deleteDependencies) {
        authorizationService.hasWritePermission(stack);
        LOGGER.info("Stack delete requested.");
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerTermination(stack.getId(), forced, deleteDependencies);
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
