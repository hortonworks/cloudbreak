package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonReader;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.converter.stack.StackIdViewToStackResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
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
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.environment.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.CloudStorageFolderResolverService;
import com.sequenceiq.cloudbreak.workspace.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.domain.FlowLog;

@Service
public class StackService implements ResourceIdProvider {

    public static final Set<String> REATTACH_COMPATIBLE_PLATFORMS = Set.of(CloudConstants.AWS, CloudConstants.AZURE, CloudConstants.GCP, CloudConstants.MOCK);

    private static final String STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE = "Stack not found by id '%d'";

    private static final String STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE = "Stack not found by name '%s'";

    private static final String STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE = "Stack not found by crn '%s'";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final String SSH_USER_CB = "cloudbreak";

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private NetworkConfigurationValidator networkConfigurationValidator;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;

    @Inject
    private StackResponseDecorator stackResponseDecorator;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private OrchestratorService orchestratorService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private StackViewService stackViewService;

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
    private FlowLogService flowLogService;

    @Inject
    private CloudStorageFolderResolverService cloudStorageFolderResolverService;

    @Inject
    private StackIdViewToStackResponseConverter stackIdViewToStackResponseConverter;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${info.app.version:}")
    private String cbVersion;

    public Optional<Stack> findStackByNameAndWorkspaceId(String name, Long workspaceId) {
        return findByNameAndWorkspaceIdWithLists(name, workspaceId);
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

    public StackV4Response getJsonByCrn(String crn, Collection<String> entry) {
        try {
            return transactionService.required(() -> {
                Stack stack = getByCrnWithLists(crn);
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

    public Stack getByCrn(String crn) {
        try {
            return transactionService.required(() -> stackRepository.findByResourceCrn(crn).orElseThrow(notFound("Stack", crn)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @PreAuthorize("hasRole('AUTOSCALE')")
    public Long getWorkspaceId(String crn) {
        return stackRepository.findWorkspaceIdByCrn(crn);
    }

    @PreAuthorize("hasRole('AUTOSCALE')")
    public Tenant getTenant(String crn) {
        Workspace workspace = stackRepository.findWorkspaceByCrn(crn).orElseThrow(notFound("workspace", crn));
        return workspace.getTenant();
    }

    @PreAuthorize("hasRole('AUTOSCALE')")
    public Set<AutoscaleStackV4Response> getAllForAutoscale() {
        try {
            return transactionService.required(() -> {
                Set<AutoscaleStack> aliveOnes = stackRepository.findAliveOnesWithAmbari();
                Set<AutoscaleStack> aliveNotUnderDeletion = Optional.ofNullable(aliveOnes).orElse(Set.of()).stream()
                        .filter(stack -> !DELETE_IN_PROGRESS.equals(stack.getStackStatus()))
                        .collect(Collectors.toSet());

                return converterUtil.convertAllAsSet(aliveNotUnderDeletion, AutoscaleStackV4Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<StackIdView> findClustersConnectedToDatalakeByDatalakeResourceId(Long datalakeResourceId) {
        return stackRepository.findEphemeralClusters(datalakeResourceId);
    }

    public Set<StackIdView> findClustersConnectedToDatalakeByDatalakeStackId(Long datalakeStackId) {
        Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findByDatalakeStackId(datalakeStackId);
        return datalakeResources.isEmpty() ? Collections.emptySet() : stackRepository.findEphemeralClusters(datalakeResources.get().getId());
    }

    public Stack getByIdWithListsInTransaction(Long id) {
        Stack stack;
        try {
            stack = transactionService.required(() -> stackRepository.findOneWithLists(id).orElse(null));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id));
        }
        return stack;
    }

    public Stack getByIdWithClusterInTransaction(Long id) {
        Stack stack;
        try {
            stack = transactionService.required(() -> stackRepository.findOneWithCluster(id).orElse(null));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        if (stack == null) {
            throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id));
        }
        return stack;
    }

    public Stack getById(Long id) {
        return stackRepository.findById(id).orElseThrow(notFound("Stack", id));
    }

    public Stack findByCrn(String crn) {
        return stackRepository.findByResourceCrn(crn).orElseThrow(notFound("Stack", crn));
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
            return transactionService.required(() -> stackViewService.findById(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackStatus getCurrentStatusByStackId(long stackId) {
        return stackStatusService.findFirstByStackIdOrderByCreatedDesc(stackId).orElseThrow(notFound("stackStatus", stackId));
    }

    public StackV4Response getByAmbariAddress(String ambariAddress) {
        try {
            return transactionService.required(() -> stackIdViewToStackResponseConverter.convert(stackRepository.findByAmbari(ambariAddress)
                    .orElseThrow(notFound("stack", ambariAddress))));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public List<StackStatusView> getByEnvironmentCrnAndStackType(String environmentCrn, StackType stackType) {
        return stackRepository.findByEnvironmentCrnAndStackType(environmentCrn, stackType);
    }

    public StackV4Response getByNameInWorkspaceWithEntries(String name, Long workspaceId, Set<String> entries, User user, StackType stackType) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = workspaceService.get(workspaceId, user);
                ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
                Optional<Stack> stack = findByNameAndWorkspaceIdWithLists(name, workspace.getId(), stackType, showTerminatedClustersAfterConfig);
                if (stack.isEmpty()) {
                    throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name));
                }
                StackV4Response stackResponse = converterUtil.convert(stack.get(), StackV4Response.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack.get(), entries);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV4Response getByCrnInWorkspaceWithEntries(String crn, Long workspaceId, Set<String> entries, User user, StackType stackType) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = workspaceService.get(workspaceId, user);
                ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
                Optional<Stack> stack = findByCrnAndWorkspaceIdWithLists(crn, workspace.getId(), stackType, showTerminatedClustersAfterConfig);
                if (stack.isEmpty()) {
                    throw new NotFoundException(String.format("Stack not found by crn '%s'", crn));
                }
                StackV4Response stackResponse = converterUtil.convert(stack.get(), StackV4Response.class);
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack.get(), entries);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV4Request getStackRequestByNameInWorkspaceId(String name, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Optional<Stack> stack = findByNameAndWorkspaceIdWithLists(name, workspaceId);
                if (stack.isEmpty()) {
                    throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name));
                }
                StackV4Request request = converterUtil.convert(stack.get(), StackV4Request.class);
                request.getCluster().setName(null);
                request.setName(name);
                return request;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackV4Request getStackRequestByCrnInWorkspaceId(String crn, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
                Optional<Stack> stack = findByCrnAndWorkspaceIdWithLists(crn, workspaceId, null, showTerminatedClustersAfterConfig);
                if (stack.isEmpty()) {
                    throw new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, crn));
                }
                StackV4Request request = converterUtil.convert(stack.get(), StackV4Request.class);
                request.getCluster().setName(null);
                request.setName(stack.get().getName());
                return request;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack getByNameInWorkspace(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceId(name, workspaceId)
                .orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name)));
    }

    public Stack getByCrnInWorkspace(String crn, Long workspaceId) {
        return stackRepository.findByCrnAndWorkspaceId(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public Optional<Stack> getByNameInWorkspaceWithLists(String name, Long workspaceId) {
        return findByNameAndWorkspaceIdWithLists(name, workspaceId);
    }

    @Measure(StackService.class)
    public Stack create(Stack stack, String platformString, StatedImage imgFromCatalog, User user, Workspace workspace) {
        if (stack.getGatewayPort() == null) {
            stack.setGatewayPort(nginxPort);
        }
        stack.setCreator(user);

        stack.setWorkspace(workspace);

        setPlatformVariant(stack);
        String stackName = stack.getName();

        MDCBuilder.buildMdcContext(stack);

        GetPlatformTemplateRequest templateRequest = connector.triggerGetTemplate(stack);

        if (!stack.getStackAuthentication().passwordAuthenticationRequired() && !Strings.isNullOrEmpty(stack.getStackAuthentication().getPublicKey())) {
            rsaPublicKeyValidator.validate(stack.getStackAuthentication().getPublicKey());
        }
        if (stack.getOrchestrator() != null) {
            orchestratorService.save(stack.getOrchestrator());
        }
        stack.getStackAuthentication().setLoginUserName(SSH_USER_CB);

        String accountId = threadBasedUserCrnProvider.getAccountId();

        stack.setResourceCrn(createCRN(accountId));

        Stack savedStack = measure(() -> stackRepository.save(stack),
                LOGGER, "Stackrepository save took {} ms for stack {}", stackName);

        MDCBuilder.buildMdcContext(savedStack);

        measure(() -> addCloudbreakDetailsForStack(savedStack),
                LOGGER, "Add Cloudbreak details took {} ms for stack {}", stackName);

        measure(() -> storeTelemetryForStack(savedStack),
                LOGGER, "Add Telemetry settings took {} ms for stack {}", stackName);

        measure(() -> instanceGroupService.saveAll(savedStack.getInstanceGroups()),
                LOGGER, "Instance groups saved in {} ms for stack {}", stackName);

        measure(() -> instanceMetaDataService.saveAll(savedStack.getInstanceMetaDataAsList()),
                LOGGER, "Instance metadatas saved in {} ms for stack {}", stackName);

        try {
            imageService.create(savedStack, platformString, imgFromCatalog);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Cloudbreak Image not found", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.info("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }

        measure(() -> addTemplateForStack(savedStack, connector.waitGetTemplate(stack, templateRequest)),
                LOGGER, "Save cluster template took {} ms for stack {}", stackName);

        return savedStack;
    }

    private void setPlatformVariant(Stack stack) {
        stack.setPlatformVariant(connector.checkAndGetPlatformVariant(stack).value());
    }

    public void deleteByName(Long id, Boolean forced, User user) {
        stackRepository.findById(id).map(stack -> {
            deleteByName(stack.getName(), stack.getWorkspace().getId(), forced, user);
            return stack;
        }).orElseThrow(notFound("Stack", id));
    }

    public void deleteByName(String name, Long workspaceId, boolean forced, User user) {
        stackRepository.findByNameAndWorkspaceId(name, workspaceId).map(stack -> {
            deleteByName(stack, forced, user);
            return stack;
        }).orElseThrow(notFound("Stack", name));
    }

    public void deleteByCrn(String crn, Long workspaceId, boolean forced, User user) {
        stackRepository.findByCrnAndWorkspaceId(crn, workspaceId).map(stack -> {
            deleteByName(stack, forced, user);
            return stack;
        }).orElseThrow(notFound("Stack", crn));
    }

    public void removeInstance(Stack stack, Long workspaceId, String instanceId, boolean forced, User user) {
        InstanceMetaData metaData = validateInstanceForDownscale(instanceId, stack, workspaceId, user);
        flowManager.triggerStackRemoveInstance(stack.getId(), metaData.getInstanceGroupName(), metaData.getPrivateId(), forced);
    }

    public void removeInstances(Stack stack, Long workspaceId, Collection<String> instanceIds, boolean forced, User user) {
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData metaData = validateInstanceForDownscale(instanceId, stack, workspaceId, user);
            instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
        }
        flowManager.triggerStackRemoveInstances(stack.getId(), instanceIdsByHostgroupMap, forced);
    }

    public void updateStatus(Long stackId, StatusRequest status, boolean updateCluster, User user) {
        Stack stack = getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterService.findOneWithLists(stack.getCluster().getId()).orElse(null);
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

    Optional<Stack> findTemplateWithLists(Long id) {
        return stackRepository.findTemplateWithLists(id);
    }

    public List<InstanceMetaData> getInstanceMetaDataForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateIds.contains(instanceMetaData.getPrivateId()))
                .filter(instanceMetaData -> !instanceMetaData.isTerminated())
                .collect(Collectors.toList());
    }

    public Optional<InstanceMetaData> getInstanceMetadata(List<InstanceMetaData> instanceMetaDataList, Long privateId) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateId.equals(instanceMetaData.getPrivateId()))
                .findFirst();
    }

    @Measure(StackService.class)
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

    public List<StackTtlView> getAllAlive() {
        return stackRepository.findAllAlive();
    }

    public Set<Stack> getAllAliveWithInstanceGroups() {
        return stackRepository.findAllAliveWithInstanceGroups();
    }

    public List<StackStatusView> getByStatuses(List<Status> statuses) {
        return stackRepository.findByStatuses(statuses);
    }

    public List<StackStatusView> getStatuses(Set<Long> stackIds) {
        return stackRepository.findStackStatusesWithoutAuth(stackIds);
    }

    public Set<StackIdView> getByNetwork(Network network) {
        return stackRepository.findByNetwork(network);
    }

    public void updateImage(Long stackId, Long workspaceId, String imageId, String imageCatalogName, String imageCatalogUrl, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        flowManager.triggerStackImageUpdate(stackId, imageId, imageCatalogName, imageCatalogUrl);
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return getByCrn(resourceCrn).getId();
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        Optional<Stack> stackByNameAndWorkspaceId = findStackByNameAndWorkspaceId(resourceName, workspaceId);
        if (stackByNameAndWorkspaceId.isPresent()) {
            return stackByNameAndWorkspaceId.get().getId();
        }
        throw new NotFoundException(String.format("Not found stack in the user's workspace with name %s", resourceName));
    }

    private Optional<Stack> findByNameAndWorkspaceIdWithLists(String name, Long workspaceId, StackType stackType, ShowTerminatedClustersAfterConfig config) {
        return stackType == null
                ? stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId, config.isActive(), config.showAfterMillisecs())
                : stackRepository.findByNameAndWorkspaceIdWithLists(name, stackType, workspaceId, config.isActive(), config.showAfterMillisecs());
    }

    private Optional<Stack> findByCrnAndWorkspaceIdWithLists(String crn, Long workspaceId, StackType stackType, ShowTerminatedClustersAfterConfig config) {
        return stackType == null
                ? stackRepository.findByCrnAndWorkspaceIdWithLists(crn, workspaceId, config.isActive(), config.showAfterMillisecs())
                : stackRepository.findByCrnAndWorkspaceIdWithLists(crn, stackType, workspaceId, config.isActive(), config.showAfterMillisecs());
    }

    private Optional<Stack> findByNameAndWorkspaceIdWithLists(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId, false, 0L);
    }

    private InstanceMetaData validateInstanceForDownscale(String instanceId, Stack stack, Long workspaceId, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        InstanceMetaData metaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId)
                .orElseThrow(() -> new NotFoundException(String.format("Metadata for instance %s has not found.", instanceId)));
        downscaleValidatorService.checkInstanceIsTheClusterManagerServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack.getCluster());
        return metaData;
    }

    private Stack getByIdWithLists(Long id) {
        return stackRepository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id)));
    }

    private Stack getByCrnWithLists(String crn) {
        return stackRepository.findOneByCrnWithLists(crn).orElseThrow(NotFoundException.notFound("Stack", crn));
    }

    private void repairFailedNodes(Stack stack, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        LOGGER.debug("Received request to replace failed nodes: " + stack.getId());
        flowManager.triggerManualRepairFlow(stack.getId());
    }

    private void sync(Stack stack, boolean full, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        // TODO: is it a good condition?
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
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
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
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
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

    public void updateNodeCount(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, boolean withClusterEvent, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        try {
            transactionService.required(() -> {
                Stack stackWithLists = getByIdWithLists(stack.getId());
                validateStackStatus(stackWithLists);
                validateInstanceGroup(stackWithLists, instanceGroupAdjustmentJson.getInstanceGroup());
                validateScalingAdjustment(instanceGroupAdjustmentJson, stackWithLists);
                validateInstanceStatuses(stackWithLists, instanceGroupAdjustmentJson);
                if (withClusterEvent) {
                    validateClusterStatus(stackWithLists);
                    validateHostGroupAdjustment(instanceGroupAdjustmentJson, stackWithLists, instanceGroupAdjustmentJson.getScalingAdjustment());
                    validataHostMetadataStatuses(stackWithLists, instanceGroupAdjustmentJson);
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

    public void updateMetaDataStatusIfFound(Long id, String hostName, InstanceStatus status, String statusReason) {
        instanceMetaDataService.findHostInStack(id, hostName).ifPresentOrElse(instanceMetaData -> {
            instanceMetaData.setStatusReason(statusReason);
            instanceMetaData.setInstanceStatus(status);
            instanceMetaDataService.save(instanceMetaData);
        }, () -> {
            LOGGER.warn("Metadata not found on stack:'{}' with hostname: '{}'.", id, hostName);
        });
    }

    public void updateMetaDataStatusIfFound(Long id, String hostName, InstanceStatus status) {
        updateMetaDataStatusIfFound(id, hostName, status, null);
    }

    public List<String> getHostNamesForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        return getInstanceMetaDataForPrivateIds(instanceMetaDataList, privateIds).stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toList());
    }

    public Optional<InstanceMetaData> getInstanceMetadataByPrivateIp(List<InstanceMetaData> instanceMetaDataList, String privateIp) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateIp.equals(instanceMetaData.getPrivateIp()))
                .findFirst();
    }

    public List<String> getInstanceIdsForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        List<InstanceMetaData> instanceMetaDataForPrivateIds = getInstanceMetaDataForPrivateIds(instanceMetaDataList, privateIds);
        return instanceMetaDataForPrivateIds.stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toList());
    }

    public Set<Long> getPrivateIdsForHostNames(List<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
        return getInstanceMetadatasForHostNames(instanceMetaDataList, hostNames).stream()
                .map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
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
            int removableHosts = instanceMetaDataService.findRemovableInstances(stack.getId(), instanceGroupAdjustmentJson.getInstanceGroup()).size();
            if (removableHosts < -1 * instanceGroupAdjustmentJson.getScalingAdjustment()) {
                throw new BadRequestException(
                        String.format("There are %s unregistered instances in instance group '%s' but %s were requested. Decommission nodes from the cluster!",
                                removableHosts, instanceGroup.getGroupName(), instanceGroupAdjustmentJson.getScalingAdjustment() * -1));
            }
        }
    }

    private void validateInstanceStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            List<InstanceMetaData> instanceMetaDataList =
                    stack.getInstanceMetaDataAsList().stream().filter(im -> !im.isTerminated() && !im.isRunning() && !im.isCreated())
                            .collect(Collectors.toList());
            if (!instanceMetaDataList.isEmpty()) {
                String ims = instanceMetaDataList.stream().map(im -> im.getInstanceId() + ": " + im.getInstanceStatus()).collect(Collectors.joining(","));
                throw new BadRequestException(
                        String.format("Upscale is not allowed because the following instances are not in running state: %s. Please remove them first!", ims));
            }
        }
    }

    private void validataHostMetadataStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            List<InstanceMetaData> instanceMetaDataAsList = stack.getInstanceMetaDataAsList();
            List<InstanceMetaData> unhealthyInstanceMetadataList = instanceMetaDataAsList.stream()
                    .filter(instanceMetaData -> InstanceStatus.SERVICES_UNHEALTHY.equals(instanceMetaData.getInstanceStatus()))
                    .collect(Collectors.toList());
            if (!unhealthyInstanceMetadataList.isEmpty()) {
                String notHealthyInstances = unhealthyInstanceMetadataList.stream()
                        .map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() + ": " + instanceMetaData.getInstanceStatus())
                        .collect(Collectors.joining(","));
                throw new BadRequestException(
                        String.format("Upscale is not allowed because the following hosts are not healthy: %s. Please remove them first!", notHealthyInstances));
            }
        }
    }

    private void validateHostGroupAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack, Integer adjustment) {
        Blueprint blueprint = stack.getCluster().getBlueprint();
        Optional<HostGroup> hostGroup = stack.getCluster().getHostGroups().stream()
                .filter(input -> input.getInstanceGroup().getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup())).findFirst();
        if (!hostGroup.isPresent()) {
            throw new BadRequestException(String.format("Instancegroup '%s' not found or not part of stack '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
        if (ClusterApi.AMBARI.equalsIgnoreCase(stack.getCluster().getVariant())) {
            ambariBlueprintValidator.validateHostGroupScalingRequest(blueprint, hostGroup.get(), adjustment);
        }
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

    private void deleteByName(Stack stack, boolean forced, User user) {
        LOGGER.info("Check permission for stack {} in environment {}.", stack.getName(), stack.getEnvironmentCrn());
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        LOGGER.info("Check stack that no cluster is attached to {} in environment.", stack.getEnvironmentCrn());
        checkStackHasNoAttachedClusters(stack);
        MDCBuilder.buildMdcContext(stack);
        if (stack.isDeleteInProgress() && forced) {
            LOGGER.info("stack {} in environment {} is already delete in progress.", stack.getName(), stack.getEnvironmentCrn());
            List<FlowLog> flowLogs = flowLogService.findAllByResourceIdOrderByCreatedDesc(stack.getId());
            Optional<FlowLog> flowLog = flowLogs.stream()
                    .filter(fl -> StackTerminationState.PRE_TERMINATION_STATE.name().equalsIgnoreCase(fl.getCurrentState()))
                    .findFirst();
            flowLog.ifPresent(fl -> {
                Map<Object, Object> variables = (Map<Object, Object>) JsonReader.jsonToJava(fl.getVariables());
                boolean runningFlowForced = variables.get("FORCEDTERMINATION") != null && Boolean.parseBoolean(variables.get("FORCEDTERMINATION").toString());
                if (!runningFlowForced) {
                    LOGGER.info("Terminate stack {} in environment {} because the current flow is not force termination.",
                            stack.getName(), stack.getEnvironmentCrn());
                    flowManager.triggerTermination(stack.getId(), true);
                }
            });
        } else if (!stack.isDeleteCompleted() && !stack.isDeleteInProgress()) {
            LOGGER.info("Terminate stack {} in environment {}.", stack.getName(), stack.getEnvironmentCrn());
            flowManager.triggerTermination(stack.getId(), forced);
        } else {
            LOGGER.debug("Stack is already deleted.");
        }
    }

    private void checkStackHasNoAttachedClusters(Stack stack) {
        Set<StackIdView> attachedOnes = findClustersConnectedToDatalakeByDatalakeStackId(stack.getId());
        if (!attachedOnes.isEmpty()) {
            throw new BadRequestException(String.format("Data Lake has attached Data Hub clusters! "
                            + "Please delete Data Hub cluster %s before deleting this Data Lake",
                    String.join(", ", attachedOnes.stream().map(StackIdView::getName).collect(Collectors.toSet()))));
        }
    }

    private void addTemplateForStack(Stack stack, String template) {
        StackTemplate stackTemplate = new StackTemplate(template, cbVersion);
        try {
            Component stackTemplateComponent = new Component(ComponentType.STACK_TEMPLATE, ComponentType.STACK_TEMPLATE.name(), new Json(stackTemplate), stack);
            componentConfigProviderService.store(stackTemplateComponent);
        } catch (IllegalArgumentException e) {
            LOGGER.info("Could not create Cloudbreak details component.", e);
        }
    }

    private void addCloudbreakDetailsForStack(Stack stack) {
        CloudbreakDetails cbDetails = new CloudbreakDetails(cbVersion);
        try {
            Component cbDetailsComponent = new Component(ComponentType.CLOUDBREAK_DETAILS,
                    ComponentType.CLOUDBREAK_DETAILS.name(), new Json(cbDetails), stack);
            componentConfigProviderService.store(cbDetailsComponent);
        } catch (IllegalArgumentException e) {
            LOGGER.info("Could not create Cloudbreak details component.", e);
        }
    }

    private void storeTelemetryForStack(Stack stack) {
        if (stack.getCluster() == null || StringUtils.isAnyEmpty(stack.getType().name(), stack.getResourceCrn(), stack.getCluster().getName())) {
            return;
        }
        List<Component> enrichedComponents = stack.getComponents().stream()
                .filter(component -> ComponentType.TELEMETRY.equals(component.getComponentType()))
                .map(component -> {
                    LOGGER.debug("Found TELEMETRY component for stack, will enrich that with cluster data before saving it.");
                    FluentClusterType fluentClusterType = StackType.DATALAKE.equals(stack.getType())
                            ? FluentClusterType.DATALAKE : FluentClusterType.DATAHUB;
                    try {
                        Telemetry telemetry = component.getAttributes().get(Telemetry.class);
                        cloudStorageFolderResolverService.updateStorageLocation(telemetry,
                                fluentClusterType.value(),
                                stack.getCluster().getName(), stack.getResourceCrn());
                        component.setAttributes(Json.silent(telemetry));
                    } catch (IOException e) {
                        LOGGER.info("Could not create Cloudbreak telemetry component.", e);
                    }
                    return component;
                })
                .collect(Collectors.toList());
        componentConfigProviderService.store(enrichedComponents);
    }

    private String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.DATAHUB)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.CLUSTER)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    public Optional<Duration> getTtlValueForStack(Long stackId) {
        return Optional.ofNullable(stackRepository.findTimeToLiveValueForSTack(stackId, PlatformParametersConsts.TTL_MILLIS))
                .map(Long::valueOf)
                .map(Duration::ofMillis);
    }

    public Boolean anyStackInWorkspace(Long workspaceId) {
        try {
            return transactionService.required(() -> stackRepository.anyStackInWorkspace(workspaceId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Boolean templateInUse(Long id) {
        return stackRepository.findTemplateInUse(id);
    }

    public Long getIdByNameInWorkspace(String name, Long workspaceId) {
        return stackRepository.findIdByNameAndWorkspaceId(name, workspaceId).orElseThrow(notFound("Stack", name));
    }

    public Set<StackIdView> findClustersConnectedToDatalake(Long stackId) {
        return stackRepository.findEphemeralClusters(stackId);
    }

    public Set<StackListItem> getByWorkspaceId(Long workspaceId, String environmentCrn, StackType stackType) {
        return stackRepository.findByWorkspaceId(workspaceId, environmentCrn, stackType);
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

    StackRepository repository() {
        return stackRepository;
    }

}
