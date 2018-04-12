package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.UserProfileResponse;
import com.sequenceiq.cloudbreak.domain.UserProfile;

@Component
public class UserProfileToUserProfileResponseConverter extends AbstractConversionServiceAwareConverter<UserProfile, UserProfileResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileToUserProfileResponseConverter.class);

    @Override
    public UserProfileResponse convert(UserProfile entity) {
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setAccount(entity.getAccount());
        userProfileResponse.setOwner(entity.getOwner());
        if (entity.getCredential() != null) {
            CredentialResponse credentialResponse = getConversionService().convert(entity.getCredential(), CredentialResponse.class);
            userProfileResponse.setCredential(credentialResponse);
        }
        Map<String, Object> map = entity.getUiProperties().getMap();
        userProfileResponse.setUiProperties(map == null ? new HashMap<>() : map);
        return userProfileResponse;
    }
}
