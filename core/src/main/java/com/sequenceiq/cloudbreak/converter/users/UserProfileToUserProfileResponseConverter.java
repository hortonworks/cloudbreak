package com.sequenceiq.cloudbreak.converter.users;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogShortResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class UserProfileToUserProfileResponseConverter extends AbstractConversionServiceAwareConverter<UserProfile, UserProfileResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileToUserProfileResponseConverter.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Override
    public UserProfileResponse convert(UserProfile entity) {
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setAccount(entity.getAccount());
        userProfileResponse.setOwner(entity.getOwner());
        if (entity.getCredential() != null) {
            CredentialResponse credentialResponse = getConversionService().convert(entity.getCredential(), CredentialResponse.class);
            userProfileResponse.setCredential(credentialResponse);
        }
        if (entity.getImageCatalog() != null) {
            userProfileResponse.setImageCatalog(getConversionService()
                    .convert(entity.getImageCatalog(), ImageCatalogShortResponse.class));
        } else {
            userProfileResponse.setImageCatalog(getConversionService()
                    .convert(imageCatalogService.getCloudbreakDefaultImageCatalog(), ImageCatalogShortResponse.class));
        }
        Map<String, Object> map = entity.getUiProperties().getMap();
        userProfileResponse.setUiProperties(map == null ? new HashMap<>() : map);
        return userProfileResponse;
    }
}
