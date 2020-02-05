package com.sequenceiq.cloudbreak.converter.v4.stacks.view;


import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.UserViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.UserView;

@Component
public class UserViewToUserViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<UserView, UserViewV4Response> {

    @Override
    public UserViewV4Response convert(UserView source) {
        UserViewV4Response userViewResponse = new UserViewV4Response();
        userViewResponse.setUserId(source.getUserId());
        userViewResponse.setUserName(source.getUserName());
        userViewResponse.setUserCrn(source.getUserCrn());
        return userViewResponse;
    }
}
