package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses.imageCatalogResponses;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.filter.GetImageCatalogV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.filter.ImageCatalogGetImagesV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.NotificationController;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StackImageFilterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class ImageCatalogV4Controller extends NotificationController implements ImageCatalogV4Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackImageFilterService stackImageFilterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ImageCatalogV4Responses list(Long workspaceId) {
        Set<ImageCatalogV4Response> responses = imageCatalogService.findAllByWorkspaceId(workspaceId).stream()
                .map(imageCatalog -> conversionService.convert(imageCatalog, ImageCatalogV4Response.class))
                .collect(Collectors.toSet());
        return imageCatalogResponses(responses);
    }

    @Override
    public ImageCatalogV4Response get(Long workspaceId, String name, GetImageCatalogV4Filter getImageCatalogV4Filter) {
        ImageCatalogV4Response imageCatalogResponse = conversionService.convert(imageCatalogService.get(workspaceId, name), ImageCatalogV4Response.class);
        Images images = imageCatalogService.propagateImagesIfRequested(workspaceId, name, getImageCatalogV4Filter.getWithImages());
        if (images != null) {
            imageCatalogResponse.setImages(conversionService.convert(images, ImagesV4Response.class));
        }
        return imageCatalogResponse;
    }

    @Override
    public ImageCatalogV4Response create(Long workspaceId, ImageCatalogV4Request request) {
        ImageCatalog imageCatalog = conversionService.convert(request, ImageCatalog.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        imageCatalog = imageCatalogService.create(imageCatalog, workspaceId, user);
        notify(ResourceEvent.IMAGE_CATALOG_CREATED);
        return conversionService.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Response delete(Long workspaceId, String name) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        ImageCatalog deleted = imageCatalogService.delete(workspaceId, name, cloudbreakUser, user);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return conversionService.convert(deleted, ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Response update(Long workspaceId, UpdateImageCatalogV4Request request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        ImageCatalog imageCatalog = imageCatalogService.update(workspaceId, conversionService.convert(request, ImageCatalog.class), user);
        return conversionService.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Response setDefault(Long workspaceId, String name) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return conversionService.convert(imageCatalogService.setAsDefault(workspaceId, name, cloudbreakUser, user), ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Request getRequest(Long workspaceId, String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(workspaceId, name);
        return conversionService.convert(imageCatalog, ImageCatalogV4Request.class);
    }

    @Override
    public ImagesV4Response getImages(Long workspaceId, ImageCatalogGetImagesV4Filter imageCatalogGetImagesV4Filter) throws Exception {
        if (StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getPlatform()) &&
                StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getStackName())) {
            throw new BadRequestException("Platform or stackName cannot be filled in the same request.");
        }
        Images images;
        if (StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getPlatform())) {
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            images = imageCatalogService.getImagesOsFiltered(imageCatalogGetImagesV4Filter.getPlatform(), null, user).getImages();
        } else if (StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getStackName())) {
            images = stackImageFilterService.getApplicableImages(workspaceId, imageCatalogGetImagesV4Filter.getStackName());
        } else {
            throw new BadRequestException("Either platform or stackName should be filled in request.");
        }
        return conversionService.convert(images, ImagesV4Response.class);
    }

    @Override
    public ImagesV4Response getImagesByName(Long workspaceId, String name, ImageCatalogGetImagesV4Filter imageCatalogGetImagesV4Filter) throws Exception {
        if (StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getPlatform()) &&
                StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getStackName())) {
            throw new BadRequestException("Platform or stackName cannot be filled in the same request.");
        }
        Images images;
        if (StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getPlatform())) {
            images = imageCatalogService.getImages(workspaceId, name, imageCatalogGetImagesV4Filter.getPlatform()).getImages();
        } else if (StringUtils.isNotEmpty(imageCatalogGetImagesV4Filter.getStackName())) {
            images = stackImageFilterService.getApplicableImages(workspaceId, name, imageCatalogGetImagesV4Filter.getStackName());
        } else {
            throw new BadRequestException("Either platform or stackName should be filled in request.");
        }
        return conversionService.convert(images, ImagesV4Response.class);
    }


}
