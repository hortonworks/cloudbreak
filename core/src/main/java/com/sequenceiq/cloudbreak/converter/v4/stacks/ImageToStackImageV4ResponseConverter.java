package com.sequenceiq.cloudbreak.converter.v4.stacks;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class ImageToStackImageV4ResponseConverter extends AbstractConversionServiceAwareConverter<Image, StackImageV4Response> {

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
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
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            image.setCatalogUrl(imageCatalogService.getImageDefaultCatalogUrl(user));
        } else {
            image.setCatalogUrl(source.getImageCatalogUrl());
        }
    }

}
