package com.sequenceiq.cloudbreak.service.template;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.projection.ClusterTemplateStatusView;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintListFilters;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.distrox.v1.distrox.service.HybridClusterTemplateValidator;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@Service
public class ClusterTemplateViewService extends AbstractWorkspaceAwareResourceService<ClusterTemplateView> {

    @Inject
    private ClusterTemplateViewRepository repository;

    @Inject
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @Inject
    private HybridClusterTemplateValidator hybridClusterTemplateValidator;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    @Inject
    private BlueprintListFilters blueprintListFilters;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplateView, Long> repository() {
        return repository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplateView resource) {
        throw new BadRequestException("Cluster template deletion is not supported from ClusterTemplateViewService");
    }

    @Override
    protected void prepareCreation(ClusterTemplateView resource) {
        throw new BadRequestException("Cluster template creation is not supported from ClusterTemplateViewService");
    }

    public Set<ClusterTemplateView> findAllActive(Long workspaceId, boolean internalTenant) {
        Set<ClusterTemplateView> allActive = repository.findAllActive(workspaceId);
        if (isGlobalDefaultTemplateEnabled()) {
            allActive = allActive.stream().filter(template -> !ResourceStatus.DEFAULT.equals(template.getStatus()))
                    .collect(Collectors.toSet());
            allActive = Stream.concat(getDefaultClusterTemplateViews().stream(), allActive.stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return allActive.stream()
                .filter(e -> internalClusterTemplateValidator.shouldPopulate(e, internalTenant))
                .collect(Collectors.toSet());
    }

    private List<ClusterTemplateView> getDefaultClusterTemplateViews() {
        boolean lakehouseOptimizerEnabled = entitlementService.isLakehouseOptimizerEnabled(ThreadBasedUserCrnProvider.getAccountId());
        return defaultClusterTemplateCache.getDefaultClusterTemplates()
                .stream()
                .filter(template -> lakehouseOptimizerEnabled || !blueprintListFilters.isLakehouseOptimizer(template.getName()))
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private ClusterTemplateView convert(ClusterTemplate template) {
        ClusterTemplateView view = new ClusterTemplateView();
        view.setName(template.getName());
        view.setResourceCrn(template.getResourceCrn());
        view.setDescription(template.getDescription());
        view.setCreated(template.getCreated());
        view.setClouderaRuntimeVersion(template.getClouderaRuntimeVersion());
        view.setType(template.getType());
        view.setCloudPlatform(template.getCloudPlatform());
        view.setStatus(template.getStatus());
        view.setFeatureState(template.getFeatureState());
        view.setDatalakeRequired(template.getDatalakeRequired());
        view.setTemplateContent(template.getTemplateContent());
        return view;
    }

    public Set<ClusterTemplateView> findAllByStackIds(List<Long> stackIds) {
        return repository.findAllByStackIds(stackIds);
    }

    public ClusterTemplateStatusView getStatusViewByResourceCrn(String resourceCrn) {
        if (isGlobalDefaultTemplateEnabled()) {
            Optional<ClusterTemplate> globalDefaultTemplate = defaultClusterTemplateCache.getDefaultClusterTemplateByResourceCrn(resourceCrn);
            if (globalDefaultTemplate.isPresent()) {
                ClusterTemplate clusterTemplate = globalDefaultTemplate.get();
                return clusterTemplate::getStatus;
            }
        }
        ClusterTemplateStatusView statusView = repository.findViewByResourceCrn(resourceCrn);
        if (statusView == null) {
            throw NotFoundException.notFoundException("Cluster definition", resourceCrn);
        }
        return statusView;
    }

    public Set<ClusterTemplateView> findAllUserManagedAndDefaultByEnvironmentCrn(Long workspaceId, String environmentCrn,
            String cloudPlatform, String runtime, boolean internalTenant, Boolean hybridEnvironment) {
        Set<ClusterTemplateView> allActive;
        if (isGlobalDefaultTemplateEnabled()) {
            allActive = repository.findAllUserManagedByEnvironmentCrn(workspaceId, environmentCrn, cloudPlatform, runtime);
            Set<ClusterTemplateView> defaultTemplates = getDefaultClusterTemplateViews().stream()
                    .filter(template ->
                            (StringUtils.isEmpty(cloudPlatform) || cloudPlatform.equalsIgnoreCase(template.getCloudPlatform())) &&
                                    (StringUtils.isEmpty(runtime) || runtime.equalsIgnoreCase(template.getClouderaRuntimeVersion())))
                    .collect(Collectors.toSet());
            allActive = Stream.concat(defaultTemplates.stream(), allActive.stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            allActive = repository.findAllUserManagedAndDefaultByEnvironmentCrn(workspaceId, environmentCrn, cloudPlatform, runtime);
        }
        return allActive.stream()
                .filter(e -> internalClusterTemplateValidator.shouldPopulate(e, internalTenant))
                .filter(e -> hybridClusterTemplateValidator.shouldPopulate(e, hybridEnvironment))
                .collect(Collectors.toSet());
    }

    private boolean isGlobalDefaultTemplateEnabled() {
        return entitlementService.isGlobalDefaultTemplateEnabled(ThreadBasedUserCrnProvider.getAccountId());
    }
}
