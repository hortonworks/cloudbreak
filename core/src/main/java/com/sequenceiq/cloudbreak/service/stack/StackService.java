package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.SaltSecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Service
public class StackService {

    public static final List<String> REATTACH_COMPATIBLE_PLATFORMS = List.of(CloudConstants.AWS, CloudConstants.AZURE, CloudConstants.GCP);

    private static final String STACK_NOT_FOUND_EXCEPTION_ID_TXT = "Stack not found by id '%d'";

    private static final String STACK_NOT_FOUND_EXCEPTION_TXT = "Stack not found by name '%s'";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final String SSH_USER_CB = "cloudbreak";

    @Inject
    private NetworkConfigurationValidator networkConfigurationValidator;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private SaltSecurityConfigRepository saltSecurityConfigRepository;

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private OrchestratorRepository orchestratorRepository;

    @Inject
    private StackResponseDecorator stackResponseDecorator;

    @Inject
    private StackStatusRepository stackStatusRepository;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private StackViewRepository stackViewRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ImageService imageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${info.app.version:}")
    private String cbVersion;

    public Long countAliveByEnvironment(Environment environment) {
        return stackRepository.countAliveOnesByWorkspaceAndEnvironment(environment.getWorkspace().getId(), environment.getId());
    }

    public Set<StackV4Response> retrieveStacksByWorkspaceId(Long workspaceId) {
        try {
            return transactionService.required(() ->
                    converterUtil.convertAllAsSet(stackRepository.findForWorkspaceIdWithLists(workspaceId), StackV4Response.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack findStackByNameAndWorkspaceId(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId);
    }

    public StackV4Response getJsonById(Long id, Collection<String> entry) {
        try {
            return transactionService.required(() -> {
                Stack stack = getByIdWithLists(id);
                StackV4Response stackResponse = converterUtil.convert(stack, StackV4Response.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entry);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack get(Long id) {
        try {
            return transactionService.required(() -> stackRepository.findById(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Long getWorkspaceId(Long stackId) {
        return stackRepository.findWorkspaceIdById(stackId);
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Tenant getTenant(Long stackId) {
        Workspace workspace = stackRepository.findWorkspaceById(stackId);
        return workspace.getTenant();
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public Set<AutoscaleStackV4Response> getAllForAutoscale() {
        try {
            return transactionService.required(() -> converterUtil.convertAllAsSet(stackRepository.findAliveOnesWithAmbari(), AutoscaleStackV4Response.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Stack> findClustersConnectedToDatalakeByDatalakeResourceId(Long datalakeResourceId) {
        return stackRepository.findEphemeralClusters(datalakeResourceId);
    }

    public Set<Stack> findClustersConnectedToDatalakeByDatalakeStackId(Long datalakeStackId) {
        DatalakeResources datalakeResources = datalakeResourcesService.getDatalakeResourcesByDatalakeStackId(datalakeStackId);
        return datalakeResources == null ? Collections.emptySet() : stackRepository.findEphemeralClusters(datalakeResources.getId());
    }

    public Stack getByIdWithListsInTransaction(Long id) {
        Stack stack;
        try {
            stack = transactionService.required(() -> stackRepository.findOneWithLists(id));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_ID_TXT, id));
        }
        return stack;
    }

    public Stack getById(Long id) {
        return stackRepository.findById(id).orElseThrow(notFound("Stack", id));
    }

    public Optional<Stack> findById(Long id) {
        return stackRepository.findById(id);
    }

    public Stack getByIdWithTransaction(Long id) {
        try {
            return transactionService.required(() -> stackRepository.findById(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackView getViewByIdWithoutAuth(Long id) {
        try {
            return transactionService.required(() -> stackViewRepository.findById(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackStatus getCurrentStatusByStackId(long stackId) {
        return stackStatusRepository.findFirstByStackIdOrderByCreatedDesc(stackId);
    }

    public StackV4Response getByAmbariAddress(String ambariAddress) {
        try {
            return transactionService.required(() -> converterUtil.convert(stackRepository.findByAmbari(ambariAddress), StackV4Response.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV4Response getByNameInWorkspaceWithEntries(String name, Long workspaceId, Set<String> entries, User user) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = workspaceService.get(workspaceId, user);
                Stack stack = stackRepository.findByNameAndWorkspaceIdWithLists(name, workspace.getId());
                if (stack == null) {
                    throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_TXT, name));
                }
                StackV4Response stackResponse = converterUtil.convert(stack, StackV4Response.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack, entries);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV4Request getStackRequestByNameInWorkspaceId(String name, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Stack stack = stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId);
                if (stack == null) {
                    throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_TXT, name));
                }
                return converterUtil.convert(stack, StackV4Request.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack getByNameInWorkspace(String name, Long workspaceId) {
        return Optional.ofNullable(stackRepository.findByNameAndWorkspaceId(name, workspaceId))
                .orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_TXT, name)));
    }

    public Stack getByNameInWorkspaceWithLists(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId);
    }

    public Stack create(Stack stack, String platformString, StatedImage imgFromCatalog, User user, Workspace workspace) {
        if (stack.getGatewayPort() == null) {
            stack.setGatewayPort(nginxPort);
        }
        stack.setCreator(user);

        stack.setWorkspace(workspace);

        setPlatformVariant(stack);
        String stackName = stack.getName();
        MDCBuilder.buildMdcContext(stack);
        try {
            if (!stack.getStackAuthentication().passwordAuthenticationRequired()
                    && !Strings.isNullOrEmpty(stack.getStackAuthentication().getPublicKey())) {
                long start = System.currentTimeMillis();
                rsaPublicKeyValidator.validate(stack.getStackAuthentication().getPublicKey());
                LOGGER.debug("RSA key has been validated in {} ms fot stack {}", System.currentTimeMillis() - start, stackName);
            }
            if (stack.getOrchestrator() != null) {
                orchestratorRepository.save(stack.getOrchestrator());
            }
            stack.getStackAuthentication().setLoginUserName(SSH_USER_CB);

            long start = System.currentTimeMillis();
            String template = connector.getTemplate(stack);
            LOGGER.debug("Get cluster template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            Stack savedStack = stackRepository.save(stack);
            LOGGER.debug("Stackrepository save took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            addTemplateForStack(savedStack, template);
            LOGGER.debug("Save cluster template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            addCloudbreakDetailsForStack(savedStack);
            LOGGER.debug("Add Cloudbreak template took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            MDCBuilder.buildMdcContext(savedStack);

            start = System.currentTimeMillis();
            instanceGroupRepository.saveAll(savedStack.getInstanceGroups());
            LOGGER.debug("Instance groups saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            instanceMetaDataRepository.saveAll(savedStack.getInstanceMetaDataAsList());
            LOGGER.debug("Instance metadatas saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            SecurityConfig securityConfig = tlsSecurityService.generateSecurityKeys(workspace);
            LOGGER.debug("Generating security keys took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            securityConfig.setStack(savedStack);
            start = System.currentTimeMillis();
            saltSecurityConfigRepository.save(securityConfig.getSaltSecurityConfig());
            securityConfigRepository.save(securityConfig);
            LOGGER.debug("Security config save took {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            savedStack.setSecurityConfig(securityConfig);

            start = System.currentTimeMillis();
            imageService.create(savedStack, platformString, connector.getPlatformParameters(savedStack), imgFromCatalog);
            LOGGER.debug("Image creation took {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            return savedStack;
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Cloudbreak Image not found", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.info("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }

    private void setPlatformVariant(Stack stack) {
        stack.setPlatformVariant(connector.checkAndGetPlatformVariant(stack).value());
    }

    public void delete(Long id, Boolean forced, Boolean deleteDependencies, User user) {
        stackRepository.findById(id).map(stack -> {
            delete(stack.getName(), stack.getWorkspace().getId(), forced, deleteDependencies, user);
            return stack;
        }).orElseThrow(notFound("Stack", id));
    }

    public void delete(String name, Long workspaceId, Boolean forced, Boolean deleteDependencies, User user) {
        Optional.ofNullable(stackRepository.findByNameAndWorkspaceId(name, workspaceId)).map(stack -> {
            delete(stack, forced, deleteDependencies, user);
            return stack;
        }).orElseThrow(notFound("Stack", name));
    }

    public void removeInstance(Stack stack, Long workspaceId, String instanceId, boolean forced, User user) {
        InstanceMetaData metaData = validateInstanceForDownscale(instanceId, stack, workspaceId, user);
        flowManager.triggerStackRemoveInstance(stack.getId(), metaData.getInstanceGroupName(), metaData.getPrivateId(), forced);
    }

    public void updateStatus(Long stackId, StatusRequest status, boolean updateCluster, User user) {
        Stack stack = getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterService.findOneWithLists(stack.getCluster().getId());
        }
        switch (status) {
            case SYNC:
                sync(stack, false, user);
                break;
            case FULL_SYNC:
                sync(stack, true, user);
                break;
            case REPAIR_FAILED_NODES:
                repairFailedNodes(stack, user);
                break;
            case STOPPED:
                stop(stack, cluster, updateCluster, user);
                break;
            case STARTED:
                start(stack, cluster, updateCluster, user);
                break;
            default:
                throw new BadRequestException("Cannot update the status of stack because status request not valid.");
        }
    }

    public void updateNodeCount(Stack stack1, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, boolean withClusterEvent, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack1.getWorkspace().getId(), WorkspaceResource.STACK, Action.WRITE, user);
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
            if (e.getCause() instanceof BadRequestException) {
                throw e.getCause();
            }
            throw new TransactionRuntimeExecutionException(e);
        }

    }

    public void updateMetaDataStatusIfFound(Long id, String hostName, InstanceStatus status) {
        InstanceMetaData metaData = instanceMetaDataRepository.findHostInStack(id, hostName);
        if (metaData == null) {
            LOGGER.debug("Metadata not found on stack:'{}' with hostname: '{}'.", id, hostName);
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

    public Optional<InstanceMetaData> getInstanceMetadata(List<InstanceMetaData> instanceMetaDataList, Long privateId) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateId.equals(instanceMetaData.getPrivateId()))
                .findFirst();
    }

    public void validateStack(StackValidation stackValidation) {
        if (stackValidation.getNetwork() != null) {
            networkConfigurationValidator.validateNetworkForStack(stackValidation.getNetwork(), stackValidation.getInstanceGroups());
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

    public List<Stack> getByStatuses(List<Status> statuses) {
        return stackRepository.findByStatuses(statuses);
    }

    public long countByFlexSubscription(FlexSubscription subscription) {
        Long count = stackRepository.countByFlexSubscription(subscription);
        return count == null ? 0L : count;
    }

    public Set<String> findDatalakeStackNamesByWorkspaceAndEnvironment(Long workspaceId, Long envId) {
        return stackRepository.findDatalakeStackNamesByWorkspaceAndEnvironment(workspaceId, envId);
    }

    public Set<String> findWorkloadStackNamesByWorkspaceAndEnvironment(Long workspaceId, Long envId) {
        return stackRepository.findWorkloadStackNamesByWorkspaceAndEnvironment(workspaceId, envId);
    }

    public List<Object[]> getStatuses(Set<Long> stackIds) {
        return stackRepository.findStackStatusesWithoutAuth(stackIds);
    }

    public Set<Stack> getByNetwork(Network network) {
        return stackRepository.findByNetwork(network);
    }

    public List<Stack> getAllForTemplate(Long id) {
        return stackRepository.findAllStackForTemplate(id);
    }

    public Stack getForCluster(Long id) {
        return stackRepository.findStackForCluster(id);
    }

    public void updateImage(Long stackId, Long workspaceId, String imageId, String imageCatalogName, String imageCatalogUrl, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        flowManager.triggerStackImageUpdate(stackId, imageId, imageCatalogName, imageCatalogUrl);
    }

    public Set<Stack> findAllForWorkspace(Long workspaceId) {
        try {
            return transactionService.required(() -> stackRepository.findAllForWorkspace(workspaceId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void delete(Stack stack, Boolean forced, Boolean deleteDependencies) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Stack delete requested.");
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerTermination(stack.getId(), forced, deleteDependencies);
        } else {
            LOGGER.debug("Stack is already deleted.");
        }
    }

    private InstanceMetaData validateInstanceForDownscale(String instanceId, Stack stack, Long workspaceId, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(workspaceId, WorkspaceResource.STACK, Action.WRITE, user);
        InstanceMetaData metaData = instanceMetaDataRepository.findByInstanceId(stack.getId(), instanceId);
        if (metaData == null) {
            throw new NotFoundException(String.format("Metadata for instance %s has not found.", instanceId));
        }
        downscaleValidatorService.checkInstanceIsTheAmbariServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack.getCluster());
        return metaData;
    }

    private Stack getByIdWithLists(Long id) {
        Stack retStack = stackRepository.findOneWithLists(id);
        if (retStack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_EXCEPTION_ID_TXT, id));
        }
        return retStack;
    }

    private void repairFailedNodes(Stack stack, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack.getWorkspace().getId(), WorkspaceResource.STACK, Action.WRITE, user);
        LOGGER.debug("Received request to replace failed nodes: " + stack.getId());
        flowManager.triggerManualRepairFlow(stack.getId());
    }

    private void sync(Stack stack, boolean full, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack.getWorkspace().getId(), WorkspaceResource.STACK, Action.WRITE, user);
        if (!stack.isDeleteInProgress() && !stack.isStackInDeletionPhase() && !stack.isModificationInProgress()) {
            if (full) {
                flowManager.triggerFullSync(stack.getId());
            } else {
                flowManager.triggerStackSync(stack.getId());
            }
        } else {
            LOGGER.debug("Stack could not be synchronized in {} state!", stack.getStatus());
        }
    }

    private void stop(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        if (cluster != null && cluster.isStopInProgress()) {
            setStackStatusToStopRequested(stack);
        } else {
            triggerStackStopIfNeeded(stack, cluster, updateCluster, user);
        }
    }

    private void triggerStackStopIfNeeded(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack.getWorkspace().getId(), WorkspaceResource.STACK, Action.WRITE, user);
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
            LOGGER.debug(statusDesc);
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

    private void start(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack.getWorkspace().getId(), WorkspaceResource.STACK, Action.WRITE, user);
        if (stack.isAvailable()) {
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.STACK_START_IGNORED.code());
            LOGGER.debug(statusDesc);
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

    private Set<InstanceMetaData> getInstanceMetadatasForHostNames(List<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> hostNames.contains(instanceMetaData.getDiscoveryFQDN()))
                .collect(Collectors.toSet());
    }

    private void validateScalingAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack) {
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

    private void validateHostGroupAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack, Integer adjustment) {
        ClusterDefinition clusterDefinition = stack.getCluster().getClusterDefinition();
        Optional<HostGroup> hostGroup = stack.getCluster().getHostGroups().stream()
                .filter(input -> input.getConstraint().getInstanceGroup().getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup())).findFirst();
        if (!hostGroup.isPresent()) {
            throw new BadRequestException(String.format("Instancegroup '%s' not found or not part of stack '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
        ambariBlueprintValidator.validateHostGroupScalingRequest(clusterDefinition, hostGroup.get(), adjustment);
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
            throw new BadRequestException(String.format("Cluster '%s' is currently in '%s' state. Node count can only be updated if it's not available.",
                    cluster.getName(), cluster.getStatus()));
        }
    }

    private void validateInstanceGroup(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(String.format("Stack '%s' does not have an instanceGroup named '%s'.", stack.getName(), instanceGroupName));
        }
    }

    private void delete(Stack stack, Boolean forced, Boolean deleteDependencies, User user) {
        permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack.getWorkspace().getId(), WorkspaceResource.STACK, Action.WRITE, user);
        checkStackHasNoAttachedClusters(stack);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Stack delete requested.");
        if (!stack.isDeleteCompleted()) {
            flowManager.triggerTermination(stack.getId(), forced, deleteDependencies);
        } else {
            LOGGER.debug("Stack is already deleted.");
        }
    }

    private void checkStackHasNoAttachedClusters(Stack stack) {
        Set<Stack> attachedOnes = findClustersConnectedToDatalakeByDatalakeStackId(stack.getId());
        if (!attachedOnes.isEmpty()) {
            throw new BadRequestException(String.format("Stack has attached clusters! Please remove them before try to delete this one. %nThe following "
                            + "clusters has to be deleted before terminating the datalake cluster: %s",
                    String.join(", ", attachedOnes.stream().map(Stack::getName).collect(Collectors.toSet()))));
        }
    }

    private void addTemplateForStack(Stack stack, String template) {
        StackTemplate stackTemplate = new StackTemplate(template, cbVersion);
        try {
            Component stackTemplateComponent = new Component(ComponentType.STACK_TEMPLATE, ComponentType.STACK_TEMPLATE.name(), new Json(stackTemplate), stack);
            componentConfigProvider.store(stackTemplateComponent);
        } catch (JsonProcessingException e) {
            LOGGER.info("Could not create Cloudbreak details component.", e);
        }
    }

    private void addCloudbreakDetailsForStack(Stack stack) {
        CloudbreakDetails cbDetails = new CloudbreakDetails(cbVersion);
        try {
            Component cbDetailsComponent = new Component(ComponentType.CLOUDBREAK_DETAILS, ComponentType.CLOUDBREAK_DETAILS.name(), new Json(cbDetails), stack);
            componentConfigProvider.store(cbDetailsComponent);
        } catch (JsonProcessingException e) {
            LOGGER.info("Could not create Cloudbreak details component.", e);
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
}
