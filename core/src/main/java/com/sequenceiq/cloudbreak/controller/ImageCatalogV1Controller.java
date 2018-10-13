package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StackImageFilterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class ImageCatalogV1Controller implements ImageCatalogV1Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackImageFilterService stackImageFilterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Override
    public List<ImageCatalogResponse> getPublics() {
        return getAll();
    }

    @Override
    public ImageCatalogResponse getByName(String name, boolean withImages) {
        ImageCatalogResponse imageCatalogResponse = convert(imageCatalogService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), name));
        Images images = imageCatalogService.propagateImagesIfRequested(restRequestThreadLocalService.getRequestedWorkspaceId(), name, withImages);
        if (images != null) {
            imageCatalogResponse.setImagesResponse(conversionService.convert(images, ImagesResponse.class));
        }
        return imageCatalogResponse;
    }

    @Override
    public ImagesResponse getImagesByProvider(String platform) throws Exception {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Images images = imageCatalogService.getImagesOsFiltered(platform, null, user).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImageCatalogResponse postPublic(ImageCatalogRequest imageCatalogRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return post(imageCatalogRequest, user);
    }

    @Override
    public ImageCatalogResponse postPrivate(ImageCatalogRequest imageCatalogRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return post(imageCatalogRequest, user);
    }

    @Override
    public ImagesResponse getImagesByProviderFromImageCatalog(String name, String platform) throws Exception {
        Images images = imageCatalogService.getImages(restRequestThreadLocalService.getRequestedWorkspaceId(), name, platform).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public void deletePublic(String name) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        imageCatalogService.delete(workspace.getId(), name, cloudbreakUser, user);
    }

    @Override
    public ImageCatalogResponse putPublic(UpdateImageCatalogRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        ImageCatalog imageCatalog = imageCatalogService.update(restRequestThreadLocalService.getRequestedWorkspaceId(),
                conversionService.convert(request, ImageCatalog.class), user);
        return convert(imageCatalog);
    }

    @Override
    public ImageCatalogResponse putSetDefaultByName(String name) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return conversionService.convert(imageCatalogService.setAsDefault(restRequestThreadLocalService.getRequestedWorkspaceId(), name, cloudbreakUser, user),
                ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogRequest getRequestfromName(String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
        return conversionService.convert(imageCatalog, ImageCatalogRequest.class);
    }

    @Override
    public ImagesResponse getImagesFromCustomImageCatalogByStack(String imageCatalogName, String stackName) throws CloudbreakImageCatalogException {
        Images images = stackImageFilterService.getApplicableImages(restRequestThreadLocalService.getRequestedWorkspaceId(), imageCatalogName, stackName);
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImagesResponse getImagesFromDefaultImageCatalogByStack(String stackName) throws Exception {
        Images images = stackImageFilterService.getApplicableImages(restRequestThreadLocalService.getRequestedWorkspaceId(), stackName);
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
        return toJsonList(imageCatalogService.findAllByWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId()), ImageCatalogResponse.class);
    }

    private ImageCatalogResponse post(ImageCatalogRequest imageCatalogRequest, User user) {
        ImageCatalog imageCatalog = conversionService.convert(imageCatalogRequest, ImageCatalog.class);
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        imageCatalog = imageCatalogService.create(imageCatalog, workspaceId, user);
        return convert(imageCatalog);
    }
}
