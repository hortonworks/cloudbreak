package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.User;

@Component
public class UserAddResponse extends AbstractFreeIpaResponse<User> {
    @Override
    public String method() {
        return "user_add";
    }

    @Override
    protected User handleInternal(String body) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        user.setKrbPasswordExpiration("20290101000000Z");
        return user;
    }
}
