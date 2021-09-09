package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;

@Component
public class UserNamePasswordV4RequestToUpdateClusterV4RequestConverter {

    public UpdateClusterV4Request convert(UserNamePasswordV4Request source) {
        UpdateClusterV4Request updateStackJson = new UpdateClusterV4Request();
        updateStackJson.setUserNamePassword(source);
        return updateStackJson;
    }

}
