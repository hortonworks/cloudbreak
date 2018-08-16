package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;
import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
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
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Service
public class StackService {

    private static final String SSH_USER_CB = "cloudbreak";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final String STACK_NOT_FOUND_EXCEPTION_FORMAT_TEXT = "Stack '%s' has not found";

    private static final String STACK_NOT_FOUND_BY_ID_EXCEPTION_FORMAT_TEXT = "Stack not found by id '%d'";

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
    private ClusterService clusterService;

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

    @Inject
    private OrganizationService organizationService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    public Set<StackResponse> retrieveStacksByOrganizationId(Long organizationId) {
        try {
            return transactionService.required(() ->
                    convertStacks(stackRepository.findForOrganizationIdWithLists(organizationId)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackResponse findStackByNameAndOrganizationId(String name, Long organizationId) {
        try {
            return transactionService.required(() ->
                    conversionService.convert(stackRepository
                            .findByNameAndOrganizationIdWithLists(name, organizationId), StackResponse.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<StackResponse> retrieveStacksForDefaultOrganization() {
        try {
            Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
            return transactionService.required(() ->
                    convertStacks(stackRepository.findForOrganizationIdWithLists(organization.getId())));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackResponse getJsonById(Long id, Collection<String> entry) {
        try {
            return transactionService.required(() -> {
                Stack stack = getByIdWithLists(id);
                StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entry);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack get(Long id) {
        try {
            return transactionService.required(() -> {
                Stack stack = stackRepository.findById(id).orElseThrow(notFound("Stack", id));
                return stack;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Stack getForAutoscale(Long id) {
        return getByIdWithoutAuth(id);
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        try {
            return transactionService.required(() -> {
                Set<Stack> aliveOnes = stackRepository.findAliveOnesWithoutAuthorization();
                return convertStacksForAutoscale(aliveOnes);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Stack> findClustersConnectedToDatalake(Long stackId) {
        return stackRepository.findEphemeralClustersWithoutAuth(stackId);
    }

    public Stack getByIdWithLists(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_ID_EXCEPTION_FORMAT_TEXT, id));
        }
        return retStack;
    }

    public Stack getByIdWithListsWithoutAuthorization(Long id) {
        Stack retStack;
        try {
            retStack = transactionService.required(() -> stackRepository.findOneWithListsWithoutAuthorization(id));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        if (retStack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_ID_EXCEPTION_FORMAT_TEXT, id));
        }
        return retStack;
    }

    public Stack getById(Long id) {
        return stackRepository.findById(id).orElseThrow(notFound("Stack", id));
    }

    public Stack getByIdWithoutAuth(Long id) {
        try {
            return transactionService.required(() -> stackRepository.findByIdWithoutAuth(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackView getViewByIdWithoutAuth(Long id) {
        try {
            return transactionService.required(() -> stackViewRepository.findByIdWithoutAuthorization(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackView getViewById(Long id) {
        return stackViewRepository.findById(id).orElseThrow(notFound("Stack", id));
    }

    public StackView getViewByNameInOrg(String name, Long organizationId) {
        Organization organization = organizationService.get(organizationId);
        StackView stackView = stackViewRepository.findByNameAndOrganization(name, organization);
        if (stackView == null) {
            throw new NotFoundException(String.format("Stack not found with name '%s'", name));
        }
        return stackView;
    }

    public StackStatus getCurrentStatusByStackId(long stackId) {
        return stackStatusRepository.findFirstByStackIdOrderByCreatedDesc(stackId);
    }

    public StackResponse getByAmbariAddress(String ambariAddress) {
        try {
            return transactionService.required(() -> conversionService.convert(stackRepository.findByAmbari(ambariAddress), StackResponse.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackResponse getStackByNameInDefaultOrg(String name, Collection<String> entries) {
        try {
            return transactionService.required(() -> {
                Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
                Stack stack = stackRepository.findByNameAndOrganizationIdWithLists(name, organization.getId());
                StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entries);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack getByNameInDefaultOrgWithLists(String name) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return stackRepository.findByNameAndOrganizationIdWithLists(name, organization.getId());
    }

    public StackResponse getStackByNameInOrganization(String name, Organization organization, Collection<String> entries) {
        try {
            return transactionService.required(() -> {
                Stack stack = stackRepository.findByNameInOrganizationWithLists(name, organization);
                StackResponse stackResponse = conversionService.convert(stack, StackResponse.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entries);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV2Request getStackRequestByNameInDefaultOrg(String name) {
        try {
            return transactionService.required(() -> {
                Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
                Stack stack = stackRepository.findByNameAndOrganizationIdWithLists(name, organization.getId());
                return conversionService.convert(stack, StackV2Request.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV2Request getStackRequestByNameInOrg(String name, Long organizationId) {
        try {
            return transactionService.required(() -> {
                Stack stack = stackRepository.findByNameAndOrganizationIdWithLists(name, organizationId);
                return conversionService.convert(stack, StackV2Request.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack getByNameInDefaultOrg(String name) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return stackRepository.findByNameAndOrganizationId(name, organization.getId());
    }

    public Stack getByNameInOrg(String name, Long organizationId) {
        return stackRepository.findByNameAndOrganizationId(name, organizationId);
    }

    public void delete(String name, Long organizationId, Boolean forced, Boolean deleteDependencies) {
        Stack stack = stackRepository.findByNameAndOrganizationId(name, organizationId);
        delete(stack, forced, deleteDependencies);
    }

    public Stack create(IdentityUser identityUser, Stack stack, String platformString, StatedImage imgFromCatalog) {
        stack.setOwner(identityUser.getUserId());
        stack.setAccount(identityUser.getAccount());
        stack.setGatewayPort(nginxPort);

        if (stack.getOrganization() == null) {
            Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
            stack.setOrganization(organization);
        }

        setPlatformVariant(stack);
        String stackName = stack.getName();
        MDCBuilder.buildMdcContext(stack);
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
            Stack savedStack = stackRepository.save(stack);


            LOGGER.info("Stackrepository save took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            addTemplateForStack(savedStack, template);
            LOGGER.info("Save cluster template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            addCloudbreakDetailsForStack(savedStack);
            LOGGER.info("Add Cloudbreak template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            MDCBuilder.buildMdcContext(savedStack);

            start = System.currentTimeMillis();
            instanceGroupRepository.saveAll(savedStack.getInstanceGroups());
            LOGGER.info("Instance groups saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            instanceMetaDataRepository.saveAll(savedStack.getInstanceMetaDataAsList());
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
            return savedStack;
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error("Cloudbreak Image not found", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.error("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }

    private void setPlatformVariant(Stack stack) {
        stack.setPlatformVariant(connector.checkAndGetPlatformVariant(stack).value());
    }

    public void delete(Long id, Boolean forced, Boolean deleteDependencies) {
        stackRepository.findById(id).ifPresent(stack -> {
            delete(stack, forced, deleteDependencies);
        });
    }

    public void removeInstance(Long stackId, Long organizationId, String instanceId) {
        removeInstance(stackId, organizationId, instanceId, false);
    }

    public void removeInstance(Long stackId, Long organizationId, String instanceId, boolean forced) {
        Stack stack = getById(stackId);
        InstanceMetaData metaData = validateInstanceForDownscale(instanceId, stack, organizationId);
        flowManager.triggerStackRemoveInstance(stackId, metaData.getInstanceGroupName(), metaData.getPrivateId(), forced);
    }

    public void removeInstance(Stack stack, Long organizationId, String instanceId, boolean forced) {
        InstanceMetaData metaData = validateInstanceForDownscale(instanceId, stack, organizationId);
        flowManager.triggerStackRemoveInstance(stack.getId(), metaData.getInstanceGroupName(), metaData.getPrivateId(), forced);
    }

    public void removeInstances(Long stackId, Long organizationId, Set<String> instanceIds) {
        Stack stack = getById(stackId);
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData metaData = validateInstanceForDownscale(instanceId, stack, organizationId);
            instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
        }
        flowManager.triggerStackRemoveInstances(stackId, instanceIdsByHostgroupMap);
    }

    public void updateStatus(Long stackId, StatusRequest status, boolean updateCluster) {
        Stack stack = getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterService.findOneWithLists(stack.getCluster().getId());
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

    private InstanceMetaData validateInstanceForDownscale(String instanceId, Stack stack, Long organizationId) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        InstanceMetaData metaData = instanceMetaDataRepository.findByInstanceId(stack.getId(), instanceId);
        if (metaData == null) {
            throw new NotFoundException(String.format("Metadata for instance %s has not found.", instanceId));
        }
        downscaleValidatorService.checkInstanceIsTheAmbariServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack.getCluster());
        return metaData;
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
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
        LOGGER.warn("Received request to replace failed nodes: " + stack.getId());
        flowManager.triggerManualRepairFlow(stack.getId());
    }

    private void sync(Stack stack, boolean full) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
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
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
        if (!isStopNeeded(stack)) {
            return;
        }
        if (cluster != null && !cluster.isStopped() && !stack.isStopFailed()) {
            if (!updateCluster) {
                throw new BadRequestException(String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.",
                        stack.getName()));
            } else if (cluster.isClusterReadyForStop() || cluster.isStopFailed()) {
                setStackStatusToStopRequested(stack);
                clusterService.updateStatus(stack.getId(), StatusRequest.STOPPED);
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
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
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
                clusterService.updateStatus(startStack, StatusRequest.STARTED);
            }
        }
    }

    public void updateNodeCount(Stack stack1, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, boolean withClusterEvent) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack1.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
        try {
            transactionService.required(() -> {
                Stack stackWithLists = getByIdWithLists(stack1.getId());
                validateStackStatus(stackWithLists);
                validateInstanceGroup(stackWithLists, instanceGroupAdjustmentJson.getInstanceGroup());
                validateScalingAdjustment(instanceGroupAdjustmentJson, stackWithLists);
                if (withClusterEvent) {
                    validateClusterStatus(stackWithLists);
                    validateHostGroupAdjustment(instanceGroupAdjustmentJson, stackWithLists, instanceGroupAdjustmentJson.getScalingAdjustment());
                }
                if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.UPSCALE_REQUESTED);
                    flowManager.triggerStackUpscale(stackWithLists.getId(), instanceGroupAdjustmentJson, withClusterEvent);
                } else {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED);
                    flowManager.triggerStackDownscale(stackWithLists.getId(), instanceGroupAdjustmentJson);
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
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
        return stackRepository.findAllAliveWithoutAuth();
    }

    public List<Stack> getByStatuses(List<Status> statuses) {
        return stackRepository.findByStatusesWithoutAuth(statuses);
    }

    public long countByCredential(Credential credential) {
        Long count = stackRepository.countByCredential(credential);
        return count == null ? 0L : count;
    }

    public long countByFlexSubscription(FlexSubscription subscription) {
        Long count = stackRepository.countByFlexSubscription(subscription);
        return count == null ? 0L : count;
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

    public Set<Stack> findAllForOrganization(Long organizationId) {
        try {
            return transactionService.required(() -> stackRepository.findAllForOrganization(organizationId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void delete(Stack stack, Boolean forced, Boolean deleteDependencies) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(stack.getOrganization().getId(), OrganizationResource.STACK, Action.WRITE);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Stack delete requested.");
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerTermination(stack.getId(), forced, deleteDependencies);
        } else {
            LOGGER.info("Stack is already deleted.");
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

    public List<Object[]> getStatuses(Set<Long> stackIds) {
        return stackRepository.findStackStatusesWithoutAuth(stackIds);
    }

    public Set<Stack> getByNetwork(Network network) {
        return stackRepository.findByNetwork(network);
    }

    public List<Stack> getAllForTemplate(Long id) {
        return stackRepository.findAllStackForTemplateWithoutAuth(id);
    }

    public List<Stack> getAllAliveAndProvisioned() {
        return stackRepository.findAllAliveAndProvisionedWithoutAuth();
    }

    public Stack getForCluster(Long id) {
        return stackRepository.findStackForClusterWithoutAuth(id);
    }

    public void updateImage(Long stackId, Long organizationId, String imageId, String imageCatalogName, String imageCatalogUrl) {
        permissionCheckingUtils.checkPermissionByOrgIdForCurrentUser(organizationId, OrganizationResource.STACK, Action.WRITE);
        flowManager.triggerStackImageUpdate(stackId, imageId, imageCatalogName, imageCatalogUrl);
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
}
