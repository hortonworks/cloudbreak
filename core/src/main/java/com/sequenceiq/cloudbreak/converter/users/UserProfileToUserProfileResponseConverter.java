package com.sequenceiq.cloudbreak.converter.users;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4ShortResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class UserProfileToUserProfileResponseConverter extends AbstractConversionServiceAwareConverter<UserProfile, UserProfileResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileToUserProfileResponseConverter.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public UserProfileResponse convert(UserProfile entity) {
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setUsername(entity.getUserName());
        userProfileResponse.setUserId(entity.getUser().getUserId());
        userProfileResponse.setTenant(entity.getUser().getTenant().getName());
        if (!entity.getDefaultCredentials().isEmpty()) {
            entity.getDefaultCredentials()
                    .stream()
                    .filter(defaultCredential -> defaultCredential.getWorkspace().getId().
                            equals(restRequestThreadLocalService.getRequestedWorkspaceId()))
                    .limit(1)
                    .forEach(credential -> {
                        CredentialResponse credentialResponse = getConversionService().convert(credential, CredentialResponse.class);
                        userProfileResponse.setCredential(credentialResponse);
                    });
        }
        if (entity.getImageCatalog() != null) {
            userProfileResponse.setImageCatalog(getConversionService()
                    .convert(entity.getImageCatalog(), ImageCatalogV4ShortResponse.class));
        } else {
            userProfileResponse.setImageCatalog(getConversionService()
                    .convert(imageCatalogService.getCloudbreakDefaultImageCatalog(), ImageCatalogV4ShortResponse.class));
        }
        Json propertiesFromVault = new Json(entity.getUiProperties());
        Map<String, Object> map = propertiesFromVault.getMap();
        userProfileResponse.setUiProperties(map == null ? new HashMap<>() : map);
        return userProfileResponse;
    }
}
