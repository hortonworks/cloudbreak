package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.User;

@Component
public class UserFindResponse extends AbstractFreeIpaResponse<Set<User>> {
    @Override
    public String method() {
        return "user_find";
    }

    @Override
    protected Set<User> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        User user = new User();
        user.setDn("admin");
        user.setUid("admin");
        user.setMemberOfGroup(List.of("admins"));
        user.setKrbPasswordExpiration(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        return Set.of(user);
    }
}
