package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
@InternalReady
@AuthorizationResource
public class ImageCatalogV4Controller extends NotificationController implements ImageCatalogV4Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    @DisableCheckPermissions
    public ImageCatalogV4Responses list(Long workspaceId) {
        Set<ImageCatalog> allByWorkspaceId = imageCatalogService.findAllByWorkspaceId(workspaceId);
        return new ImageCatalogV4Responses(converterUtil.convertAllAsSet(allByWorkspaceId, ImageCatalogV4Response.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Response getByName(Long workspaceId, @ResourceName String name, Boolean withImages) {
        ImageCatalog catalog = imageCatalogService.get(NameOrCrn.ofName(name), workspaceId);
        ImageCatalogV4Response imageCatalogResponse = converterUtil.convert(catalog, ImageCatalogV4Response.class);
        Images images = imageCatalogService.propagateImagesIfRequested(workspaceId, name, withImages);
        if (images != null) {
            imageCatalogResponse.setImages(converterUtil.convert(images, ImagesV4Response.class));
        }
        return imageCatalogResponse;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Response getByCrn(Long workspaceId, @ResourceCrn String crn, Boolean withImages) {
        ImageCatalog catalog = imageCatalogService.get(NameOrCrn.ofCrn(crn), workspaceId);
        ImageCatalogV4Response imageCatalogResponse = converterUtil.convert(catalog, ImageCatalogV4Response.class);
        Images images = imageCatalogService.propagateImagesIfRequested(workspaceId, catalog.getName(), withImages);
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
        ImageCatalog imageCatalog = imageCatalogService.createForLoggedInUser(catalogToSave, workspaceId, accountId, creator);
        notify(ResourceEvent.IMAGE_CATALOG_CREATED);
        return converterUtil.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        ImageCatalog deleted = imageCatalogService.delete(NameOrCrn.ofName(name), workspaceId);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return converterUtil.convert(deleted, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        ImageCatalog deleted = imageCatalogService.delete(NameOrCrn.ofCrn(crn), workspaceId);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return converterUtil.convert(deleted, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<ImageCatalog> deleted = imageCatalogService.deleteMultiple(workspaceId, names);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return new ImageCatalogV4Responses(converterUtil.convertAllAsSet(deleted, ImageCatalogV4Response.class));
    }

    @Override
    @CheckPermissionByResourceObject
    public ImageCatalogV4Response update(Long workspaceId, @ResourceObject UpdateImageCatalogV4Request request) {
        ImageCatalog imageCatalog = imageCatalogService.update(workspaceId, converterUtil.convert(request, ImageCatalog.class));
        return converterUtil.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Request getRequest(Long workspaceId, @ResourceName String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(workspaceId, name);
        return converterUtil.convert(imageCatalog, ImageCatalogV4Request.class);
    }

    @Override
    @DisableCheckPermissions
    public ImagesV4Response getImages(Long workspaceId, String stackName, String platform) throws Exception {
        Images images = imageCatalogService.getImagesFromDefault(workspaceId, stackName, platform, Collections.emptySet());
        return converterUtil.convert(images, ImagesV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImagesV4Response getImagesByName(Long workspaceId, @ResourceName String name, String stackName, String platform) throws Exception {
        Images images = imageCatalogService.getImagesByCatalogName(workspaceId, name, stackName, platform);
        return converterUtil.convert(images, ImagesV4Response.class);
    }
}
