package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.security.User;

@Component
public class UserToUserResponseJsonConverter extends AbstractConversionServiceAwareConverter<User, UserResponseJson> {

    @Override
    public UserResponseJson convert(User user) {
        return new UserResponseJson(user.getId(), user.getUserName(), user.getUserId());
    }
}
