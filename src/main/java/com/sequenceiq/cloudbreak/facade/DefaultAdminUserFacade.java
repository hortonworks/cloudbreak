package com.sequenceiq.cloudbreak.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class DefaultAdminUserFacade implements AdminUserFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdminUserFacade.class);

    @Autowired
    private UserService userService;

    @Override
    public String inviteUser(User admin, String email) {
        LOGGER.debug("Inviting {} ...", email);
        String hash = userService.inviteUser(admin, email);
        LOGGER.debug("Invitation hash {}.", hash);
        return hash;
    }
}
