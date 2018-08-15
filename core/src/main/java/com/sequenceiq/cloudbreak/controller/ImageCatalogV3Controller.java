package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ImageCatalogV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Controller
@Transactional(TxType.NEVER)
public class ImageCatalogV3Controller extends NotificationController implements ImageCatalogV3Endpoint {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<ImageCatalogResponse> listByOrganization(Long organizationId) {
        return imageCatalogService.listByOrganizationId(organizationId).stream()
                .map(imageCatalog -> conversionService.convert(imageCatalog, ImageCatalogResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public ImageCatalogResponse getByNameInOrganization(Long organizationId, String name) {
        ImageCatalog imageCatalog = imageCatalogService.getByNameForOrganization(name, organizationId);
        return conversionService.convert(imageCatalog, ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogResponse createInOrganization(Long organizationId, ImageCatalogRequest request) {
        ImageCatalog imageCatalog = conversionService.convert(request, ImageCatalog.class);
        imageCatalog = imageCatalogService.create(imageCatalog, organizationId);
        notify(authenticatedUserService.getCbUser(), ResourceEvent.IMAGE_CATALOG_CREATED);
        return conversionService.convert(imageCatalog, ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogResponse deleteInOrganization(Long organizationId, String name) {
        ImageCatalog deleted = imageCatalogService.deleteByNameFromOrganization(name, organizationId);
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        notify(identityUser, ResourceEvent.IMAGE_CATALOG_DELETED);
        return conversionService.convert(deleted, ImageCatalogResponse.class);
    }
}
