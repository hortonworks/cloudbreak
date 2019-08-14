package com.sequenceiq.cloudbreak.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.authentication.UmsAuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class CrnUserDetailsService implements UserDetailsService {

    private final UmsAuthenticationService umsAuthenticationService;

    public CrnUserDetailsService(GrpcUmsClient umsClient) {
        umsAuthenticationService = new UmsAuthenticationService(umsClient);
    }

    @Override
    public UserDetails loadUserByUsername(String crn) throws UsernameNotFoundException {
        return getUmsUser(crn);
    }

    private UserDetails getUmsUser(String crnText) {
        if (InternalCrnBuilder.isInternalCrn(crnText)) {
            return InternalCrnBuilder.createInternalCrnUser(Crn.fromString(crnText));
        }
        CloudbreakUser cloudbreakUser = umsAuthenticationService.getCloudbreakUser(crnText, null);
        return new CrnUser(cloudbreakUser.getUserId(),
                cloudbreakUser.getUserCrn(),
                cloudbreakUser.getEmail(),
                cloudbreakUser.getEmail(),
                cloudbreakUser.getTenant(),
                "CRN_USER");
    }

}
