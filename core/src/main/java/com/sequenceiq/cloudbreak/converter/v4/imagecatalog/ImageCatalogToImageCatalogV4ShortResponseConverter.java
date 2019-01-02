package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4ShortResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class ImageCatalogToImageCatalogV4ShortResponseConverter extends AbstractConversionServiceAwareConverter<ImageCatalog, ImageCatalogV4ShortResponse> {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ImageCatalogV4ShortResponse convert(ImageCatalog source) {
        ImageCatalogV4ShortResponse imageCatalogResponse = new ImageCatalogV4ShortResponse();
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
