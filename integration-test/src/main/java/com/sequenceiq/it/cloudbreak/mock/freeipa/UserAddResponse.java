package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.User;

import spark.Request;
import spark.Response;

@Component
public class UserAddResponse extends AbstractFreeIpaResponse<User> {
    @Override
    public String method() {
        return "user_add";
    }

    @Override
    protected User handleInternal(Request request, Response response) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        return user;
    }
}
