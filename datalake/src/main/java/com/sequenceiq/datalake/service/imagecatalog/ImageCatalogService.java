package com.sequenceiq.datalake.service.imagecatalog;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Service
public class ImageCatalogService implements ResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Override
    public Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.IMAGE_CATALOG);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ImageCatalogV4Endpoint imageCatalogV4Endpoint = cloudbreakInternalCrnClient.withInternalCrn().imageCatalogV4Endpoint();
        ImageCatalogV4Response response = imageCatalogV4Endpoint.getByNameInternal(SdxService.WORKSPACE_ID_DEFAULT, resourceName, false, initiatorUserCrn);

        return response.getCrn();
    }

    public ImageV4Response getImageResponseFromImageRequest(ImageSettingsV4Request imageSettingsV4Request, CloudPlatform cloudPlatform) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        if (imageSettingsV4Request == null) {
            return null;
        }

        ImageCatalogV4Endpoint imageCatalogV4Endpoint = cloudbreakInternalCrnClient.withInternalCrn().imageCatalogV4Endpoint();

        try {
            LOGGER.info("Calling cloudbreak to get image response for the given image catalog {} and image id {}",
                    imageSettingsV4Request.getCatalog(), imageSettingsV4Request.getId());
            ImagesV4Response imagesV4Response = null;
            try {
                if (Strings.isBlank(imageSettingsV4Request.getCatalog())) {
                    imagesV4Response = imageCatalogV4Endpoint.getImageByImageId(SdxService.WORKSPACE_ID_DEFAULT, imageSettingsV4Request.getId(), accountId);
                } else {
                    imagesV4Response = imageCatalogV4Endpoint.getImageByCatalogNameAndImageId(SdxService.WORKSPACE_ID_DEFAULT,
                            imageSettingsV4Request.getCatalog(), imageSettingsV4Request.getId(), accountId);
                }
            } catch (Exception e) {
                LOGGER.error("Sdx service fails to get image using image id", e);
            }

            if (imagesV4Response == null) {
                return null;
            }

            for (ImageV4Response imageV4Response : imagesV4Response.getCdhImages()) {
                // find the image can be used on the cloud platform of the environment
                if (imageV4Response.getImageSetsByProvider() != null) {
                    if (imageV4Response.getImageSetsByProvider().containsKey(cloudPlatform.name().toLowerCase())) {
                        return imageV4Response;
                    }
                }
            }

            String errorMessage = String.format("SDX cluster is on the cloud platform %s, but the image requested with uuid %s:%s does not support it",
                    cloudPlatform.name(), imageSettingsV4Request.getCatalog() != null ? imageSettingsV4Request.getCatalog() : "default",
                    imageSettingsV4Request.getId());
            LOGGER.error(errorMessage);

            return null;
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }
}
