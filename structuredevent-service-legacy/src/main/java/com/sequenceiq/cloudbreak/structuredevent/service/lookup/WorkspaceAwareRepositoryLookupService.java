package com.sequenceiq.cloudbreak.structuredevent.service.lookup;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.util.RepositoryLookupService;

@Service
public class WorkspaceAwareRepositoryLookupService extends RepositoryLookupService<WorkspaceResourceRepository<?, ?>> {

    @Inject
    private List<WorkspaceResourceRepository<?, ?>> repositoryList;

    @Override
    protected List<WorkspaceResourceRepository<?, ?>> getRepositoryList() {
        return repositoryList;
    }
}
