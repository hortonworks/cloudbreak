package com.sequenceiq.datalake.service.imagecatalog;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Service
public class ImageCatalogService implements AuthorizationResourceCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.IMAGE_CATALOG;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ImageCatalogV4Endpoint imageCatalogV4Endpoint = cloudbreakInternalCrnClient.withInternalCrn().imageCatalogV4Endpoint();
        ImageCatalogV4Response response = imageCatalogV4Endpoint.getByNameInternal(SdxService.WORKSPACE_ID_DEFAULT, resourceName, false, initiatorUserCrn);

        return response.getCrn();
    }

    public ImageV4Response getImageResponseFromImageRequest(ImageSettingsV4Request imageSettingsV4Request, ImageCatalogPlatform imageCatalogPlatform) {
        if (imageSettingsV4Request != null) {
            List<ImageV4Response> images = getImagesMatchingRequest(imageSettingsV4Request);
            if (images != null) {
                String providerName = imageCatalogPlatform.nameToLowerCase();
                for (ImageV4Response imageV4Response : images) {
                    // find the image can be used on the cloud platform of the environment
                    if (imageV4Response.getImageSetsByProvider() != null && imageV4Response.getImageSetsByProvider().containsKey(providerName)) {
                        return imageV4Response;
                    }
                }
                String errorMessage = String.format("SDX cluster is on the cloud platform %s, but the image requested with uuid %s:%s does not support it",
                        providerName, imageSettingsV4Request.getCatalog() != null ? imageSettingsV4Request.getCatalog() : "default",
                        imageSettingsV4Request.getId());
                LOGGER.error(errorMessage);
            }
        }
        return null;
    }

    private List<ImageV4Response> getImagesMatchingRequest(ImageSettingsV4Request imageSettingsV4Request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ImageCatalogV4Endpoint imageCatalogV4Endpoint = cloudbreakInternalCrnClient.withInternalCrn().imageCatalogV4Endpoint();

        LOGGER.info("Calling cloudbreak to get image response for the given image catalog {} and image id {}",
                imageSettingsV4Request.getCatalog(), imageSettingsV4Request.getId());
        try {
            ImagesV4Response imagesV4Response;
            if (StringUtils.isBlank(imageSettingsV4Request.getCatalog())) {
                imagesV4Response = imageCatalogV4Endpoint.getImageByImageId(SdxService.WORKSPACE_ID_DEFAULT, imageSettingsV4Request.getId(), accountId);
            } else {
                imagesV4Response = imageCatalogV4Endpoint.getImageByCatalogNameAndImageId(SdxService.WORKSPACE_ID_DEFAULT,
                        imageSettingsV4Request.getCatalog(), imageSettingsV4Request.getId(), accountId);
            }
            List<ImageV4Response> images = new LinkedList<>();
            if (imagesV4Response.getCdhImages() != null) {
                images.addAll(imagesV4Response.getCdhImages());
            }
            if (imagesV4Response.getBaseImages() != null) {
                images.addAll(imagesV4Response.getBaseImages());
            }
            return images;
        } catch (Exception e) {
            LOGGER.error("Sdx service failed to get images for request {}", imageSettingsV4Request, e);
            return null;
        }
    }

    public List<String> getDefaultImageCatalogRuntimeVersions(Long workspaceId) {
        ImageCatalogV4Endpoint imageCatalogV4Endpoint = cloudbreakInternalCrnClient.withInternalCrn().imageCatalogV4Endpoint();
        try {
            return imageCatalogV4Endpoint.getRuntimeVersionsFromDefault(workspaceId).getRuntimeVersions();
        } catch (Exception ex) {
            LOGGER.error("Failed to get runtime versions from default image catalog.", ex);
            throw new CloudbreakServiceException(ex);
        }
    }
}
