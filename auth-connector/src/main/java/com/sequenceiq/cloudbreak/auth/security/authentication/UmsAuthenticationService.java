package com.sequenceiq.cloudbreak.auth.security.authentication;

import org.springframework.security.core.Authentication;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class UmsAuthenticationService implements AuthenticationService {

    private final GrpcUmsClient umsClient;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public UmsAuthenticationService(GrpcUmsClient umsClient,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.umsClient = umsClient;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(Authentication auth) {
        if (auth.getPrincipal() instanceof CrnUser) {
            CrnUser crnuser = (CrnUser) auth.getPrincipal();
            return getCloudbreakUser(crnuser.getUserCrn(), null);
        }
        return null;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(String userCrn, String principal) {
        Crn crn;
        try {
            crn = Crn.safeFromString(userCrn);
        } catch (NullPointerException | CrnParseException e) {
            throw new UmsAuthenticationException(String.format("Invalid CRN has been provided: %s", userCrn));
        }
        CloudbreakUser cloudbreakUser;
        switch (crn.getResourceType()) {
            case USER:
                if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn)) {
                    return RegionAwareInternalCrnGeneratorUtil.createInternalCrnUser(Crn.fromString(userCrn));
                } else {
                    User userInfo = umsClient.getUserDetails(userCrn, regionAwareInternalCrnGeneratorFactory);
                    String userName = principal != null ? principal : userInfo.getEmail();
                    cloudbreakUser = new CloudbreakUser(userInfo.getUserId(), userCrn,
                            userName, userInfo.getEmail(), crn.getAccountId());
                }
                break;
            case MACHINE_USER:
                MachineUser machineUserInfo =
                        umsClient.getMachineUserDetails(userCrn, Crn.fromString(userCrn).getAccountId(), regionAwareInternalCrnGeneratorFactory);
                String machineUserName = principal != null ? principal : machineUserInfo.getMachineUserName();
                cloudbreakUser = new CloudbreakUser(machineUserInfo.getMachineUserId(), userCrn,
                        machineUserName, machineUserInfo.getMachineUserName(), crn.getAccountId());
                break;
            default:
                throw new UmsAuthenticationException(String.format("Authentication is supported only with User and MachineUser CRN: %s", userCrn));
        }
        return cloudbreakUser;
    }
}
