package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.UserJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Component
public class UserToUserJsonConverter extends AbstractConversionServiceAwareConverter<User, UserJson> {

    @Override
    public UserJson convert(User user) {
        return new UserJson(user.getUserName());
    }
}
