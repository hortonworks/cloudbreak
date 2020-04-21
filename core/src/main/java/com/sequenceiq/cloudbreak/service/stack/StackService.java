package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.lang.String.format;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
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
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.environment.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.environment.tag.AccountTagClientService;
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
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class StackService implements ResourceIdProvider {

    public static final Set<String> REATTACH_COMPATIBLE_PLATFORMS = Set.of(CloudConstants.AWS, CloudConstants.AZURE, CloudConstants.GCP, CloudConstants.MOCK);

    private static final String STACK_NOT_FOUND_BY_ID_EXCEPTION_MESSAGE = "Stack not found by id '%d'";

    private static final String STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE = "Stack not found by name '%s'";

    private static final String STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE = "Stack not found by name or crn '%s'";

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
    private DatalakeResourcesService datalakeResourcesService;

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
    private ConverterUtil converterUtil;

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
    private ResourceService resourceService;

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

    public StackClusterStatusView getStatusByCrn(String crn) {
        return stackRepository.getStatusByCrn(crn).orElseThrow(notFound("stack", crn));
    }

    public StackClusterStatusView getStatusByNameOrCrn(NameOrCrn nameOrCrn, Long workspaceId) {
        Optional<StackClusterStatusView> foundStack = nameOrCrn.hasName()
                ? stackRepository.getStatusByNameAndWorkspace(nameOrCrn.getName(), workspaceId)
                : stackRepository.getStatusByCrnAndWorkspace(nameOrCrn.getCrn(), workspaceId);
        return foundStack.orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn)));
    }

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
                    throw new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name));
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
                    throw new NotFoundException(format("Stack not found by crn '%s'", crn));
                }
                StackV4Response stackResponse = converterUtil.convert(stack.get(), StackV4Response.class);
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
                StackV4Request request = converterUtil.convert(stack.get(), StackV4Request.class);
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
                : stackRepository.findByCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId);
        return foundStack.orElseThrow(() -> new NotFoundException(String.format(STACK_NOT_FOUND_BY_NAME_OR_CRN_EXCEPTION_MESSAGE, nameOrCrn)));
    }

    public Stack getByNameInWorkspace(String name, Long workspaceId) {
        return stackRepository.findByNameAndWorkspaceId(name, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name)));
    }

    public Stack getByCrnInWorkspace(String crn, Long workspaceId) {
        return stackRepository.findByCrnAndWorkspaceId(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public StackView getViewByNameInWorkspace(String name, Long workspaceId) {
        return stackViewService.findNotTerminatedByName(name, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_NAME_EXCEPTION_MESSAGE, name)));
    }

    public StackView getViewByCrnInWorkspace(String crn, Long workspaceId) {
        return stackViewService.findNotTerminatedByCrn(crn, workspaceId)
                .orElseThrow(() -> new NotFoundException(format(STACK_NOT_FOUND_BY_CRN_EXCEPTION_MESSAGE, crn)));
    }

    public Set<StackView> getViewsByCrnListInWorkspace(List<String> crns, Long workspaceId) {
        return stackViewService.findNotTerminatedByCrnList(crns, workspaceId);
    }

    public Set<StackView> getViewsByNameListInWorkspace(List<String> names, Long workspaceId) {
        return stackViewService.findNotTerminatedByNameList(names, workspaceId);
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

        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        stack.setResourceCrn(createCRN(accountId));
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

    private void setDefaultTags(Stack stack) {
        try {
            StackTags stackTag = stack.getTags().get(StackTags.class);
            Map<String, String> userDefinedTags = stackTag.getUserDefinedTags();

            boolean internalTenant = entitlementService.internalTenant(stack.getCreator().getUserCrn(), stack.getCreator().getTenant().getName());
            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                    .withCreatorCrn(stack.getCreator().getUserCrn())
                    .withEnvironmentCrn(stack.getEnvironmentCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withAccountId(stack.getCreator().getTenant().getName())
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
        stack.setPlatformVariant(connector.checkAndGetPlatformVariant(stack).value());
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

    private Stack getByCrnWithLists(String crn) {
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

    public Set<Long> getPrivateIdsForHostNames(List<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
        return getInstanceMetadatasForHostNames(instanceMetaDataList, hostNames).stream()
                .map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
    }

    private Set<InstanceMetaData> getInstanceMetadatasForHostNames(List<InstanceMetaData> instanceMetaDataList, Collection<String> hostNames) {
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

    public Set<StackIdView> findClustersConnectedToDatalake(Long stackId) {
        return stackRepository.findEphemeralClusters(stackId);
    }

    public Set<StackListItem> getByWorkspaceId(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        return stackRepository.findByWorkspaceId(workspaceId, environmentCrn, stackTypes);
    }

    public int setMinaSshdServiceIdByStackId(Long id, String minaSshdServiceId) {
        return stackRepository.setMinaSshdServiceIdByStackId(id, minaSshdServiceId);
    }

    StackRepository repository() {
        return stackRepository;
    }
}
