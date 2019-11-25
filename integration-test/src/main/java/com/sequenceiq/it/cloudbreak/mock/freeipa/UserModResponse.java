package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.User;

import spark.Request;
import spark.Response;

@Component
public class UserModResponse extends AbstractFreeIpaResponse<Set<User>> {
    @Override
    public String method() {
        return "user_mod";
    }

    @Override
    protected Set<User> handleInternal(Request request, Response response) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        user.setKrbPasswordExpiration("20290101000000Z");
        return Set.of(user);
    }
}
