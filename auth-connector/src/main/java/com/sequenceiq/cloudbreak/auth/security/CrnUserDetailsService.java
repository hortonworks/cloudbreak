package com.sequenceiq.cloudbreak.auth.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.auth.security.authentication.UmsAuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class CrnUserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnUserDetailsService.class);

    private final UmsAuthenticationService umsAuthenticationService;

    public CrnUserDetailsService(GrpcUmsClient umsClient) {
        umsAuthenticationService = new UmsAuthenticationService(umsClient);
    }

    public CrnUser getUmsUser(String crnText) {
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(crnText)) {
            return RegionAwareInternalCrnGeneratorUtil.createInternalCrnUser(Crn.fromString(crnText));
        }
        CloudbreakUser cloudbreakUser = umsAuthenticationService.getCloudbreakUser(crnText);
        return new CrnUser(cloudbreakUser.getUserId(),
                cloudbreakUser.getUserCrn(),
                cloudbreakUser.getEmail(),
                cloudbreakUser.getEmail(),
                cloudbreakUser.getTenant(),
                "CRN_USER");
    }

}
