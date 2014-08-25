package com.sequenceiq.cloudbreak.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.InviteConfirmationRequest;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class DefaultUserRegistrationFacade implements UserRegistrationFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUserRegistrationFacade.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserConverter userConverter;

    @Override
    public UserJson registerUser(UserJson userJson) {
        User user = userService.findByEmail(userJson.getEmail());
        if (null == user) {
            LOGGER.debug("Registering new user {}", userJson.getFirstName());
            Account account = accountService.registerAccount(userJson.getCompany());
            user = userService.registerUserInAccount(userConverter.convert(userJson), account);
        } else if (user.getStatus().equals(UserStatus.INVITED)) {
            LOGGER.debug("Registering invited user {}", userJson.getFirstName());
            user = userService.registerInvitedUser(userConverter.convert(userJson));
        } else {
            throw new IllegalStateException(String.format("User registration went wrong. User already registered; id: %s", user.getId()));
        }
        return userConverter.convert(user);
    }

    @Override
    public UserJson confirmInvite(String inviteToken, InviteConfirmationRequest inviteConfirmationRequest) {
        User user = new User();
        user.setPassword(inviteConfirmationRequest.getPassword());
        user.setFirstName(inviteConfirmationRequest.getFirstName());
        user.setLastName(inviteConfirmationRequest.getLastName());
        user = userService.registerUponInvitation(inviteToken, user);
        return userConverter.convert(user);
    }
}
