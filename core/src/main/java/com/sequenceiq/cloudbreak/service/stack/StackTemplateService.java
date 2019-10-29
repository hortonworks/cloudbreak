package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class StackTemplateService extends AbstractWorkspaceAwareResourceService<Stack> {

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    public Optional<Stack> getByIdWithLists(Long id) {
        return stackService.findTemplateWithLists(id);
    }

    @Override
    protected WorkspaceResourceRepository<Stack, Long> repository() {
        return stackService.repository();
    }

    @Override
    protected void prepareDeletion(Stack resource) {
        clusterService.pureDelete(resource.getCluster());
    }

    @Override
    protected void prepareCreation(Stack resource) {

    }

}
