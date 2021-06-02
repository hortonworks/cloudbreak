package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_IMAGE_CATALOG;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.service.image.DefaultImageCatalogService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class ImageCatalogV4Controller extends NotificationController implements ImageCatalogV4Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private DefaultImageCatalogService defaultImageCatalogService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ImageCatalogFiltering imageCatalogFiltering;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    @FilterListBasedOnPermissions
    public ImageCatalogV4Responses list(Long workspaceId, boolean customCatalogsOnly) {
        Set<ImageCatalog> allByWorkspaceId = imageCatalogFiltering.filterImageCatalogs(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, customCatalogsOnly);
        return new ImageCatalogV4Responses(converterUtil.convertAllAsSet(allByWorkspaceId, ImageCatalogV4Response.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Response getByName(Long workspaceId, @ResourceName String name, Boolean withImages) {
        ImageCatalog catalog = imageCatalogService.get(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        ImageCatalogV4Response imageCatalogResponse = converterUtil.convert(catalog, ImageCatalogV4Response.class);
        Images images = imageCatalogService.propagateImagesIfRequested(restRequestThreadLocalService.getRequestedWorkspaceId(), name, withImages);
        if (images != null) {
            imageCatalogResponse.setImages(converterUtil.convert(images, ImagesV4Response.class));
        }
        return imageCatalogResponse;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Response getByCrn(Long workspaceId, @ResourceCrn String crn, Boolean withImages) {
        ImageCatalog catalog = imageCatalogService.get(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        ImageCatalogV4Response imageCatalogResponse = converterUtil.convert(catalog, ImageCatalogV4Response.class);
        Images images = imageCatalogService.propagateImagesIfRequested(restRequestThreadLocalService.getRequestedWorkspaceId(), catalog.getName(), withImages);
        if (images != null) {
            imageCatalogResponse.setImages(converterUtil.convert(images, ImagesV4Response.class));
        }
        return imageCatalogResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_IMAGE_CATALOG)
    public ImageCatalogV4Response create(Long workspaceId, ImageCatalogV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ImageCatalog catalogToSave = converterUtil.convert(request, ImageCatalog.class);
        ImageCatalog imageCatalog = imageCatalogService.createForLoggedInUser(catalogToSave, restRequestThreadLocalService.getRequestedWorkspaceId(),
                accountId, creator);
        notify(ResourceEvent.IMAGE_CATALOG_CREATED);
        return converterUtil.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        ImageCatalog deleted = imageCatalogService.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return converterUtil.convert(deleted, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        ImageCatalog deleted = imageCatalogService.delete(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return converterUtil.convert(deleted, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<ImageCatalog> deleted = imageCatalogService.deleteMultiple(restRequestThreadLocalService.getRequestedWorkspaceId(), names);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return new ImageCatalogV4Responses(converterUtil.convertAllAsSet(deleted, ImageCatalogV4Response.class));
    }

    @Override
    @CheckPermissionByRequestProperty(path = "crn", type = CRN, action = EDIT_IMAGE_CATALOG)
    public ImageCatalogV4Response update(Long workspaceId, @RequestObject UpdateImageCatalogV4Request request) {
        ImageCatalog imageCatalog = imageCatalogService.update(restRequestThreadLocalService.getRequestedWorkspaceId(),
                converterUtil.convert(request, ImageCatalog.class));
        return converterUtil.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Request getRequest(Long workspaceId, @ResourceName String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
        return converterUtil.convert(imageCatalog, ImageCatalogV4Request.class);
    }

    @Override
    @DisableCheckPermissions
    public ImagesV4Response getImages(Long workspaceId, String stackName, String platform,
            String runtimeVersion, String imageType) throws Exception {
        Images images = imageCatalogService.getImagesFromDefault(restRequestThreadLocalService.getRequestedWorkspaceId(), stackName,
                platform, Collections.emptySet());
        return converterUtil.convert(images, ImagesV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImagesV4Response getImagesByName(Long workspaceId, @ResourceName String name, String stackName, String platform,
        String runtimeVersion, String imageType) throws Exception {
        Images images = imageCatalogService.getImagesByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), name, stackName, platform);
        return converterUtil.convert(images, ImagesV4Response.class);
    }

    @Override
    @DisableCheckPermissions
    public ImagesV4Response getImageByImageId(Long workspaceId, String imageId, @AccountId String accountId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, "");
        Images images = new Images(List.of(), List.of(statedImage.getImage()), List.of(), Set.of());
        return converterUtil.convert(images, ImagesV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImagesV4Response getImageByCatalogNameAndImageId(Long workspaceId, @ResourceName String name,
            String imageId, @AccountId String accountId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, name);
        Images images = new Images(List.of(), List.of(statedImage.getImage()), List.of(), Set.of());
        return converterUtil.convert(images, ImagesV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageV4Response getSingleImageByCatalogNameAndImageId(Long workspaceId, @ResourceName String name, String imageId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, name);
        return converterUtil.convert(statedImage.getImage(), ImageV4Response.class);
    }

    @Override
    @AccountIdNotNeeded
    @InternalOnly
    public ImageV4Response getImageFromDefaultById(Long workspaceId, @ResourceName String imageId) throws Exception {
        StatedImage statedImage = defaultImageCatalogService.getImageFromDefaultCatalog(imageId);
        return converterUtil.convert(statedImage.getImage(), ImageV4Response.class);
    }

    @Override
    @AccountIdNotNeeded
    @InternalOnly
    public ImageV4Response getImageFromDefault(Long workspaceId, String type, String provider, String runtime) throws Exception {
        StatedImage statedImage = defaultImageCatalogService.getImageFromDefaultCatalog(type, provider, runtime);
        return converterUtil.convert(statedImage.getImage(), ImageV4Response.class);
    }

    @Override
    @AccountIdNotNeeded
    @InternalOnly
    public ImageV4Response getImageFromDefault(Long workspaceId, String type, String provider) throws Exception {
        StatedImage statedImage = defaultImageCatalogService.getImageFromDefaultCatalog(type, provider);
        return converterUtil.convert(statedImage.getImage(), ImageV4Response.class);
    }
}
