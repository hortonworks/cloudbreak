package com.sequenceiq.cloudbreak.auth.security.authentication;

import java.util.Optional;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
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
    public CloudbreakUser getCloudbreakUser(OAuth2Authentication auth) {
        String userCrn = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        String principal = (String) auth.getPrincipal();
        return getCloudbreakUser(userCrn, principal);
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
                UserManagementProto.User userInfo = umsClient.getUserDetails(userCrn, userCrn, Optional.ofNullable(requestId));
                String userName = principal != null ? principal : userInfo.getEmail();
                cloudbreakUser = new CloudbreakUser(userInfo.getUserId(), userCrn,
                        userName, userInfo.getEmail(), crn.getAccountId());
                break;
            case MACHINE_USER:
                UserManagementProto.MachineUser machineUserInfo = umsClient.getMachineUserDetails(userCrn, userCrn, Optional.ofNullable(requestId));
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
