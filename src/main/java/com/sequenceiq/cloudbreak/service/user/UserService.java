package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;

public interface UserService {

    String confirmRegistration(String confToken);

    String generatePasswordResetToken(String email);

    String resetPassword(String confToken, String password);

    Long registerUserInAccount(User user, Account account);

}
