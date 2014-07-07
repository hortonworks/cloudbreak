package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.domain.User;

public interface UserService {

    Long registerUser(User user);

    void confirmRegistration(String confToken);

    void disableUser(String email);

    void resetPassword(String confToken, String password);

    boolean validateResetPassword(String confToken);
}
