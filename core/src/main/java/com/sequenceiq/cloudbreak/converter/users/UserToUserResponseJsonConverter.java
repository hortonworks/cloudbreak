package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Component
public class UserToUserResponseJsonConverter extends AbstractConversionServiceAwareConverter<User, UserV4Response> {

    @Override
    public UserV4Response convert(User user) {
        return new UserV4Response(user.getId(), user.getUserName(), user.getUserId());
    }
}
