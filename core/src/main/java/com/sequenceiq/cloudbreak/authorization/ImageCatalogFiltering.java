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
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ImageCatalogFiltering extends AbstractAuthorizationFiltering<Set<ImageCatalog>> {

    // Temporary solution! As long as the UI supports the old JSON based custom catalogs, we'll need to support them as
    // well, meaning that we separate functionality: CLI added support for DB based custom catalogs (referred as "custom catalog")
    // while the UI has to retain support for JSON based ones (referred as "image catalog")
    private static final String CUSTOM_CATALOGS_ONLY = "customCatalogsOnly";

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private WorkspaceService workspaceService;

    public Set<ImageCatalog> filterImageCatalogs(AuthorizationResourceAction action, boolean customCatalogsOnly) {
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, Map.of(CUSTOM_CATALOGS_ONLY, customCatalogsOnly));
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        return imageCatalogService.findAsAuthorizationResorcesInWorkspace(workspaceService.getForCurrentUser().getId(),
                (Boolean) args.get(CUSTOM_CATALOGS_ONLY));
    }

    @Override
    public Set<ImageCatalog> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return imageCatalogService.findAllByIdsWithDefaults(authorizedResourceIds, (Boolean) args.get(CUSTOM_CATALOGS_ONLY));
    }

    @Override
    public Set<ImageCatalog> getAll(Map<String, Object> args) {
        return imageCatalogService.findAllByWorkspaceId(workspaceService.getForCurrentUser().getId(), (Boolean) args.get(CUSTOM_CATALOGS_ONLY));
    }
}
