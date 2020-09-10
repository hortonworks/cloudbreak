package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
public class AuthorizationRightChecksFactory {
    public List<AuthorizationProto.RightCheck> create(String environmentCrn) {
        requireNonNull(environmentCrn);
        return List.of(
                AuthorizationProto.RightCheck.newBuilder()
                        .setRight(UserSyncConstants.ACCESS_ENVIRONMENT)
                        .setResource(environmentCrn)
                        .build(),
                AuthorizationProto.RightCheck.newBuilder()
                        .setRight(UserSyncConstants.ADMIN_FREEIPA)
                        .setResource(environmentCrn)
                        .build());
    }
}
