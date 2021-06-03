package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListItemResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4UpdateImageResponse;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.CustomImageCatalogService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

import javax.validation.Valid;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class CustomImageCatalogV4Controller implements CustomImageCatalogV4Endpoint {

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CustomImageCatalogService customImageCatalogService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ImageCatalogFiltering imageCatalogFiltering;

    @Override
    @FilterListBasedOnPermissions
    public CustomImageCatalogV4ListResponse list(@AccountId String accountId) {
        Set<ImageCatalog> imageCatalogs = imageCatalogFiltering.filterImageCatalogs(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, true);
        return new CustomImageCatalogV4ListResponse(converterUtil.convertAllAsSet(imageCatalogs, CustomImageCatalogV4ListItemResponse.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public CustomImageCatalogV4GetResponse get(@ResourceName String name, @AccountId String accountId) {
        ImageCatalog imageCatalog = customImageCatalogService.getImageCatalog(restRequestThreadLocalService.getRequestedWorkspaceId(), name);

        return converterUtil.convert(imageCatalog, CustomImageCatalogV4GetResponse.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_IMAGE_CATALOG)
    public CustomImageCatalogV4CreateResponse create(@Valid CustomImageCatalogV4CreateRequest request, @AccountId String accountId) {
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ImageCatalog imageCatalog = converterUtil.convert(request, ImageCatalog.class);
        ImageCatalog savedImageCatalog = customImageCatalogService
                .create(imageCatalog, restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, creator);

        return converterUtil.convert(savedImageCatalog, CustomImageCatalogV4CreateResponse.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public CustomImageCatalogV4DeleteResponse delete(@ResourceName String name, @AccountId String accountId) {
        ImageCatalog imageCatalog = customImageCatalogService.delete(restRequestThreadLocalService.getRequestedWorkspaceId(), name);

        return converterUtil.convert(imageCatalog, CustomImageCatalogV4DeleteResponse.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public CustomImageCatalogV4GetImageResponse getCustomImage(@ResourceName String name, String imageId, @AccountId String accountId) {
        CustomImage customImage = customImageCatalogService.getCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), name, imageId);
        Image sourceImage = customImageCatalogService.getSourceImage(customImage);
        CustomImageCatalogV4GetImageResponse response = converterUtil.convert(customImage, CustomImageCatalogV4GetImageResponse.class);
        response.setSourceImageDate(sourceImage.getCreated());

        return response;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4CreateImageResponse createCustomImage(@ResourceName String name,
            @Valid CustomImageCatalogV4CreateImageRequest request, @AccountId String accountId) {
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        CustomImage customImage = converterUtil.convert(request, CustomImage.class);
        CustomImage savedCustomImage = customImageCatalogService
                .createCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, creator, name, customImage);

        return converterUtil.convert(savedCustomImage, CustomImageCatalogV4CreateImageResponse.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4UpdateImageResponse updateCustomImage(@ResourceName String name, String imageId,
            @Valid CustomImageCatalogV4UpdateImageRequest request, @AccountId String accountId) {
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        CustomImage customImage = converterUtil.convert(request, CustomImage.class);
        customImage.setName(imageId);
        CustomImage savedCustomImage = customImageCatalogService
                .updateCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), creator, name, customImage);

        return converterUtil.convert(savedCustomImage, CustomImageCatalogV4UpdateImageResponse.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4DeleteImageResponse deleteCustomImage(@ResourceName String name, String imageId, @AccountId String accountId) {
        CustomImage customImage = customImageCatalogService.deleteCustomImage(restRequestThreadLocalService.getRequestedWorkspaceId(), name, imageId);

        return converterUtil.convert(customImage, CustomImageCatalogV4DeleteImageResponse.class);
    }
}
