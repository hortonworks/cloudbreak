package com.sequenceiq.cloudbreak.auth.security.authentication;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class UmsAuthenticationService implements AuthenticationService {

    private final GrpcUmsClient umsClient;

    public UmsAuthenticationService(GrpcUmsClient umsClient) {
        this.umsClient = umsClient;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(String userCrn) {
        if (userCrn != null) {
            try {
                Crn crn = Crn.safeFromString(userCrn);
                switch (crn.getResourceType()) {
                    case USER:
                        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn)) {
                            return RegionAwareInternalCrnGeneratorUtil.createInternalCrnUser(crn);
                        } else {
                            UserManagementProto.User user = umsClient.getUserDetails(userCrn);
                            return new CloudbreakUser(user.getUserId(), userCrn, user.getEmail(), user.getEmail(), crn.getAccountId());
                        }
                    case MACHINE_USER:
                        UserManagementProto.MachineUser user = umsClient.getMachineUserDetails(userCrn, crn.getAccountId());
                        return new CloudbreakUser(user.getMachineUserId(), userCrn, user.getMachineUserName(), user.getMachineUserName(), crn.getAccountId());
                    default:
                        throw new UmsAuthenticationException(String.format("Authentication is supported only with User and MachineUser CRN: %s", userCrn));
                }
            } catch (CrnParseException e) {
                throw new UmsAuthenticationException(String.format("Invalid CRN has been provided: %s", userCrn));
            }

        }
        return null;
    }
}
