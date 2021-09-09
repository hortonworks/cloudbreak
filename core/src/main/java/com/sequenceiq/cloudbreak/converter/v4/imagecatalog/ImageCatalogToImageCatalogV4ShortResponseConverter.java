package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4ShortResponse;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class ImageCatalogToImageCatalogV4ShortResponseConverter {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    public ImageCatalogV4ShortResponse convert(ImageCatalog source) {
        ImageCatalogV4ShortResponse imageCatalogResponse = new ImageCatalogV4ShortResponse();
        imageCatalogResponse.setCreator(source.getCreator());
        imageCatalogResponse.setCrn(source.getResourceCrn());
        imageCatalogResponse.setUrl(source.getImageCatalogUrl());

        String imageCatalogName = source.getName();
        imageCatalogResponse.setUsedAsDefault(isDefault(imageCatalogName));
        imageCatalogResponse.setName(imageCatalogName);
        imageCatalogResponse.setDescription(source.getDescription());

        return imageCatalogResponse;
    }

    private boolean isDefault(String imageCatalogName) {
        CloudbreakUser cloudbreakUser = legacyRestRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        String defaultImageCatalogName = imageCatalogService.getDefaultImageCatalogName(user);
        return imageCatalogName.equals(defaultImageCatalogName) || (defaultImageCatalogName == null && imageCatalogService.isEnvDefault(imageCatalogName));
    }

}
