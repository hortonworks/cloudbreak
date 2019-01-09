package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4ShortResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.UserProfile;
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
                        CredentialV4Response credentialResponse = getConversionService().convert(credential, CredentialV4Response.class);
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
        return userProfileV4Response;
    }
}
