package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.ClusterTemplateStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.init.clustertemplate.ClusterTemplateLoaderService;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;

@Service
public class ClusterTemplateService extends AbstractWorkspaceAwareResourceService<ClusterTemplate> implements ResourceBasedCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateService.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private ClusterTemplateRepository clusterTemplateRepository;

    @Inject
    private ClusterTemplateViewService clusterTemplateViewService;

    @Inject
    private UserService userService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private ClusterTemplateLoaderService clusterTemplateLoaderService;

    @Inject
    private OrchestratorService orchestratorService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private NetworkService networkService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackTemplateService stackTemplateService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private SupportedRuntimes supportedRuntimes;

    @Inject
    private ClusterTemplateCloudPlatformValidator cloudPlatformValidator;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplate, Long> repository() {
        return clusterTemplateRepository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplate resource) {
        if (resource.getStatus() == ResourceStatus.DEFAULT || resource.getStatus() == ResourceStatus.DEFAULT_DELETED) {
            throw new AccessDeniedException("Default template deletion is forbidden");
        }
    }

    public ClusterTemplate createForLoggedInUser(ClusterTemplate resource, Long workspaceId, String accountId, String creator) {
        resource.setResourceCrn(createCRN(accountId));
        try {
            return transactionService.required(() -> {
                ClusterTemplate created = super.createForLoggedInUser(resource, workspaceId);
                grpcUmsClient.assignResourceOwnerRoleIfEntitled(creator, resource.getResourceCrn(), accountId);
                return created;
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof BadRequestException) {
                throw e.getCause();
            }
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public Set<ClusterTemplate> getTemplatesByBlueprint(Blueprint blueprint) {
        return clusterTemplateRepository.getTemplatesByBlueprintId(blueprint.getId(), blueprint.getWorkspace().getId());
    }

    @Override
    protected void prepareCreation(ClusterTemplate resource) {

        measure(() -> validateBeforeCreate(resource), LOGGER, "Cluster template validated in {}ms");

        Stack stackTemplate = resource.getStackTemplate();
        stackTemplate.setName(UUID.randomUUID().toString());
        if (stackTemplate.getOrchestrator() != null) {
            orchestratorService.save(stackTemplate.getOrchestrator());
        }

        Network network = stackTemplate.getNetwork();
        if (network != null) {
            network.setWorkspace(stackTemplate.getWorkspace());
            networkService.pureSave(network);
        }

        Cluster cluster = stackTemplate.getCluster();
        if (cluster != null) {
            cluster.setWorkspace(stackTemplate.getWorkspace());
            clusterService.saveWithRef(cluster);
        }

        stackTemplate.setResourceCrn(createCRN(ThreadBasedUserCrnProvider.getAccountId()));

        stackTemplate = stackTemplateService.pureSave(stackTemplate);

        componentConfigProviderService.store(new ArrayList<>(stackTemplate.getComponents()));

        if (cluster != null) {
            cluster.setStack(stackTemplate);
            clusterService.save(cluster);
        }

        if (stackTemplate.getInstanceGroups() != null && !stackTemplate.getInstanceGroups().isEmpty()) {
            instanceGroupService.saveAll(stackTemplate.getInstanceGroups(), stackTemplate.getWorkspace());
        }
        resource.setCreated(System.currentTimeMillis());
    }

    private void validateBeforeCreate(ClusterTemplate resource) {

        if (resource.getStackTemplate() == null) {
            throw new BadRequestException("The stack tempalte cannot be null.");
        }

        if (resource.getStatus() != ResourceStatus.DEFAULT && resource.getStackTemplate().getEnvironmentCrn() == null) {
            throw new BadRequestException("The environment cannot be null.");
        }

        if (clusterTemplateRepository.findByNameAndWorkspace(resource.getName(), resource.getWorkspace()).isPresent()) {
            throw new DuplicateClusterTemplateException(
                    format("clustertemplate already exists with name '%s' in workspace %s", resource.getName(), resource.getWorkspace().getName()));
        }

        Optional<String> messageIfBlueprintIsInvalidInCluster = getMessageIfBlueprintIsInvalidInCluster(resource);
        if (messageIfBlueprintIsInvalidInCluster.isPresent()) {
            throw new BadRequestException(messageIfBlueprintIsInvalidInCluster.get());
        }
    }

    @Override
    public Set<ClusterTemplate> findAllByWorkspace(Workspace workspace) {
        updateDefaultClusterTemplates(workspace);
        return clusterTemplateRepository.findAllByNotDeletedInWorkspace(workspace.getId());
    }

    public Set<ClusterTemplateView> findAllByEnvironment(String environmenCrn) {
        LOGGER.debug("About to collect cluster definitions by environment: [crn: {}]", environmenCrn);
        return clusterTemplateViewService.findAllByEnvironmentCrn(environmenCrn);
    }

    @Override
    public Set<ClusterTemplate> findAllByWorkspaceId(Long workspaceId) {
        updateDefaultClusterTemplates(workspaceId);
        return clusterTemplateRepository.findAllByNotDeletedInWorkspace(workspaceId);
    }

    private boolean isNotUsableClusterTemplate(ClusterTemplateViewV4Response response) {
        return !isUsableClusterTemplate(response);
    }

    @VisibleForTesting
    boolean isUsableClusterTemplate(ClusterTemplateViewV4Response response) {
        return (isUserManaged(response) && hasEnvironment(response)) || isDefaultTemplate(response);
    }

    private boolean isUserManaged(ClusterTemplateViewV4Response response) {
        return ResourceStatus.USER_MANAGED == response.getStatus();
    }

    private boolean hasEnvironment(ClusterTemplateViewV4Response response) {
        return nonNull(response.getEnvironmentName());
    }

    private boolean isDefaultTemplate(ClusterTemplateViewV4Response response) {
        return ResourceStatus.DEFAULT == response.getStatus() && supportedRuntimes.isSupported(response.getStackVersion());
    }

    @VisibleForTesting
    boolean isClusterTemplateHasValidCloudPlatform(ClusterTemplateViewV4Response response) {
        return cloudPlatformValidator.isClusterTemplateCloudPlatformValid(response.getCloudPlatform(), ThreadBasedUserCrnProvider.getAccountId());
    }

    public void updateDefaultClusterTemplates(long workspaceId) {
        Workspace workspace = getWorkspaceService().getByIdForCurrentUser(workspaceId);
        updateDefaultClusterTemplates(workspace);
    }

    private Optional<String> getMessageIfBlueprintIsInvalidInCluster(ClusterTemplate clusterTemplate) {
        if (Objects.isNull(clusterTemplate.getStackTemplate().getCluster())) {
            String msg = "Stack template in cluster definition should contain a – valid – cluster request!";
            return Optional.of(msg);
        }
        String msg = null;
        boolean hasBlueprint = nonNull(clusterTemplate.getStackTemplate().getCluster().getBlueprint());
        if (!hasBlueprint) {
            msg = "Cluster definition should contain a cluster template!";
        } else {
            boolean hasExistingBlueprint = blueprintService.getAllAvailableInWorkspace(clusterTemplate.getWorkspace())
                    .stream()
                    .anyMatch(blueprint -> blueprint.getName().equals(clusterTemplate.getStackTemplate().getCluster().getBlueprint().getName()));
            if (!hasExistingBlueprint) {
                msg = "The cluster template (aka blueprint) in the cluster definition should be an existing one!";
            }
        }
        return Optional.ofNullable(msg);
    }

    public Set<ClusterTemplateViewV4Response> listInWorkspaceAndCleanUpInvalids(Long workspaceId) {
        try {
            Set<ClusterTemplateView> views = transactionService.required(() -> clusterTemplateViewService.findAllActive(workspaceId));
            Set<ClusterTemplateViewV4Response> responses = transactionService.required(() ->
                    converterUtil.convertAllAsSet(views, ClusterTemplateViewV4Response.class));
            environmentServiceDecorator.prepareEnvironments(responses);

            cleanUpInvalidClusterDefinitions(workspaceId, responses);

            return responses.stream()
                    .filter(this::isUsableClusterTemplate)
                    .filter(this::isClusterTemplateHasValidCloudPlatform)
                    .collect(toSet());
        } catch (TransactionExecutionException e) {
            LOGGER.warn("Unable to find cluster definitions due to {}", e.getMessage());
            LOGGER.warn("Unable to find cluster definitions", e);
            throw new CloudbreakServiceException("Unable to obtain cluster definitions!");
        }
    }

    private void updateDefaultClusterTemplates(Workspace workspace) {
        Set<ClusterTemplate> clusterTemplates = clusterTemplateRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        if (clusterTemplateLoaderService.isDefaultClusterTemplateUpdateNecessaryForUser(clusterTemplates)) {
            LOGGER.debug("Modifying clusterTemplates based on the defaults for the '{} ({})' workspace.", workspace.getName(), workspace.getId());
            Collection<ClusterTemplate> outdatedTemplates = clusterTemplateLoaderService.collectOutdatedTemplatesInDb(clusterTemplates);
            LOGGER.debug("Outdated clusterTemplates collected: '{}'.", outdatedTemplates.size());
            delete(new HashSet<>(outdatedTemplates));
            LOGGER.debug("Outdated clusterTemplates deleted: '{}'.", outdatedTemplates.size());
            clusterTemplates = clusterTemplateRepository.findAllByNotDeletedInWorkspace(workspace.getId());
            LOGGER.debug("None deleted clusterTemplates collected: '{}'.", clusterTemplates.size());
            clusterTemplateLoaderService.loadClusterTemplatesForWorkspace(clusterTemplates, this::createAll);
            LOGGER.debug("ClusterTemplate modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
    }

    private Collection<ClusterTemplate> createAll(Iterable<ClusterTemplate> clusterTemplates) {
        return StreamSupport.stream(clusterTemplates.spliterator(), false)
                .map(ct -> {
                    try {
                        return measure(() -> create(ct, ct.getWorkspace(), userService.getOrCreate(legacyRestRequestThreadLocalService.getCloudbreakUser())),
                                LOGGER, "Cluster template created in {}ms");
                    } catch (DuplicateClusterTemplateException duplicateClusterTemplateException) {
                        LOGGER.info("Template was found, try to get it", duplicateClusterTemplateException);
                        return getByNameForWorkspace(ct.getName(), ct.getWorkspace());
                    } catch (BadRequestException badRequestException) {
                        LOGGER.info("Template save failed, but try to get it", badRequestException);
                        try {
                            return getByNameForWorkspace(ct.getName(), ct.getWorkspace());
                        } catch (NotFoundException notFoundException) {
                            LOGGER.info("Template was not found", notFoundException);
                            throw notFoundException;
                        }
                    }
                })
                .collect(Collectors.toList());
    }

    public ClusterTemplate deleteByName(String name, Long workspaceId) {
        ClusterTemplate clusterTemplate = getByNameForWorkspaceId(name, workspaceId);
        clusterTemplate = delete(clusterTemplate);
        grpcUmsClient.notifyResourceDeleted(clusterTemplate.getResourceCrn(), MDCUtils.getRequestId());
        stackTemplateService.delete(clusterTemplate.getStackTemplate());
        return clusterTemplate;
    }

    public ClusterTemplate getByCrn(String crn, Long workspaceId) {
        Optional<ClusterTemplate> clusterTemplateOptional = clusterTemplateRepository.getByCrnForWorkspaceId(crn, workspaceId);
        if (clusterTemplateOptional.isEmpty()) {
            throw new BadRequestException(
                    format("cluster template does not exist with crn '%s' in workspace %s", crn, workspaceId));
        }
        return clusterTemplateOptional.get();
    }

    public ClusterTemplate deleteByCrn(String crn, Long workspaceId) {
        ClusterTemplate clusterTemplate = getByCrn(crn, workspaceId);
        clusterTemplate = delete(clusterTemplate);
        stackTemplateService.delete(clusterTemplate.getStackTemplate());
        return clusterTemplate;
    }

    private void cleanUpInvalidClusterDefinitions(final long workspaceId, Set<ClusterTemplateViewV4Response> envPreparedTemplates) {
        try {
            LOGGER.debug("Collecting cluster definition(s) which has no associated existing environment...");
            Set<String> invalidTemplateNames = envPreparedTemplates.stream()
                    .filter(this::isNotUsableClusterTemplate)
                    .map(CompactViewV4Response::getName)
                    .collect(toSet());

            if (!invalidTemplateNames.isEmpty()) {
                LOGGER.debug("About to delete invalid cluster definition(s): [{}]", String.join(", ", invalidTemplateNames));
                transactionService.required(() -> deleteMultiple(invalidTemplateNames, workspaceId));
            }
        } catch (TransactionExecutionException e) {
            LOGGER.warn("Unable to delete invalid cluster definition(s) due to: {}", e.getMessage());
        }
    }

    public Set<ClusterTemplate> deleteMultiple(Set<String> names, Long workspaceId) {
        return names.stream().map(name -> deleteByName(name, workspaceId)).collect(toSet());
    }

    public String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.DATAHUB)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.CLUSTER_DEFINITION)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return clusterTemplateRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public List<String> getResourceCrnsInAccount() {
        return clusterTemplateRepository.findAllResourceCrnsByAccountId(ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return resourceNames.stream()
                .map(resourceName -> clusterTemplateRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId()))
                .collect(Collectors.toList());
    }

    public ClusterTemplate getByResourceCrn(String resourceCrn) {
        return clusterTemplateRepository.findByResourceCrn(resourceCrn);
    }

    public ClusterTemplateStatusView getStatusViewByResourceCrn(String resourceCrn) {
        return clusterTemplateRepository.findViewByResourceCrn(resourceCrn);
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.CLUSTER_DEFINITION;
    }

}
