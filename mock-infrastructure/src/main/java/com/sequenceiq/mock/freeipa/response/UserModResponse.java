package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.User;

@Component
public class UserModResponse extends AbstractFreeIpaResponse<User> {

    @Override
    public String method() {
        return "user_mod";
    }

    @Override
    protected User handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        user.setKrbPasswordExpiration("20290101000000Z");
        return user;
    }
}
