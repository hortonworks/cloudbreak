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
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
@Transactional(TxType.NEVER)
public class ImageCatalogV1Controller implements ImageCatalogV1Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public List<ImageCatalogResponse> getPublics() {
        return toJsonList(imageCatalogService.getAllPublicInAccount(), ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogResponse getPublicByName(String name, boolean withImages) {
        ImageCatalogResponse imageCatalogResponse = convert(imageCatalogService.get(name));
        Images images = imageCatalogService.propagateImagesIfRequested(name, withImages);
        if (images != null) {
            imageCatalogResponse.setImagesResponse(conversionService.convert(images, ImagesResponse.class));
        }
        return imageCatalogResponse;
    }

    @Override
    public ImagesResponse getImagesByProvider(String platform) throws Exception {
        Images images = imageCatalogService.getImages(platform).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImageCatalogResponse postPublic(ImageCatalogRequest imageCatalogRequest) {
        return createImageCatalog(imageCatalogRequest, true);
    }

    @Override
    public ImageCatalogResponse postPrivate(ImageCatalogRequest imageCatalogRequest) {
        return createImageCatalog(imageCatalogRequest, false);
    }

    @Override
    public ImagesResponse getImagesByProviderFromImageCatalog(String name, String platform) throws Exception {
        Images images = imageCatalogService.getImages(name, platform).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public void deletePublic(String name) {
        imageCatalogService.delete(name);
    }

    @Override
    public ImageCatalogResponse putPublic(UpdateImageCatalogRequest request) {
        ImageCatalog imageCatalog = imageCatalogService.update(conversionService.convert(request, ImageCatalog.class));
        return convert(imageCatalog);
    }

    @Override
    public ImageCatalogResponse putSetDefaultByName(String name) {
        return conversionService.convert(imageCatalogService.setAsDefault(name), ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogRequest getRequestfromName(String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(name);
        return conversionService.convert(imageCatalog, ImageCatalogRequest.class);
    }

    private ImageCatalogResponse createImageCatalog(ImageCatalogRequest imageCatalogRequest, boolean publicInAccount) {
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
