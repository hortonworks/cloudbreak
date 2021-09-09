package com.sequenceiq.cloudbreak.converter.v4.stacks;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Component
public class ImageToStackImageV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageToStackImageV4ResponseConverter.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    public StackImageV4Response convert(Image source) {
        StackImageV4Response image = new StackImageV4Response();
        image.setName(source.getImageName());

        decorateWithImageCatalogUrl(source, image);
        image.setCatalogName(Strings.isNullOrEmpty(source.getImageCatalogName()) ? "cloudbreak-default" : source.getImageCatalogName());
        image.setId(Strings.isNullOrEmpty(source.getImageId()) ? null : source.getImageId());
        return image;
    }

    private void decorateWithImageCatalogUrl(Image source, StackImageV4Response image) {
        if (Strings.isNullOrEmpty(source.getImageCatalogUrl())) {
            LOGGER.debug(String.format("Persisted image catalog url is null for image catalog '%s'", source.getImageCatalogName()));
            ImageCatalog imageCatalog = getImageCatalog(source);
            if (shouldLookupImageCatalogUrlByCloudbreakUser(imageCatalog)) {
                CloudbreakUser cloudbreakUser = legacyRestRequestThreadLocalService.getCloudbreakUser();
                LOGGER.debug(String.format("Lookup the url of '%s' user's image catalog.", cloudbreakUser == null ? null : cloudbreakUser.getUserCrn()));
                User user = userService.getOrCreate(cloudbreakUser);
                image.setCatalogUrl(imageCatalogService.getImageDefaultCatalogUrl(user));
            }
        } else {
            image.setCatalogUrl(source.getImageCatalogUrl());
        }
    }

    private ImageCatalog getImageCatalog(Image source) {
        if (source.getImageCatalogName() != null) {
            try {
                LOGGER.debug(String.format("Try to lookup image catalog '%s' from workspace %d",
                        source.getImageCatalogName(), restRequestThreadLocalService.getRequestedWorkspaceId()));
                return imageCatalogService.getImageCatalogByName(restRequestThreadLocalService.getRequestedWorkspaceId(), source.getImageCatalogName());
            } catch (Exception ex) {
                LOGGER.debug(String.format("Failed to lookup image catalog '%s' from workspace %d",
                        source.getImageCatalogName(), restRequestThreadLocalService.getRequestedWorkspaceId()));
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean shouldLookupImageCatalogUrlByCloudbreakUser(ImageCatalog imageCatalog) {
        return imageCatalog == null || imageCatalog.getImageCatalogUrl() != null;
    }
}
