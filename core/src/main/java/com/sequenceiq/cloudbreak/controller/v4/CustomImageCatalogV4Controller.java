package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.CustomImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4UpdateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4UpdateImageResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageCatalogV4CreateImageRequestToCustomImageConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageCatalogV4CreateRequestToImageCatalogConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageCatalogV4UpdateImageRequestToCustomImageConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4CreateImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4DeleteImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4GetImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4UpdateImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4CreateResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4DeleteResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4GetResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4ListItemResponseConverter;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.CustomImageCatalogService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class CustomImageCatalogV4Controller implements CustomImageCatalogV4Endpoint {

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CustomImageCatalogService customImageCatalogService;

    @Inject
    private ImageCatalogFiltering imageCatalogFiltering;

    @Inject
    private ImageCatalogToCustomImageCatalogV4ListItemResponseConverter imageCatalogToCustomImageCatalogV4ListItemResponseConverter;

    @Inject
    private CustomImageToCustomImageCatalogV4GetImageResponseConverter customImageToCustomImageCatalogV4GetImageResponseConverter;

    @Inject
    private CustomImageToCustomImageCatalogV4CreateImageResponseConverter customImageToCustomImageCatalogV4CreateImageResponseConverter;

    @Inject
    private CustomImageToCustomImageCatalogV4DeleteImageResponseConverter customImageToCustomImageCatalogV4DeleteImageResponseConverter;

    @Inject
    private CustomImageCatalogV4CreateImageRequestToCustomImageConverter customImageCatalogV4CreateImageRequestToCustomImageConverter;

    @Inject
    private CustomImageToCustomImageCatalogV4UpdateImageResponseConverter customImageToCustomImageCatalogV4UpdateImageResponseConverter;

    @Inject
    private CustomImageCatalogV4UpdateImageRequestToCustomImageConverter customImageCatalogV4UpdateImageRequestToCustomImageConverter;

    @Inject
    private CustomImageCatalogV4CreateRequestToImageCatalogConverter customImageCatalogV4CreateRequestToImageCatalogConverter;

    @Inject
    private ImageCatalogToCustomImageCatalogV4GetResponseConverter imageCatalogToCustomImageCatalogV4GetResponseConverter;

    @Inject
    private ImageCatalogToCustomImageCatalogV4CreateResponseConverter imageCatalogToCustomImageCatalogV4CreateResponseConverter;

    @Inject
    private ImageCatalogToCustomImageCatalogV4DeleteResponseConverter imageCatalogToCustomImageCatalogV4DeleteResponseConverter;

    @Override
    @FilterListBasedOnPermissions
    public CustomImageCatalogV4ListResponse list(@AccountId String accountId) {
        Set<ImageCatalog> imageCatalogs = imageCatalogFiltering.filterImageCatalogs(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, true);
        return new CustomImageCatalogV4ListResponse(
                imageCatalogs.stream()
                .map(i -> imageCatalogToCustomImageCatalogV4ListItemResponseConverter.convert(i))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public CustomImageCatalogV4GetResponse get(@ResourceName String name, @AccountId String accountId) {
        ImageCatalog imageCatalog = customImageCatalogService.getImageCatalog(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
        return imageCatalogToCustomImageCatalogV4GetResponseConverter.convert(imageCatalog);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_IMAGE_CATALOG)
    public CustomImageCatalogV4CreateResponse create(CustomImageCatalogV4CreateRequest request, @AccountId String accountId) {
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ImageCatalog imageCatalog = customImageCatalogV4CreateRequestToImageCatalogConverter.convert(request);
        ImageCatalog savedImageCatalog = customImageCatalogService
                .create(imageCatalog, restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, creator);

        return imageCatalogToCustomImageCatalogV4CreateResponseConverter.convert(savedImageCatalog);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public CustomImageCatalogV4DeleteResponse delete(@ResourceName String name, @AccountId String accountId) {
        ImageCatalog imageCatalog = customImageCatalogService.delete(restRequestThreadLocalService.getRequestedWorkspaceId(), name);

        return imageCatalogToCustomImageCatalogV4DeleteResponseConverter.convert(imageCatalog);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public CustomImageCatalogV4GetImageResponse getCustomImage(@ResourceName String name, String imageId, @AccountId String accountId) {
        CustomImage customImage = customImageCatalogService.getCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), name, imageId);
        Image sourceImage = customImageCatalogService.getSourceImage(customImage);
        CustomImageCatalogV4GetImageResponse response = customImageToCustomImageCatalogV4GetImageResponseConverter
                .convert(customImage);
        response.setSourceImageDate(sourceImage.getCreated());

        return response;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4CreateImageResponse createCustomImage(@ResourceName String name,
            CustomImageCatalogV4CreateImageRequest request, @AccountId String accountId) {
        CustomImage customImage = customImageCatalogV4CreateImageRequestToCustomImageConverter.convert(request);
        CustomImage savedCustomImage = customImageCatalogService
                .createCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, name, customImage);

        return customImageToCustomImageCatalogV4CreateImageResponseConverter.convert(savedCustomImage);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4UpdateImageResponse updateCustomImage(@ResourceName String name, String imageId,
            CustomImageCatalogV4UpdateImageRequest request, @AccountId String accountId) {
        CustomImage customImage = customImageCatalogV4UpdateImageRequestToCustomImageConverter.convert(request);
        customImage.setName(imageId);
        CustomImage savedCustomImage = customImageCatalogService
                .updateCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), name, customImage);

        return customImageToCustomImageCatalogV4UpdateImageResponseConverter.convert(savedCustomImage);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4DeleteImageResponse deleteCustomImage(@ResourceName String name, String imageId, @AccountId String accountId) {
        CustomImage customImage = customImageCatalogService.deleteCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), name, imageId);

        return customImageToCustomImageCatalogV4DeleteImageResponseConverter.convert(customImage);
    }
}
