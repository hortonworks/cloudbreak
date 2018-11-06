package com.sequenceiq.cloudbreak.converter;


import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.UserViewResponse;
import com.sequenceiq.cloudbreak.domain.view.UserView;

@Component
public class UserViewToUserViewResponseConverter extends AbstractConversionServiceAwareConverter<UserView, UserViewResponse> {

    @Override
    public UserViewResponse convert(UserView source) {
        UserViewResponse userViewResponse = new UserViewResponse();
        userViewResponse.setUserId(source.getUserId());
        userViewResponse.setUserName(source.getUserName());
        return userViewResponse;
    }
}
