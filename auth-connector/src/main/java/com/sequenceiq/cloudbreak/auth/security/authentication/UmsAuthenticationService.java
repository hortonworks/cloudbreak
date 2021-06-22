package com.sequenceiq.cloudbreak.auth.security.authentication;

import java.util.Optional;

import org.springframework.security.core.Authentication;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class UmsAuthenticationService implements AuthenticationService {

    private final GrpcUmsClient umsClient;

    public UmsAuthenticationService(GrpcUmsClient umsClient) {
        this.umsClient = umsClient;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(Authentication auth) {
        if (auth.getPrincipal() instanceof CrnUser) {
            CrnUser crnuser = (CrnUser) auth.getPrincipal();
            return getCloudbreakUser(crnuser.getUserCrn(), null);
        }
        return null;
    }

    public CloudbreakUser getCloudbreakUser(String userCrn, String principal) {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        Crn crn;
        try {
            crn = Crn.safeFromString(userCrn);
        } catch (NullPointerException | CrnParseException e) {
            throw new UmsAuthenticationException(String.format("Invalid CRN has been provided: %s", userCrn));
        }
        CloudbreakUser cloudbreakUser;
        switch (crn.getResourceType()) {
            case USER:
                if (InternalCrnBuilder.isInternalCrn(userCrn)) {
                    return InternalCrnBuilder.createInternalCrnUser(Crn.fromString(userCrn));
                } else {
                    User userInfo = umsClient.getUserDetails(userCrn, Optional.ofNullable(requestId));
                    String userName = principal != null ? principal : userInfo.getEmail();
                    cloudbreakUser = new CloudbreakUser(userInfo.getUserId(), userCrn,
                            userName, userInfo.getEmail(), crn.getAccountId());
                }
                break;
            case MACHINE_USER:
                MachineUser machineUserInfo =
                        umsClient.getMachineUserDetails(userCrn, Crn.fromString(userCrn).getAccountId(), Optional.ofNullable(requestId));
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
