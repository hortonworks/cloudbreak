package com.sequenceiq.cloudbreak.facade;

import com.sequenceiq.cloudbreak.domain.User;

public interface AdminUserFacade extends CloudBreakFacade {

    String inviteUser(User admin, String email);
}
