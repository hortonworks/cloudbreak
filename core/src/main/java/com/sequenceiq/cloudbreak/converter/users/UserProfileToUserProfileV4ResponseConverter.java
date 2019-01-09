package com.sequenceiq.cloudbreak.converter.users;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.base.UIPropertiesV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4ShortResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class UserProfileToUserProfileV4ResponseConverter extends AbstractConversionServiceAwareConverter<UserProfile, UserProfileV4Response> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileToUserProfileV4ResponseConverter.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public UserProfileV4Response convert(UserProfile entity) {
        UserProfileV4Response userProfileV4Response = new UserProfileV4Response();
        userProfileV4Response.setUsername(entity.getUserName());
        userProfileV4Response.setUserId(entity.getUser().getUserId());
        userProfileV4Response.setTenant(entity.getUser().getTenant().getName());
        if (!entity.getDefaultCredentials().isEmpty()) {
            entity.getDefaultCredentials()
                    .stream()
                    .filter(defaultCredential -> defaultCredential.getWorkspace().getId().
                            equals(restRequestThreadLocalService.getRequestedWorkspaceId()))
                    .limit(1)
                    .forEach(credential -> {
                        CredentialResponse credentialResponse = getConversionService().convert(credential, CredentialResponse.class);
                        userProfileV4Response.setCredential(credentialResponse);
                    });
        }
        if (entity.getImageCatalog() != null) {
            userProfileV4Response.setImageCatalog(getConversionService()
                    .convert(entity.getImageCatalog(), ImageCatalogV4ShortResponse.class));
        } else {
            userProfileV4Response.setImageCatalog(getConversionService()
                    .convert(imageCatalogService.getCloudbreakDefaultImageCatalog(), ImageCatalogV4ShortResponse.class));
        }
        Json propertiesFromVault = new Json(entity.getUiProperties());
        Map<String, Object> map = propertiesFromVault.getMap();
        userProfileV4Response.setUiProperties(Sets.newHashSet());
        if (map != null) {
            userProfileV4Response.setUiProperties(map
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        UIPropertiesV4Base uiProperty = new UIPropertiesV4Base();
                        uiProperty.setKey(entry.getKey());
                        uiProperty.setObject(entry.getValue());
                        return uiProperty;
                    })
                    .collect(Collectors.toSet()));
        }
        return userProfileV4Response;
    }
}
