package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.User;

import spark.Request;
import spark.Response;

@Component
public class UserFindResponse extends AbstractFreeIpaResponse<Set<User>> {
    @Override
    public String method() {
        return "user_find";
    }

    @Override
    protected Set<User> handleInternal(Request request, Response response) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        user.setKrbPasswordExpiration(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        return Set.of(user);
    }
}
