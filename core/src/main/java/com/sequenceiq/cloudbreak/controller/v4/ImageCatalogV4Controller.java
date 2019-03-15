package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class ImageCatalogV4Controller extends NotificationController implements ImageCatalogV4Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public ImageCatalogV4Responses list(Long workspaceId) {
        Set<ImageCatalog> allByWorkspaceId = imageCatalogService.findAllByWorkspaceId(workspaceId);
        return new ImageCatalogV4Responses(converterUtil.convertAllAsSet(allByWorkspaceId, ImageCatalogV4Response.class));
    }

    @Override
    public ImageCatalogV4Response get(Long workspaceId, String name, Boolean withImages) {
        ImageCatalogV4Response imageCatalogResponse = converterUtil.convert(imageCatalogService.get(workspaceId, name), ImageCatalogV4Response.class);
        Images images = imageCatalogService.propagateImagesIfRequested(workspaceId, name, withImages);
        if (images != null) {
            imageCatalogResponse.setImages(converterUtil.convert(images, ImagesV4Response.class));
        }
        return imageCatalogResponse;
    }

    @Override
    public ImageCatalogV4Response create(Long workspaceId, ImageCatalogV4Request request) {
        ImageCatalog imageCatalog = imageCatalogService.createForLoggedInUser(converterUtil.convert(request, ImageCatalog.class), workspaceId);
        notify(ResourceEvent.IMAGE_CATALOG_CREATED);
        return converterUtil.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Response delete(Long workspaceId, String name) {
        ImageCatalog deleted = imageCatalogService.delete(workspaceId, name);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return converterUtil.convert(deleted, ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<ImageCatalog> deleted = imageCatalogService.deleteMultiple(workspaceId, names);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return new ImageCatalogV4Responses(converterUtil.convertAllAsSet(deleted, ImageCatalogV4Response.class));
    }

    @Override
    public ImageCatalogV4Response update(Long workspaceId, UpdateImageCatalogV4Request request) {
        ImageCatalog imageCatalog = imageCatalogService.update(workspaceId, converterUtil.convert(request, ImageCatalog.class));
        return converterUtil.convert(imageCatalog, ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Response setDefault(Long workspaceId, String name) {
        return converterUtil.convert(imageCatalogService.setAsDefault(workspaceId, name), ImageCatalogV4Response.class);
    }

    @Override
    public ImageCatalogV4Request getRequest(Long workspaceId, String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(workspaceId, name);
        return converterUtil.convert(imageCatalog, ImageCatalogV4Request.class);
    }

    @Override
    public ImagesV4Response getImages(Long workspaceId, String stackName, String platform) throws Exception {
        Images images = imageCatalogService.getImagesFromDefault(workspaceId, stackName, platform, Collections.emptySet());
        return converterUtil.convert(images, ImagesV4Response.class);
    }

    @Override
    public ImagesV4Response getImagesByName(Long workspaceId, String name, String stackName, String platform) throws Exception {
        Images images = imageCatalogService.getImagesByCatalogName(workspaceId, name, stackName, platform);
        return converterUtil.convert(images, ImagesV4Response.class);
    }

}
