package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.domain.User;

public interface UserService {

    Long registerUser(User user);

    String confirmRegistration(String confToken);

    String generatePasswordResetToken(String email);

    String resetPassword(String confToken, String password);

}
