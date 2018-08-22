package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.organization.User;
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

        return imageCatalogResponse;
    }

    private boolean isDefault(String imageCatalogName) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        String defaultImageCatalogName = imageCatalogService.getDefaultImageCatalogName(identityUser, user);
        return imageCatalogName.equals(defaultImageCatalogName) || (defaultImageCatalogName == null && imageCatalogService.isEnvDefault(imageCatalogName));
    }
}
