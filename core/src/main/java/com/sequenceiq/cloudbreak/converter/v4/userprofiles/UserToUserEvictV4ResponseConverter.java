package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserEvictV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Component
public class UserToUserEvictV4ResponseConverter extends AbstractConversionServiceAwareConverter<User, UserEvictV4Response> {

    @Override
    public UserEvictV4Response convert(User user) {
        return new UserEvictV4Response(user.getUserName());
    }
}
