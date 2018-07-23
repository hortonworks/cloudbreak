package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.UserJson;
import com.sequenceiq.cloudbreak.domain.security.User;

@Component
public class UserToUserJsonConverter extends AbstractConversionServiceAwareConverter<User, UserJson> {
    @Override
    public UserJson convert(User user) {
        UserJson json = new UserJson();
        json.setUsername(user.getName());
        return json;
    }
}
