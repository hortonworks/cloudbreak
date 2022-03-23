package com.sequenceiq.cloudbreak.auth.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.auth.security.authentication.UmsAuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class CrnUserDetailsService implements UserDetailsService {

    private final UmsAuthenticationService umsAuthenticationService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CrnUserDetailsService(GrpcUmsClient umsClient, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        umsAuthenticationService = new UmsAuthenticationService(umsClient, regionAwareInternalCrnGeneratorFactory);
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    public CrnUser loadUserByUsername(String crn) throws UsernameNotFoundException {
        return getUmsUser(crn);
    }

    private CrnUser getUmsUser(String crnText) {
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(crnText)) {
            return RegionAwareInternalCrnGeneratorUtil.createInternalCrnUser(Crn.fromString(crnText));
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
