package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.User;

public interface AdminUserFacade extends CloudBreakFacade {

    String inviteUser(User admin, String email);

    UserJson activateUser(Long userId);

    UserJson deactivateUser(Long usertId);

    List<UserJson> accountUsers(User admin);
}
