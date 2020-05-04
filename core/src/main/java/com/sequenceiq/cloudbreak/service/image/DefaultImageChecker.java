package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CrnsByCategory;
import com.sequenceiq.authorization.service.DefaultResourceChecker;

@Component
public class DefaultImageChecker implements DefaultResourceChecker {

    private final ImageCatalogService imageCatalogService;

    @Inject
    public DefaultImageChecker(ImageCatalogService imageCatalogService) {
        this.imageCatalogService = imageCatalogService;
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.IMAGE_CATALOG;
    }

    @Override
    public boolean isDefault(String resourceCrn) {
        return imageCatalogService.getDefaultImageCatalogs().stream().anyMatch(imageCatalog -> imageCatalog.getResourceCrn().equals(resourceCrn));
    }

    @Override
    public CrnsByCategory getDefaultResourceCrns(Collection<String> resourceCrns) {
        Map<Boolean, List<String>> byDefault = resourceCrns.stream().collect(Collectors.partitioningBy(this::isDefault));
        return CrnsByCategory.newBuilder()
                .defaultResourceCrns(byDefault.getOrDefault(true, List.of()))
                .notDefaultResourceCrns(byDefault.getOrDefault(false, List.of()))
                .build();
    }

    @Override
    public boolean isAllowedAction(AuthorizationResourceAction action) {
        return DESCRIBE_IMAGE_CATALOG.equals(action);
    }
}
