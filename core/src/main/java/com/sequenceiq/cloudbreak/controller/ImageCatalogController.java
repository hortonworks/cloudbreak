package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogEndpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class ImageCatalogController implements ImageCatalogEndpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public List<ImageCatalogResponse> getPublics() throws Exception {
        return toJsonList(imageCatalogService.getAllPublicInAccount(), ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogResponse getPublicByName(String name) throws Exception {
        return convert(imageCatalogService.get(name));
    }

    @Override
    public ImagesResponse getImagesByProvider(String platform) throws Exception {
        Images images = imageCatalogService.getImages(platform);
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImageCatalogResponse postPublic(ImageCatalogRequest imageCatalogRequest) throws Exception {
        return createImageCatalog(imageCatalogRequest, true);
    }

    @Override
    public ImageCatalogResponse postPrivate(ImageCatalogRequest imageCatalogRequest) throws Exception {
        return createImageCatalog(imageCatalogRequest, false);
    }

    @Override
    public ImagesResponse getImagesByProviderFromImageCatalog(String name, String platform) throws Exception {
        return conversionService.convert(imageCatalogService.getImages(name, platform), ImagesResponse.class);
    }

    @Override
    public void deletePublic(String name) {
        imageCatalogService.delete(name);
    }

    @Override
    public ImageCatalogResponse putPublic(UpdateImageCatalogRequest request) throws CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = imageCatalogService.update(conversionService.convert(request, ImageCatalog.class));
        return convert(imageCatalog);
    }

    @Override
    public ImageCatalogResponse putSetDefaultByName(String name) {
        return conversionService.convert(imageCatalogService.setAsDefault(name), ImageCatalogResponse.class);
    }

    private ImageCatalogResponse createImageCatalog(ImageCatalogRequest imageCatalogRequest, boolean publicInAccount) throws Exception {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        ImageCatalog imageCatalog = conversionService.convert(imageCatalogRequest, ImageCatalog.class);
        imageCatalog.setAccount(identityUser.getAccount());
        imageCatalog.setOwner(identityUser.getUserId());
        imageCatalog.setPublicInAccount(publicInAccount);
        imageCatalog = imageCatalogService.create(imageCatalog);
        return convert(imageCatalog);
    }

    private ImageCatalogResponse convert(ImageCatalog imageCatalog) {
        return conversionService.convert(imageCatalog, ImageCatalogResponse.class);
    }

    private <S, T> List<T> toJsonList(Iterable<S> objs, Class<T> clss) {
        return (List<T>) conversionService.convert(objs,
                TypeDescriptor.forObject(objs),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(clss)));
    }
}
