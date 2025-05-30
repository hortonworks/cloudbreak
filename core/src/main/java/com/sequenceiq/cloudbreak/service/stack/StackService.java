package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFoundException;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.authorization.service.AuthorizationResourceNamesProvider;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.converter.stack.AutoscaleStackToAutoscaleStackResponseJsonConverter;
import com.sequenceiq.cloudbreak.converter.stack.StackIdViewToStackResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.StackToStackV4RequestConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackCrnView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.projection.StackPlatformVariantView;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.monitoring.MonitoringEnablementService;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.environment.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.environment.tag.AccountTagClientService;
import com.sequenceiq.cloudbreak.service.environment.telemetry.AccountTelemetryClientService;
import com.sequenceiq.cloudbreak.service.externaldatabase.AzureDatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.CloudStorageFolderResolverService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class StackService implements ResourceIdProvider, AuthorizationResourceNamesProvider, PayloadContextProvider, MonitoringEnablementService<StackDto> {

    public static final Set<String> REATTACH_COMPATIBLE_PLATFORMS = Set.of(
            CloudConstants.AWS,
            CloudConstants.AZURE,
            CloudConstants.GCP,
            CloudConstants.MOCK,
            CloudConstants.AWS_NATIVE,
            CloudConstants.AWS_NATIVE_GOV);

    private static final String STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE = "Stack not found by id '%d'";

    private static final String STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE = "Stack not found by name '%s'";

    private static final String STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE = "Stack not found by name or crn '%s'";

    private static final String STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE = "Stack not found by crn '%s'";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final String SSH_USER_CB = "cloudbreak";

    @VisibleForTesting
    Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private NetworkConfigurationValidator networkConfigurationValidator;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private OpenSshPublicKeyValidator openSshPublicKeyValidator;

    @Inject
    private StackResponseDecorator stackResponseDecorator;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private OrchestratorService orchestratorService;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private TransactionService transactionService;

    @Inject
    @Qualifier("stackViewServiceDeprecated")
    private StackViewService stackViewService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private UserDataService userDataService;

    @Inject
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Inject
    private ClusterCommandService clusterCommandService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackPatchService stackPatchService;

    @Inject
    private ImageService imageService;

    @Inject
    private CostTagging costTagging;

    @Inject
    private CloudStorageFolderResolverService cloudStorageFolderResolverService;

    @Inject
    private StackIdViewToStackResponseConverter stackIdViewToStackResponseConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AccountTagClientService accountTagClientService;

    @Inject
    private AccountTelemetryClientService accountTelemetryClientService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private StackToStackV4ResponseConverter stackToStackV4ResponseConverter;

    @Inject
    private StackToStackV4RequestConverter stackToStackV4RequestConverter;

    @Inject
    private AutoscaleStackToAutoscaleStackResponseJsonConverter autoscaleStackToAutoscaleStackResponseJsonConverter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private AzureDatabaseServerParameterDecorator azureDatabaseServerParameterDecorator;

    @Inject
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private StackUtil stackUtil;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cluster.fetch.deleted.maxDaysLimit:2}")
    private Integer maxLimitForDeletedClusters;

    public Optional<Stack> findStackByNameAndWorkspaceId(String name, Long workspaceId) {
        return findByNameAndWorkspaceId(name, workspaceId);
    }

    public Optional<Stack> findStackByNameOrCrnAndWorkspaceId(NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? findByNameAndWorkspaceId(nameOrCrn.getName(), workspaceId)
                : findByCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId);
    }

    public StackV4Response getJsonByCrn(String crn) {
        StackDto stack = stackDtoService.getByCrn(crn);
        return stackToStackV4ResponseConverter.convert(stack);
    }

    public Stack get(Long id) {
        try {
            return transactionService.required(() -> stackRepository.findById(id).orElseThrow(notFound("Stack", id)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack getByCrnOrElseNull(String crn) {
        try {
            return transactionService.required(() -> stackRepository.findByResourceCrn(crn).orElse(null));
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

    public String getEnvCrnByCrn(String crn) {
        try {
            return transactionService.required(() -> stackRepository.findEnvCrnByResourceCrn(crn).orElseThrow(notFound("Stack", crn)));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public List<StackCrnView> findAllByCrn(Set<String> crns) {
        try {
            return transactionService.required(() -> stackRepository.findAllByResourceCrn(crns));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<StackIdView> findByDatalakeCrn(String crn) {
        try {
            return transactionService.required(() -> stackRepository.findByDatalakeCrn(crn));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<StackIdView> findNotTerminatedByDatalakeCrn(String crn) {
        try {
            return transactionService.required(() -> stackRepository.findNotTerminatedByDatalakeCrn(crn));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackClusterStatusView getStatusByCrn(String crn) {
        return stackRepository.getStatusByCrn(crn).orElseThrow(notFound("stack", crn));
    }

    public List<StackClusterStatusView> findStatusesByIds(List<Long> ids) {
        return stackRepository.findStatusesByIds(ids);
    }

    public StackClusterStatusView getStatusByNameOrCrn(NameOrCrn nameOrCrn, Long workspaceId) {
        Optional<StackClusterStatusView> foundStack = nameOrCrn.hasName()
                ? stackRepository.getStatusByNameAndWorkspace(nameOrCrn.getName(), workspaceId)
                : stackRepository.getStatusByCrnAndWorkspace(nameOrCrn.getCrn(), workspaceId);
        return foundStack.orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn)));
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> getEndpointsByCrn(String crn, String accountId) {
        StackDto stackDto = stackDtoService.getByNameOrCrn(NameOrCrn.ofCrn(crn), accountId);
        String managerAddress = stackUtil.extractClusterManagerAddress(stackDto);
        return serviceEndpointCollector.prepareClusterExposedServices(stackDto, managerAddress);
    }

    public List<StackClusterStatusView> getStatusesByCrnsInternal(List<String> crns, StackType stackType) {
        return stackRepository.getStatusByCrnsInternal(crns, stackType);
    }

    public Set<AutoscaleStackV4Response> getAllForAutoscale() {
        try {
            return transactionService.required(() -> {
                Set<AutoscaleStack> aliveOnes = stackRepository.findAliveOnesWithClusterManager();
                Set<AutoscaleStack> aliveNotUnderDeletion = Optional.ofNullable(aliveOnes).orElse(Set.of()).stream()
                        .filter(stack -> !DELETE_IN_PROGRESS.equals(stack.getStackStatus()))
                        .collect(Collectors.toSet());

                return aliveNotUnderDeletion.stream()
                        .map(a -> autoscaleStackToAutoscaleStackResponseJsonConverter.convert(a))
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<StackIdView> findClustersConnectedToDatalakeByDatalakeStackId(Long datalakeStackId) {
        Stack datalakeStack = get(datalakeStackId);
        Set<StackIdView> stacksConnectedByDatalakeCrn;
        if (datalakeStack != null) {
            stacksConnectedByDatalakeCrn = findByDatalakeCrn(datalakeStack.getResourceCrn());
        } else {
            stacksConnectedByDatalakeCrn = Collections.emptySet();
        }

        return stacksConnectedByDatalakeCrn;
    }

    public Stack getByIdWithListsInTransaction(Long id) {
        Stack stack;
        try {
            stack = transactionService.required(() -> stackRepository.findOneWithLists(id).orElse(null));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        if (stack == null) {
            throw new NotFoundException(format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id));
        }
        return stack;
    }

    public StackDto getStackProxyById(Long id) {
        return stackDtoService.getById(id);
    }

    public Stack getByIdWithGatewayInTransaction(Long id) {
        Stack stack;
        try {
            stack = transactionService.required(() -> stackRepository.findOneWithGateway(id).orElse(null));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        if (stack == null) {
            throw new NotFoundException(format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id));
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
            throw new NotFoundException(format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id));
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

    public Optional<PayloadContext> findStackAsPayloadContext(Long id) {
        return stackRepository.findStackAsPayloadContext(id);
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

    public List<StackIdView> getByEnvironmentCrnAndStackType(String environmentCrn, StackType stackType) {
        return stackRepository.findByEnvironmentCrnAndStackType(environmentCrn, stackType);
    }

    public StackV4Response getByNameInWorkspaceWithEntries(String name, String accountId, Set<String> entries, StackType stackType, boolean withResources) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
        Optional<StackDto> stack = findByNameAndWorkspaceId(name, accountId, stackType, showTerminatedClustersAfterConfig, withResources);
        if (stack.isEmpty()) {
            throw new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name));
        }
        StackV4Response stackResponse = stackToStackV4ResponseConverter.convert(stack.get());
        stackResponse = stackResponseDecorator.decorate(stackResponse, stack.get(), entries);
        return stackResponse;
    }

    public StackV4Response getByCrnInWorkspaceWithEntries(String crn, Set<String> entries, StackType stackType, boolean withResources) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
        Optional<StackDto> stack = findByCrnAndWorkspaceId(crn, stackType, showTerminatedClustersAfterConfig, withResources);
        if (stack.isEmpty()) {
            throw new NotFoundException(format("Stack not found by crn '%s'", crn));
        }
        StackV4Response stackResponse = stackToStackV4ResponseConverter.convert(stack.get());
        stackResponse = stackResponseDecorator.decorate(stackResponse, stack.get(), entries);
        return stackResponse;
    }

    public StackV4Request getStackRequestByNameOrCrnInWorkspaceId(NameOrCrn nameOrCrn, Long workspaceId) {
        try {
            return transactionService.required(() -> {
                ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
                Optional<Stack> stack = findByNameOrCrnAndWorkspaceIdWithLists(nameOrCrn, workspaceId);
                if (stack.isEmpty()) {
                    throw new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn));
                }
                StackV4Request request = stackToStackV4RequestConverter.convert(stack.get());
                request.getCluster().setName(null);
                request.setName(stack.get().getName());
                return request;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Stack getByNameOrCrnInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        if (nameOrCrn.hasName()) {
            return getByNameInWorkspace(nameOrCrn.getName(), workspaceId);
        } else {
            return getNotTerminatedByCrnInWorkspace(nameOrCrn.getCrn(), workspaceId);
        }
    }

    public Stack getByNameInWorkspace(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceId(name, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name)));
    }

    public Stack getNotTerminatedByCrnInWorkspace(String crn, Long workspaceId) {
        return stackRepository.findNotTerminatedByCrnAndWorkspaceId(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public Stack getByCrnInWorkspace(String crn, Long workspaceId) {
        return stackRepository.findByCrnAndWorkspaceId(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public StackView getViewByNameInWorkspace(String name, Long workspaceId) {
        return stackViewService.findNotTerminatedByName(name, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name)));
    }

    public StackView getNotTerminatedViewByCrnInWorkspace(String crn, Long workspaceId) {
        return stackViewService.findNotTerminatedByCrn(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public StackView getViewByCrnInWorkspace(String crn, Long workspaceId) {
        return stackViewService.findByCrn(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public String getResourceCrnInTenant(String name, String tenantName) {
        return stackViewService.findResourceCrnByNameAndTenantName(name, tenantName)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name)));
    }

    public Set<String> getResourceCrnsByNameListInTenant(List<String> names, String tenantName) {
        return stackViewService.findResourceCrnsByNameListAndTenant(names, tenantName);
    }

    public Optional<Stack> getByNameInWorkspaceWithLists(String name, Long workspaceId) {
        return findByNameAndWorkspaceId(name, workspaceId);
    }

    @Measure(StackService.class)
    public Stack create(Stack stack, StatedImage imgFromCatalog, User user, Workspace workspace) {
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
            // FIPS mode if and only if using AWS GovCloud
            boolean fipsEnabled = CloudConstants.AWS_NATIVE_GOV.equals(stack.getPlatformVariant());
            openSshPublicKeyValidator.validate(stack.getStackAuthentication().getPublicKey(), fipsEnabled);
        }
        if (stack.getOrchestrator() != null) {
            orchestratorService.save(stack.getOrchestrator());
        }
        stack.getStackAuthentication().setLoginUserName(SSH_USER_CB);

        Stack savedStack = measure(() -> save(stack), LOGGER, "Stackrepository save took {} ms for stack {}", stackName);

        setDefaultTags(savedStack);

        savedStack.populateStackIdForComponents();

        MDCBuilder.buildMdcContext(savedStack);

        measure(() -> addCloudbreakDetailsForStack(savedStack),
                LOGGER, "Add Cloudbreak details took {} ms for stack {}", stackName);

        measure(() -> storeTelemetryForStack(savedStack),
                LOGGER, "Add Telemetry settings took {} ms for stack {}", stackName);

        measure(() -> instanceGroupService.saveAll(savedStack.getInstanceGroups()),
                LOGGER, "Instance groups saved in {} ms for stack {}", stackName);

        measure(() -> instanceMetaDataService.saveAll(savedStack.getInstanceMetaDataAsList()),
                LOGGER, "Instance metadatas saved in {} ms for stack {}", stackName);

        measure(() -> loadBalancerPersistenceService.saveAll(savedStack.getLoadBalancers()),
                LOGGER, "Load balancers saved in {} ms for stack {}", stackName);

        measure(() -> targetGroupPersistenceService.saveAll(savedStack.getTargetGroupAsList()),
                LOGGER, "Target groups saved in {} ms for stack {}", stackName);

        try {
            Set<Component> components = imageService.create(stack, imgFromCatalog);
            setArchitecture(stack, imgFromCatalog);
            setRuntime(stack, components);
            setDbVersion(stack, imgFromCatalog);
            Stack savedStackWithAllDetails = stackRepository.save(stack);
            measure(() -> addTemplateForStack(savedStackWithAllDetails, connector.waitGetTemplate(templateRequest)),
                    LOGGER, "Save cluster template took {} ms for stack {}", stackName);
            return savedStackWithAllDetails;
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Cloudbreak Image not found", e);
            throw new BadRequestException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.info("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }

    private void setArchitecture(Stack stack, StatedImage imgFromCatalog) {
        Architecture architecture = Architecture.fromStringWithFallback(imgFromCatalog.getImage().getArchitecture());
        stack.setArchitecture(architecture);
    }

    private void setRuntime(Stack stack, Set<Component> components) {
        String stackVersion = calculateStackVersion(components);
        Optional.ofNullable(stackVersion).ifPresent(stack::setStackVersion);
    }

    private void setDbVersion(Stack stack, StatedImage imgFromCatalog) {
        boolean flexibleServerRequested = !isSingleServerRequested(stack);
        String dbEngineVersion = databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeAndOsIfMissing(
                stack.getStackVersion(), imgFromCatalog.getImage().getOs(), stack.getExternalDatabaseEngineVersion(),
                CloudPlatform.valueOf(stack.getCloudPlatform()),
                !Optional.ofNullable(stack.getDatabase().getExternalDatabaseAvailabilityType()).orElse(DatabaseAvailabilityType.NONE).isEmbedded(),
                flexibleServerRequested);
        if (stack.getDatabase() != null) {
            stack.getDatabase().setExternalDatabaseEngineVersion(dbEngineVersion);
        }
    }

    private boolean isSingleServerRequested(Stack stack) {
        return Optional.ofNullable(stack.getDatabase())
                .map(Database::getAttributesMap)
                .map(attributeMap -> azureDatabaseServerParameterDecorator.getDatabaseType(attributeMap).orElse(AzureDatabaseType.FLEXIBLE_SERVER))
                .map(AzureDatabaseType::isSingleServer)
                .orElse(Boolean.FALSE);
    }

    private String calculateStackVersion(Set<Component> components) {
        ClouderaManagerProduct runtime = ComponentConfigProviderService.getComponent(components, ClouderaManagerProduct.class, CDH_PRODUCT_DETAILS);
        if (Objects.nonNull(runtime)) {
            String stackVersion = substringBefore(runtime.getVersion(), "-");
            LOGGER.debug("Setting runtime version {} for stack", stackVersion);
            return stackVersion;
        } else {
            LOGGER.warn("Product component is not present amongst components, runtime could not be set! This is normal in case of base images");
            return null;
        }
    }

    public StackViewService getStackViewService() {
        return stackViewService;
    }

    private void setDefaultTags(Stack stack) {
        try {
            StackTags stackTag = stack.getTags().get(StackTags.class);
            Map<String, String> userDefinedTags = stackTag.getUserDefinedTags();

            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            boolean internalTenant = entitlementService.internalTenant(accountId);
            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                    .withCreatorCrn(stack.getCreator().getUserCrn())
                    .withEnvironmentCrn(stack.getEnvironmentCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withAccountId(accountId)
                    .withResourceCrn(stack.getResourceCrn())
                    .withResourceId(Optional.ofNullable(stack.getId()).map(String::valueOf).orElse(null))
                    .withIsInternalTenant(internalTenant)
                    .withUserName(stack.getCreator().getUserName())
                    .withAccountTags(accountTagClientService.list())
                    .withUserDefinedTags(userDefinedTags)
                    .build();

            Map<String, String> defaultTags = stackTag.getDefaultTags();
            defaultTags.putAll(costTagging.prepareDefaultTags(request));
            stack.setTags(new Json(new StackTags(userDefinedTags, stackTag.getApplicationTags(), defaultTags)));
        } catch (AccountTagValidationFailed aTVF) {
            throw new BadRequestException(aTVF.getMessage(), aTVF);
        } catch (Exception e) {
            LOGGER.debug("Exception during reading default tags.", e);
        }
    }

    private void setPlatformVariant(Stack stack) {
        if (StringUtils.isEmpty(stack.getPlatformVariant())) {
            String variant = connector.checkAndGetPlatformVariant(stack).value();
            stack.setPlatformVariant(variant);
        }
        stack.getInstanceGroups().stream()
                .flatMap(it -> it.getAllInstanceMetaData().stream())
                .filter(it -> StringUtils.isEmpty(it.getVariant()))
                .forEach(it -> it.setVariant(stack.getPlatformVariant()));
    }

    Optional<Stack> findTemplateWithLists(Long id) {
        return stackRepository.findTemplateWithLists(id);
    }

    public List<InstanceMetaData> getInstanceMetaDataForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> privateIds.contains(instanceMetaData.getPrivateId()))
                .collect(Collectors.toList());
    }

    public List<InstanceMetaData> getInstanceMetaDataForPrivateIdsWithoutTerminatedInstances(List<InstanceMetaData> instanceMetaDataList,
            Collection<Long> privateIds) {
        return getInstanceMetaDataForPrivateIds(instanceMetaDataList, privateIds).stream()
                .filter(instanceMetaData -> !instanceMetaData.isTerminated())
                .collect(Collectors.toList());
    }

    public Optional<InstanceMetadataView> getInstanceMetadata(List<InstanceMetadataView> instanceMetaDataList, Long privateId) {
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
            throw new BadRequestException(format("Invalid orchestrator type: %s", e.getMessage()));
        } catch (CloudbreakOrchestratorException e) {
            throw new BadRequestException(format("Error occurred when trying to reach orchestrator API: %s", e.getMessage()));
        }
    }

    public void checkLiveStackExistenceByName(String name, String accountId, StackType stackType) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.getHideTerminated();
        Optional<StackDto> stack = findByNameAndWorkspaceId(name, accountId, stackType, showTerminatedClustersAfterConfig, false);
        if (stack.isEmpty()) {
            throw new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name));
        }
    }

    public Stack save(Stack stack) {
        return stackRepository.save(stack);
    }

    public void updateMultiAzFlag(Long id, boolean multiAz) {
        try {
            transactionService.required(() -> stackRepository.setMultiAzFlag(id, multiAz));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public List<StackTtlView> getAllAlive() {
        return stackRepository.findAllAlive();
    }

    public Set<Stack> getAllAliveWithInstanceGroups() {
        return stackRepository.findAllAliveWithInstanceGroups();
    }

    public List<Image> getImagesOfAliveStacks(Integer thresholdInDays) {
        final LocalDateTime thresholdDate = nowSupplier.get()
                .minusDays(Optional.ofNullable(thresholdInDays).orElse(0));
        final long thresholdTimestamp = Timestamp.valueOf(thresholdDate).getTime();
        return stackRepository.findImagesOfAliveStacks(thresholdTimestamp).stream()
                .map(stackImageView -> {
                    try {
                        return stackImageView.getImage().get(Image.class);
                    } catch (IOException e) {
                        final String message =
                                String.format("Could not deserialize image for stack %d from %s", stackImageView.getId(), stackImageView.getImage());
                        LOGGER.error(message, e);
                        throw new IllegalStateException(message, e);
                    }
                })
                .collect(Collectors.toList());
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

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return Optional.ofNullable(getIdByCrnInWorkspace(resourceCrn, workspaceId)).orElseThrow(notFound("Stack", resourceCrn));
    }

    @Override
    public String getResourceCrnByResourceId(Long resourceId) {
        return getById(resourceId).getResourceCrn();
    }

    @Override
    public List<Long> getResourceIdsByResourceCrn(String resourceCrn) {
        return List.of(getResourceIdByResourceCrn(resourceCrn));
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return Optional.ofNullable(getIdByNameInWorkspace(resourceName, workspaceId)).orElseThrow(notFound("Stack", resourceName));
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        return findStackAsPayloadContext(resourceId).orElse(null);
    }

    private Optional<StackDto> findByNameAndWorkspaceId(String name, String accountId, StackType stackType, ShowTerminatedClustersAfterConfig config,
            boolean withResources) {
        StackDto stackDto = stackDtoService.getByNameOrCrn(NameOrCrn.ofName(name), accountId, stackType, config, withResources);
        return Optional.ofNullable(stackDto);
    }

    private Optional<StackDto> findByCrnAndWorkspaceId(String crn, StackType stackType, ShowTerminatedClustersAfterConfig config, boolean withResources) {
        StackDto stackDto = stackDtoService.getByNameOrCrn(NameOrCrn.ofCrn(crn), null, stackType, config, withResources);
        return Optional.ofNullable(stackDto);
    }

    public Stack getByNameOrCrnAndWorkspaceIdWithLists(NameOrCrn nameOrCrn, Long workspaceId) {
        return findByNameOrCrnAndWorkspaceIdWithLists(nameOrCrn, workspaceId)
                .orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn)));
    }

    private Optional<Stack> findByNameOrCrnAndWorkspaceIdWithLists(NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? stackRepository.findByNameAndWorkspaceIdWithLists(nameOrCrn.getName(), workspaceId, false, 0L)
                : stackRepository.findByCrnAndWorkspaceIdWithLists(nameOrCrn.getCrn(), workspaceId, false, 0L);
    }

    private Optional<Stack> findByNameAndWorkspaceId(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId, false, 0L);
    }

    private Optional<Stack> findByCrnAndWorkspaceId(String name, Long workspaceId) {
        return stackRepository.findByCrnAndWorkspaceIdWithLists(name, workspaceId, false, 0L);
    }

    public Stack getByIdWithLists(Long id) {
        return stackRepository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE, id)));
    }

    public Stack getByCrnWithLists(String crn) {
        return stackRepository.findOneByCrnWithLists(crn).orElseThrow(NotFoundException.notFound("Stack", crn));
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

    public List<String> getHostNamesForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        return getInstanceMetaDataForPrivateIdsWithoutTerminatedInstances(instanceMetaDataList, privateIds).stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toList());
    }

    public List<String> getInstanceIdsForPrivateIds(List<InstanceMetaData> instanceMetaDataList, Collection<Long> privateIds) {
        List<InstanceMetaData> instanceMetaDataForPrivateIds = getInstanceMetaDataForPrivateIds(instanceMetaDataList, privateIds);
        return instanceMetaDataForPrivateIds.stream()
                .map(instanceMetaData -> {
                    if (instanceMetaData.getInstanceId() != null) {
                        return instanceMetaData.getInstanceId();
                    } else {
                        return instanceMetaData.getPrivateId().toString();
                    }
                })
                .collect(Collectors.toList());
    }

    public Set<Long> getPrivateIdsForHostNames(Collection<? extends InstanceMetadataView> instanceMetaDataList, Collection<String> hostNames) {
        return getInstanceMetadatasForHostNames(instanceMetaDataList, hostNames).stream()
                .map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
    }

    public void updateTemplateForStackToLatest(Long stackId) {
        Stack stack = get(stackId);
        GetPlatformTemplateRequest templateRequest = connector.triggerGetTemplate(stack);
        String latestTemplate = connector.waitGetTemplate(templateRequest);
        componentConfigProviderService.updateStackTemplate(stack.getId(), latestTemplate);
    }

    private Set<? extends InstanceMetadataView> getInstanceMetadatasForHostNames(Collection<? extends InstanceMetadataView> instanceMetaDataList,
            Collection<String> hostNames) {
        return instanceMetaDataList.stream()
                .filter(instanceMetaData -> hostNames.contains(instanceMetaData.getDiscoveryFQDN()))
                .collect(Collectors.toSet());
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
            Component cbDetailsComponent = new Component(ComponentType.CLOUDBREAK_DETAILS, ComponentType.CLOUDBREAK_DETAILS.name(), new Json(cbDetails), stack);
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
                        if (telemetry != null) {
                            telemetry.setRules(accountTelemetryClientService.getAnonymizationRules());
                            dynamicEntitlementRefreshService.setupDynamicEntitlementsForProvision(stack.getResourceCrn(), telemetry);
                        }
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

    public Long getIdByNameOrCrnInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? stackRepository.findIdByNameAndWorkspaceId(nameOrCrn.getName(), workspaceId).orElseThrow(notFound("Stack", nameOrCrn.getName()))
                : stackRepository.findIdByCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId).orElseThrow(notFound("Stack", nameOrCrn.getCrn()));
    }

    public Long getIdByNameInWorkspace(String name, Long workspaceId) {
        return stackRepository.findIdByNameAndWorkspaceId(name, workspaceId).orElseThrow(notFound("Stack", name));
    }

    public Long getIdByCrnInWorkspace(String crn, Long workspaceId) {
        return stackRepository.findIdByCrnAndWorkspaceId(crn, workspaceId).orElseThrow(notFound("Stack", crn));
    }

    public Crn getCrnById(Long id) {
        return Crn.fromString(stackRepository.findCrnById(id).orElseThrow(notFound("Stack", id)));
    }

    public Set<StackListItem> getByWorkspaceId(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        return stackRepository.findByWorkspaceId(workspaceId, environmentCrn, stackTypes);
    }

    public Set<StackListItem> getByWorkspaceIdAndStackIds(Long workspaceId, List<Long> stackIds, List<StackType> stackTypes) {
        return stackRepository.findByWorkspaceIdAnStackIds(workspaceId, stackIds, stackTypes);
    }

    public List<ResourceWithId> getAsAuthorizationResourcesByEnvCrn(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        return stackRepository.getAsAuthorizationResourcesByEnvCrn(workspaceId, environmentCrn, stackTypes);
    }

    public List<ResourceWithId> getAsAuthorizationResources(Long workspaceId, List<StackType> stackTypes) {
        return stackRepository.getAsAuthorizationResources(workspaceId, stackTypes);
    }

    public List<ResourceWithId> getAsAuthorizationResourcesByCrns(Long workspaceId, StackType stackType, List<String> crns) {
        return stackRepository.getAsAuthorizationResourcesByCrns(workspaceId, stackType, crns);
    }

    public int setMinaSshdServiceIdByStackId(Long id, String minaSshdServiceId) {
        return stackRepository.setMinaSshdServiceIdByStackId(id, minaSshdServiceId);
    }

    public int setCcmV2AgentCrnByStackId(Long id, String ccmV2AgentCrn) {
        return stackRepository.setCcmV2AgentCrnByStackId(id, ccmV2AgentCrn);
    }

    StackRepository repository() {
        return stackRepository;
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        stackRepository.findResourceNamesByCrnAndWorkspaceId(crns, workspaceId)
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.DATALAKE, Crn.ResourceType.CLUSTER, Crn.ResourceType.STACK);
    }

    public CloudPlatformVariant getPlatformVariantByStackId(Long resourceId) {
        StackPlatformVariantView variantView = stackRepository.findPlatformVariantAndCloudPlatformById(resourceId);
        return new CloudPlatformVariant(variantView.getCloudPlatform(), variantView.getPlatformVariant());
    }

    public List<JobResource> getAllAliveForAutoSync(Set<Status> statusesNotIn) {
        return stackRepository.getJobResourcesNotIn(statusesNotIn);
    }

    public List<JobResource> getAllAliveDatahubs(Set<Status> statusesNotIn) {
        return stackRepository.getDatahubJobResourcesNotIn(statusesNotIn);
    }

    public JobResource getJobResource(Long resourceId) {
        return stackRepository.getJobResource(resourceId).orElseThrow(notFound("Stack", resourceId));
    }

    public Optional<ResourceBasicView> getResourceBasicViewByResourceCrn(String resourceCrn) {
        return stackRepository.findResourceBasicViewByResourceCrn(resourceCrn);
    }

    public int setTunnelByStackId(Long stackId, Tunnel tunnel) {
        return stackRepository.setTunnelByStackId(stackId, tunnel);
    }

    public void updateSecurityConfigByStackId(Long stackId, SecurityConfig securityConfig) {
        stackRepository.updateSecurityConfigByStackId(stackId, securityConfig);
    }

    public void updateCustomDomainByStackId(Long stackId, String customDomain) {
        stackRepository.updateCustomDomainByStackId(stackId, customDomain);
    }

    public void updateStackVersion(Long stackId, String stackVersion) {
        stackRepository.updateStackVersion(stackId, stackVersion);
    }

    public void updateJavaVersion(Long stackId, String javaVersion) {
        stackRepository.updateJavaVersion(stackId, javaVersion);
    }

    public void updateProviderSyncStates(Long stackId, Set<ProviderSyncState> statuses) {
        stackRepository.updateProviderSyncStates(stackId, statuses);
    }

    public void updateExternalDatabaseEngineVersion(Long stackId, String databaseVersion) {
        LOGGER.info("Updating DB engine version for [{}] to [{}]", stackId, databaseVersion);
        Optional<Long> databaseId = stackRepository.findDatabaseIdByStackId(stackId);
        databaseId.ifPresentOrElse(id -> updateExternalDatabaseEngineVersion(stackId, id, databaseVersion), () -> {
            LOGGER.warn("Stack with id [{}] is not found, update database engine version to [{}] is not possible", stackId, databaseVersion);
            throw notFoundException("Stack with", stackId + " id");
        });
        LOGGER.info("Updated database engine version for [{}] with [{}]", stackId, databaseVersion);
    }

    private void updateExternalDatabaseEngineVersion(Long stackId, Long databaseId, String databaseVersion) {
        Optional<PayloadContext> payloadContext = stackRepository.findStackAsPayloadContext(stackId);
        String cloudPlatform = payloadContext.map(PayloadContext::getCloudPlatform).orElse(null);
        databaseService.updateExternalDatabaseEngineVersion(databaseId, databaseVersion, CloudPlatform.fromName(cloudPlatform));
    }

    public void updateDomainDnsResolverByStackId(Long stackId, DnsResolverType actualDnsResolverType) {
        stackRepository.updateDomainDnsResolverByStackId(stackId, actualDnsResolverType);
    }

    public Stack getStackReferenceById(Long stackId) {
        return stackRepository.findById(stackId).orElse(null);
    }

    public int getNotUpgradedStackCount(String envCrn, Collection<Tunnel> upgradableTunnels) {
        return stackRepository.getNotUpgradedStackCount(envCrn, upgradableTunnels);
    }

    @Override
    public Optional<Boolean> computeMonitoringEnabled(StackDto entity) {
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(entity.getId());
            if (telemetry != null) {
                return Optional.of(telemetry.isComputeMonitoringEnabled());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String findEnvironmentCrnByStackId(Long id) {
        return stackRepository.findEnvironmentCrnByStackId(id).orElseThrow(notFound("Stack", id));
    }

    public String findRegionByStackId(Long id) {
        return stackRepository.findRegionByStackId(id).orElseThrow(notFound("Region by stack ID", id));
    }

    public List<StackClusterStatusView> getDeletedStacks(Long since) {
        validateMaxLimitForDeletedClusters(since);
        return stackRepository.getDeletedStacks(since);
    }

    public Set<String> getAllForArchive(long dateFrom, int limit) {
        Page<String> allArchived = stackRepository
                .findAllTerminatedBefore(dateFrom, PageRequest.of(0, limit));
        return allArchived.stream()
                .collect(Collectors.toSet());
    }

    public void deleteArchivedByResourceCrn(String crn) {
        Optional<Stack> stackOptional = stackRepository.findByResourceCrnArchivedIsTrue(crn);
        if (stackOptional.isPresent()) {
            // detaching all related entity on stack and orphan removal must delete all of them
            Stack stack = stackOptional.get();
            try {
                Long stackId = stack.getId();
                cleanupCluster(stack, stackId);
                cleanupInstanceGroups(stack);

                LOGGER.debug("Cleanup security config for stack {}", crn);
                securityConfigService.deleteByStackId(stackId);

                LOGGER.debug("Cleanup userData for stack {}", crn);
                userDataService.deleteByStackId(stackId);

                LOGGER.debug("Cleanup resources for stack {}", crn);
                resourceService.deleteByStackId(stackId);

                LOGGER.debug("Cleanup loadBalancer for stack {}", crn);
                loadBalancerPersistenceService.deleteByStackId(stackId);

                LOGGER.debug("Cleanup stackPatch for stack {}", crn);
                stackPatchService.deleteByStackId(stackId);

                LOGGER.debug("Cleanup components for stack {}", crn);
                componentConfigProviderService.deleteComponentsForStack(stackId);

                LOGGER.debug("Deleting stack with crn: {}", crn);
                stackRepository.deleteByResourceCrn(crn);
            } catch (Exception e) {
                String msg = String.format("Could not delete archived stack '%s' from database.", stack.getResourceCrn());
                LOGGER.error(msg, e);
                throw new CloudbreakServiceException(msg, e);
            }
        }
    }

    private void cleanupCluster(Stack stack, Long stackId) {
        LOGGER.debug("Cleanup cluster for stack {}", stack.getResourceCrn());
        if (stack.getCluster() != null) {
            Optional<Cluster> cluster = clusterService.findOneByStackId(stackId);
            if (cluster.isPresent()) {
                Long clusterId = cluster.get().getId();
                LOGGER.debug("Cleanup idBroker entity for cluster {}", clusterId);
                idBrokerService.deleteByClusterId(clusterId);
                LOGGER.debug("Cleanup clusterCommand entity for cluster {}", clusterId);
                clusterCommandService.deleteByClusterId(clusterId);
                clusterService.pureDelete(stack.getCluster());
            }
        }
    }

    private void cleanupInstanceGroups(Stack stack) {
        LOGGER.debug("Cleanup instancegroups for stack {}", stack.getResourceCrn());
        if (stack.getInstanceGroups() != null) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                instanceGroup.setTargetGroups(new HashSet<>());
                instanceGroupService.save(instanceGroup);

                LOGGER.debug("Cleanup targetgroups for instanceGroup {}", instanceGroup.getId());
                Set<TargetGroup> targetGroups = targetGroupPersistenceService.findByInstanceGroupId(instanceGroup.getId());
                for (TargetGroup targetGroup : targetGroups) {
                    targetGroupPersistenceService.delete(targetGroup.getId());
                }
                instanceGroupService.delete(instanceGroup.getId());
            }
        }
    }

    private void validateMaxLimitForDeletedClusters(Long since) {
        if (System.currentTimeMillis() - since > TimeUnit.DAYS.toMillis(maxLimitForDeletedClusters)) {
            throw new BadRequestException(String.format("Fetching deleted clusters is only allowed for last %s days",
                    maxLimitForDeletedClusters));
        }
    }
}