package com.sequenceiq.cloudbreak.facade;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.controller.json.UserUpdateRequest;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;

public interface AdminUserFacade extends CloudBreakFacade {

    String inviteUser(User admin, String email);

    String inviteAdmin(User admin, String email);

    UserJson activateUser(Long userId);

    UserJson deactivateUser(Long usertId);

    UserJson putUserInRoles(Long userId, Set<UserRole> roles);

    List<UserJson> accountUsers(User admin);

    UserJson updateUser(User admin, Long userId, UserUpdateRequest updateRequest);
}
