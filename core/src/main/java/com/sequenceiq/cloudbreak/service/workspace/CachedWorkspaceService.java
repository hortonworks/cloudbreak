package com.sequenceiq.cloudbreak.service.workspace;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class CachedWorkspaceService {

    @Inject
    private WorkspaceService workspaceService;

    @Cacheable(cacheNames = "workspaceServiceCache", key = "#workspace")
    public Optional<Workspace> getByName(String workspace, User user) {
        return workspaceService.getByNameForTenant(workspace, user.getTenant());
    }
}
