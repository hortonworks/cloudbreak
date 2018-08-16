package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StackImageFilterService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Component
@Transactional(TxType.NEVER)
public class ImageCatalogV1Controller implements ImageCatalogV1Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackImageFilterService stackImageFilterService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private OrganizationService organizationService;

    @Override
    public List<ImageCatalogResponse> getPublics() {
        return getAll();
    }

    @Override
    public ImageCatalogResponse getByName(String name, boolean withImages) {
        ImageCatalogResponse imageCatalogResponse = convert(imageCatalogService.get(getDefOrgId(), name));
        Images images = imageCatalogService.propagateImagesIfRequested(getDefOrgId(), name, withImages);
        if (images != null) {
            imageCatalogResponse.setImagesResponse(conversionService.convert(images, ImagesResponse.class));
        }
        return imageCatalogResponse;
    }

    @Override
    public ImagesResponse getImagesByProvider(String platform) throws Exception {
        Images images = imageCatalogService.getImagesOsFiltered(platform, null).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImageCatalogResponse postPublic(ImageCatalogRequest imageCatalogRequest) {
        return post(imageCatalogRequest);
    }

    @Override
    public ImageCatalogResponse postPrivate(ImageCatalogRequest imageCatalogRequest) {
        return post(imageCatalogRequest);
    }

    @Override
    public ImagesResponse getImagesByProviderFromImageCatalog(String name, String platform) throws Exception {
        Images images = imageCatalogService.getImages(getDefOrgId(), name, platform).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public void deletePublic(String name) {
        imageCatalogService.delete(getDefOrgId(), name);
    }

    @Override
    public ImageCatalogResponse putPublic(UpdateImageCatalogRequest request) {
        ImageCatalog imageCatalog = imageCatalogService.update(getDefOrgId(), conversionService.convert(request, ImageCatalog.class));
        return convert(imageCatalog);
    }

    @Override
    public ImageCatalogResponse putSetDefaultByName(String name) {
        return conversionService.convert(imageCatalogService.setAsDefault(getDefOrgId(), name), ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogRequest getRequestfromName(String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(getDefOrgId(), name);
        return conversionService.convert(imageCatalog, ImageCatalogRequest.class);
    }

    @Override
    public ImagesResponse getImagesFromCustomImageCatalogByStack(String imageCatalogName, String stackName) throws CloudbreakImageCatalogException {
        Images images = stackImageFilterService.getApplicableImages(getDefOrgId(), imageCatalogName, stackName);
        return conversionService.convert(images, ImagesResponse.class);
    }

    private Long getDefOrgId() {
        return organizationService.getDefaultOrganizationForCurrentUser().getId();
    }

    @Override
    public ImagesResponse getImagesFromDefaultImageCatalogByStack(String stackName) throws Exception {
        Images images = stackImageFilterService.getApplicableImages(getDefOrgId(), stackName);
        return conversionService.convert(images, ImagesResponse.class);
    }

    private ImageCatalogResponse convert(ImageCatalog imageCatalog) {
        return conversionService.convert(imageCatalog, ImageCatalogResponse.class);
    }

    private <S, T> List<T> toJsonList(Iterable<S> objs, Class<T> clss) {
        return (List<T>) conversionService.convert(objs,
                TypeDescriptor.forObject(objs),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(clss)));
    }

    private List<ImageCatalogResponse> getAll() {
        return toJsonList(imageCatalogService.listForUsersDefaultOrganization(), ImageCatalogResponse.class);
    }

    private ImageCatalogResponse post(ImageCatalogRequest imageCatalogRequest) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        ImageCatalog imageCatalog = conversionService.convert(imageCatalogRequest, ImageCatalog.class);
        imageCatalog.setAccount(identityUser.getAccount());
        imageCatalog.setOwner(identityUser.getUserId());
        imageCatalog = imageCatalogService.create(imageCatalog, getDefOrgId());
        return convert(imageCatalog);
    }
}
