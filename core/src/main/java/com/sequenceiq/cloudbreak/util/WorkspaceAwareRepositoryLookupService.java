package com.sequenceiq.cloudbreak.util;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.RepositoryLookupService;

@Service
public class WorkspaceAwareRepositoryLookupService extends RepositoryLookupService<WorkspaceResourceRepository<?, ?>> {

    @Inject
    private List<WorkspaceResourceRepository<?, ?>> repositoryList;

    @Override
    protected List<WorkspaceResourceRepository<?, ?>> getRepositoryList() {
        return repositoryList;
    }
}
