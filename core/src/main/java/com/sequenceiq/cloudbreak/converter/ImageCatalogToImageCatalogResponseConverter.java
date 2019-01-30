package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class ImageCatalogToImageCatalogResponseConverter extends AbstractConversionServiceAwareConverter<ImageCatalog, ImageCatalogResponse> {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ImageCatalogResponse convert(ImageCatalog source) {
        ImageCatalogResponse imageCatalogResponse = new ImageCatalogResponse();
        imageCatalogResponse.setId(source.getId());
        imageCatalogResponse.setUrl(source.getImageCatalogUrl());

        String imageCatalogName = source.getName();
        imageCatalogResponse.setUsedAsDefault(isDefault(imageCatalogName));
        imageCatalogResponse.setName(imageCatalogName);
        imageCatalogResponse.setDescription(source.getDescription());

        return imageCatalogResponse;
    }

    private boolean isDefault(String imageCatalogName) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        String defaultImageCatalogName = imageCatalogService.getDefaultImageCatalogName(user);
        return imageCatalogName.equals(defaultImageCatalogName) || (defaultImageCatalogName == null && imageCatalogService.isEnvDefault(imageCatalogName));
    }
}
