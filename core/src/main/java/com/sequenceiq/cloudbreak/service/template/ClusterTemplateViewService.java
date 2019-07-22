package com.sequenceiq.cloudbreak.service.template;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

@Service
public class ClusterTemplateViewService extends AbstractWorkspaceAwareResourceService<ClusterTemplateView> {

    @Inject
    private ClusterTemplateViewRepository clusterTemplateViewRepository;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplateView, Long> repository() {
        return clusterTemplateViewRepository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplateView resource) {
        throw new BadRequestException("Cluster template deletion is not supported from ClusterTemplateViewService");
    }

    @Override
    protected void prepareCreation(ClusterTemplateView resource) {
        throw new BadRequestException("Cluster template creation is not supported from ClusterTemplateViewService");
    }

    @Override
    public AuthorizationResource resource() {
        return AuthorizationResource.DATAHUB;
    }
}
