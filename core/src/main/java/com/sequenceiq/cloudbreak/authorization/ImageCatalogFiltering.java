package com.sequenceiq.cloudbreak.authorization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ImageCatalogFiltering extends AbstractAuthorizationFiltering<Set<ImageCatalog>> {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private WorkspaceService workspaceService;

    public Set<ImageCatalog> filterImageCatalogs(AuthorizationResourceAction action) {
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, Map.of());
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        return imageCatalogService.findAsAuthorizationResorcesInWorkspace(workspaceService.getForCurrentUser().getId());
    }

    @Override
    public Set<ImageCatalog> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return imageCatalogService.findAllByIdsWithDefaults(authorizedResourceIds);
    }

    @Override
    public Set<ImageCatalog> getAll(Map<String, Object> args) {
        return imageCatalogService.findAllByWorkspaceId(workspaceService.getForCurrentUser().getId());
    }
}
