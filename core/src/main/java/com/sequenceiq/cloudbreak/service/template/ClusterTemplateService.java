package com.sequenceiq.cloudbreak.service.template;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.clustertemplate.ClusterTemplateLoaderService;
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class ClusterTemplateService extends AbstractWorkspaceAwareResourceService<ClusterTemplate> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateService.class);

    @Inject
    private ClusterTemplateRepository repository;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateLoaderService clusterTemplateLoaderService;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplate, Long> repository() {
        return repository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplate resource) {
        if (resource.getStatus() != ResourceStatus.USER_MANAGED) {
            throw new AccessDeniedException("Default template deletion is forbidden");
        }
    }

    @Override
    protected void prepareCreation(ClusterTemplate resource) {

    }

    @Override
    public Set<ClusterTemplate> findAllByWorkspace(Workspace workspace) {
        return getAllAvailableInWorkspace(workspace);
    }

    @Override
    public Set<ClusterTemplate> findAllByWorkspaceId(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getAllAvailableInWorkspace(workspace);
    }

    public Set<ClusterTemplate> getAllAvailableInWorkspace(Workspace workspace) {
        Set<ClusterTemplate> clusterTemplates = repository.findAllByNotDeletedInWorkspace(workspace.getId());
        if (clusterTemplateLoaderService.isDefaultClusterTemplateUpdateNecessaryForUser(clusterTemplates)) {
            LOGGER.info("Modifying clusterTemplates based on the defaults for the '{}' workspace.", workspace.getId());
            clusterTemplates = clusterTemplateLoaderService.loadClusterTemplatesForWorkspace(clusterTemplates, workspace, this::saveDefaultsWithReadRight);
            LOGGER.info("ClusterTemplate modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return clusterTemplates;
    }

    private Iterable<ClusterTemplate> saveDefaultsWithReadRight(Iterable<ClusterTemplate> clusterTemplates) {
        return repository.saveAll(clusterTemplates);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.CLUSTER_TEMPLATE;
    }
}
