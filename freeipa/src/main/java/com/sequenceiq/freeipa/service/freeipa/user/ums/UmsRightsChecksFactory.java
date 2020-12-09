package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;

@Component
public class UmsRightsChecksFactory {

    public List<UserManagementProto.RightsCheck> get(List<String> environmentCrnsList) {
        return environmentCrnsList.stream()
                .map(crn -> UserManagementProto.RightsCheck.newBuilder()
                        .setResourceCrn(crn)
                        .addAllRight(UserSyncConstants.RIGHTS)
                        .build())
                .collect(Collectors.toList());
    }
}
