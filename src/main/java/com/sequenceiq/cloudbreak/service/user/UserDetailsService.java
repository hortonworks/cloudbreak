package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.domain.CbUser;

public interface UserDetailsService {
    CbUser getDetails(String username);
}
