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
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class UserProfileToUserProfileResponseConverter extends AbstractConversionServiceAwareConverter<UserProfile, UserProfileResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileToUserProfileResponseConverter.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private VaultService vaultService;

    @Override
    public UserProfileResponse convert(UserProfile entity) {
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setAccount(entity.getAccount());
        userProfileResponse.setOwner(entity.getOwner());
        userProfileResponse.setUsername(entity.getUserName());
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
                    .convert(entity.getImageCatalog(), ImageCatalogShortResponse.class));
        } else {
            userProfileResponse.setImageCatalog(getConversionService()
                    .convert(imageCatalogService.getCloudbreakDefaultImageCatalog(), ImageCatalogShortResponse.class));
        }
        Json propertiesFromVault = new Json(vaultService.resolveSingleValue(entity.getUiProperties()));
        Map<String, Object> map = propertiesFromVault.getMap();
        userProfileResponse.setUiProperties(map == null ? new HashMap<>() : map);
        return userProfileResponse;
    }
}
