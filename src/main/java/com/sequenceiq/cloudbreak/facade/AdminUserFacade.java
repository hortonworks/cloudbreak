package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.InviteRequest;
import com.sequenceiq.cloudbreak.controller.json.UpdateRequest;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.User;

public interface AdminUserFacade extends CloudBreakFacade {

    String inviteUser(User admin, InviteRequest inviteRequest);

    List<UserJson> accountUsers(User admin);

    UserJson updateUser(User admin, Long userId, UpdateRequest updateRequest);
}
