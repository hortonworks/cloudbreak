package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.User;

import spark.Request;
import spark.Response;

@Component
public class UserModResponse extends AbstractFreeIpaResponse<User> {

    @Override
    public String method() {
        return "user_mod";
    }

    @Override
    protected User handleInternal(Request request, Response response) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        user.setKrbPasswordExpiration("20290101000000Z");
        return user;
    }
}
