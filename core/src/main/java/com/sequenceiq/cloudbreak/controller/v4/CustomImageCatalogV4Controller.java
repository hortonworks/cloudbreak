package com.sequenceiq.cloudbreak.controller.v4;

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
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import org.springframework.stereotype.Controller;

import javax.transaction.Transactional;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class CustomImageCatalogV4Controller implements CustomImageCatalogV4Endpoint {

    @Override
    @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, filter = ImageCatalogFiltering.class)
    public CustomImageCatalogV4ListResponse list() {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public CustomImageCatalogV4GetResponse get(@ResourceName String name) {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_IMAGE_CATALOG)
    public CustomImageCatalogV4CreateResponse create(CustomImageCatalogV4CreateRequest request) {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public CustomImageCatalogV4DeleteResponse delete(@ResourceName String name) {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public CustomImageCatalogV4GetImageResponse getCustomImage(@ResourceName String name, String imageId) {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4CreateImageResponse createCustomImage(@ResourceName String name,
            CustomImageCatalogV4CreateImageRequest request) {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4UpdateImageResponse updateCustomImage(@ResourceName String name, String imageId,
            CustomImageCatalogV4UpdateImageRequest request) {
        //FIXME Implement this!
        return null;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG)
    public CustomImageCatalogV4DeleteImageResponse deleteCustomImage(@ResourceName String name, String imageId) {
        //FIXME Implement this!
        return null;
    }
}
