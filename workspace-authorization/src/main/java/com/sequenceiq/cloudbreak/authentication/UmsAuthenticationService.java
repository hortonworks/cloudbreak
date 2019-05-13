package com.sequenceiq.cloudbreak.authentication;

import java.util.Optional;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
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
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        UserManagementProto.User userInfo = umsClient.getUserDetails(userCrn, userCrn, Optional.ofNullable(requestId));
        return new CloudbreakUser(userInfo.getUserId(), userCrn,
                (String) auth.getPrincipal(), userInfo.getEmail(), Crn.fromString(userCrn).getAccountId());
    }
}
