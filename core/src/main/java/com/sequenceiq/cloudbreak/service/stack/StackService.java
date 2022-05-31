package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
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
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackCrnView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.projection.StackPlatformVariantView;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.environment.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.environment.tag.AccountTagClientService;
import com.sequenceiq.cloudbreak.service.environment.telemetry.AccountTelemetryClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.CloudStorageFolderResolverService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class StackService implements ResourceIdProvider, AuthorizationResourceNamesProvider, PayloadContextProvider {

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
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;

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
    private TransactionService transactionService;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackRepository stackRepository;

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
    private ResourceService resourceService;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private StackToStackV4ResponseConverter stackToStackV4ResponseConverter;

    @Inject
    private StackToStackV4RequestConverter stackToStackV4RequestConverter;

    @Inject
    private AutoscaleStackToAutoscaleStackResponseJsonConverter autoscaleStackToAutoscaleStackResponseJsonConverter;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${info.app.version:}")
    private String cbVersion;

    public Optional<Stack> findStackByNameAndWorkspaceId(String name, Long workspaceId) {
        return findByNameAndWorkspaceIdWithLists(name, workspaceId);
    }

    public Optional<Stack> findStackByNameOrCrnAndWorkspaceId(NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? findByNameAndWorkspaceIdWithLists(nameOrCrn.getName(), workspaceId)
                : findByCrnAndWorkspaceIdWithLists(nameOrCrn.getCrn(), workspaceId);
    }

    public StackV4Response getJsonById(Long id, Collection<String> entry) {
        try {
            return transactionService.required(() -> {
                Stack stack = getByIdWithLists(id);
                StackV4Response stackResponse = stackToStackV4ResponseConverter.convert(stack);
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
                StackV4Response stackResponse = stackToStackV4ResponseConverter.convert(stack);
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

    public StackClusterStatusView getStatusByCrn(String crn) {
        return stackRepository.getStatusByCrn(crn).orElseThrow(notFound("stack", crn));
    }

    public StackClusterStatusView getStatusByNameOrCrn(NameOrCrn nameOrCrn, Long workspaceId) {
        Optional<StackClusterStatusView> foundStack = nameOrCrn.hasName()
                ? stackRepository.getStatusByNameAndWorkspace(nameOrCrn.getName(), workspaceId)
                : stackRepository.getStatusByCrnAndWorkspace(nameOrCrn.getCrn(), workspaceId);
        return foundStack.orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn)));
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

    public StackV4Response getByNameInWorkspaceWithEntries(String name, Long workspaceId, Set<String> entries, User user, StackType stackType) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = workspaceService.get(workspaceId, user);
                ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
                Optional<Stack> stack = findByNameAndWorkspaceIdWithLists(name, workspace.getId(), stackType, showTerminatedClustersAfterConfig);
                if (stack.isEmpty()) {
                    throw new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name));
                }
                StackV4Response stackResponse = stackToStackV4ResponseConverter.convert(stack.get());
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
                    throw new NotFoundException(format("Stack not found by crn '%s'", crn));
                }
                StackV4Response stackResponse = stackToStackV4ResponseConverter.convert(stack.get());
                stackResponse = stackResponseDecorator.decorate(stackResponse, stack.get(), entries);
                return stackResponse;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
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
        Optional<Stack> foundStack = nameOrCrn.hasName()
                ? stackRepository.findByNameAndWorkspaceId(nameOrCrn.getName(), workspaceId)
                : stackRepository.findNotTerminatedByCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId);
        return foundStack.orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn)));
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
        return findByNameAndWorkspaceIdWithLists(name, workspaceId);
    }

    @Measure(StackService.class)
    public Stack create(Stack stack, String platformString, StatedImage imgFromCatalog, User user, Workspace workspace, Optional<String> externalCrn) {
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

        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        if (externalCrn.isPresent()) {
            // it means it is a DL cluster, double check it in sdx service
            sdxClientService.getByCrn(externalCrn.get());
            stack.setResourceCrn(externalCrn.get());
        } else {
            stack.setResourceCrn(createCRN(accountId));
        }
        setDefaultTags(stack);

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

        measure(() -> loadBalancerPersistenceService.saveAll(savedStack.getLoadBalancers()),
                LOGGER, "Load balancers saved in {} ms for stack {}", stackName);

        measure(() -> targetGroupPersistenceService.saveAll(savedStack.getTargetGroupAsList()),
                LOGGER, "Target groups saved in {} ms for stack {}", stackName);

        try {
            Set<Component> components = imageService.create(stack, imgFromCatalog);
            setRuntime(stack, components);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Cloudbreak Image not found", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.info("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }

        measure(() -> addTemplateForStack(savedStack, connector.waitGetTemplate(templateRequest)),
                LOGGER, "Save cluster template took {} ms for stack {}", stackName);

        return savedStack;
    }

    private void setRuntime(Stack stack, Set<Component> components) {
        ClouderaManagerProduct runtime = ComponentConfigProviderService.getComponent(components, ClouderaManagerProduct.class, CDH_PRODUCT_DETAILS);
        if (Objects.nonNull(runtime)) {
            String stackVersion = substringBefore(runtime.getVersion(), "-");
            LOGGER.debug("Setting runtime version {} for stack", stackVersion);
            stack.setStackVersion(stackVersion);
            stackRepository.save(stack);
        } else {
            // should not happen ever
            LOGGER.warn("Product component is not present amongst components, runtime could not be set!");
        }
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
            throw new BadRequestException(format("Invalid orchestrator type: %s", e.getMessage()));
        } catch (CloudbreakOrchestratorException e) {
            throw new BadRequestException(format("Error occurred when trying to reach orchestrator API: %s", e.getMessage()));
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
    public Long getResourceIdByResourceName(String resourceName) {
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return Optional.ofNullable(getIdByNameInWorkspace(resourceName, workspaceId)).orElseThrow(notFound("Stack", resourceName));
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        return findStackAsPayloadContext(resourceId).orElse(null);
    }

    private Optional<Stack> findByNameAndWorkspaceIdWithLists(String name, Long workspaceId, StackType stackType, ShowTerminatedClustersAfterConfig config) {
        Optional<Stack> stack = stackType == null
                ? stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId, config.isActive(), config.showAfterMillisecs())
                : stackRepository.findByNameAndWorkspaceIdWithLists(name, stackType, workspaceId, config.isActive(), config.showAfterMillisecs());

        return stack.map(st -> st.resources(new HashSet<>(resourceService.getAllByStackId(st.getId()))))
                .map(st -> st.instanceGroups(instanceGroupService.findNotTerminatedByStackId(st.getId())));
    }

    private Optional<Stack> findByCrnAndWorkspaceIdWithLists(String crn, Long workspaceId, StackType stackType, ShowTerminatedClustersAfterConfig config) {
        return stackType == null
                ? stackRepository.findByCrnAndWorkspaceIdWithLists(crn, workspaceId, config.isActive(), config.showAfterMillisecs())
                : stackRepository.findByCrnAndWorkspaceIdWithLists(crn, stackType, workspaceId, config.isActive(), config.showAfterMillisecs());
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

    private Optional<Stack> findByNameAndWorkspaceIdWithLists(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceIdWithLists(name, workspaceId, false, 0L);
    }

    private Optional<Stack> findByCrnAndWorkspaceIdWithLists(String name, Long workspaceId) {
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

    public Set<Long> getPrivateIdsForHostNames(Collection<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
        return getInstanceMetadatasForHostNames(instanceMetaDataList, hostNames).stream()
                .map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
    }

    private Set<InstanceMetaData> getInstanceMetadatasForHostNames(Collection<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
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
                        if (telemetry != null) {
                            telemetry.setRules(accountTelemetryClientService.getAnonymizationRules());
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

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.DATAHUB, accountId);
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

    public List<ResourceWithId> getAsAuthorizationResourcesByEnvCrn(Long workspaceId, String environmentCrn, StackType stackType) {
        return stackRepository.getAsAuthorizationResourcesByEnvCrn(workspaceId, environmentCrn, stackType);
    }

    public List<ResourceWithId> getAsAuthorizationResources(Long workspaceId, StackType stackType) {
        return stackRepository.getAsAuthorizationResources(workspaceId, stackType);
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

    public JobResource getJobResource(Long resourceId) {
        return stackRepository.getJobResource(resourceId).orElseThrow(notFound("Stack", resourceId));
    }

    public Optional<ResourceBasicView> getResourceBasicViewByResourceCrn(String resourceCrn) {
        return stackRepository.findResourceBasicViewByResourceCrn(resourceCrn);
    }

    public int setTunnelByStackId(Long stackId, Tunnel tunnel) {
        return stackRepository.setTunnelByStackId(stackId, tunnel);
    }
}
