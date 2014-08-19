package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserStatus;

public interface UserService {

    String confirmRegistration(String confToken);

    String generatePasswordResetToken(String email);

    String resetPassword(String confToken, String password);

    Long registerUserInAccount(User user, Account account);

    String inviteUser(User admin, String email);

    User invitedUser(String hash);

    User setUserStatus(Long userId, UserStatus userStatus);

    Long registerInvitedUser(User user, Account account);

}
