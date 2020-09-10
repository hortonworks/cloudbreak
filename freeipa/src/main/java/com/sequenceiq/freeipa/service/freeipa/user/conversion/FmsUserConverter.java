package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;

@Component
public class FmsUserConverter {
    @VisibleForTesting
    static final String NONE_STRING = "None";

    public FmsUser toFmsUser(UserManagementProto.User umsUser) {
        return createFmsUser(umsUser.getWorkloadUsername(),
                umsUser.getFirstName(),
                umsUser.getLastName());
    }

    public FmsUser toFmsUser(UserManagementProto.MachineUser umsMachineUser) {
        // Machine users don't have a first and last name.
        // Store the machine user name and id instead.
        return createFmsUser(umsMachineUser.getWorkloadUsername(),
                umsMachineUser.getMachineUserName(),
                umsMachineUser.getMachineUserId());
    }

    public FmsUser toFmsUser(
            UserManagementProto.UserSyncActorDetails actorDetails) {
        return createFmsUser(actorDetails.getWorkloadUsername(),
                actorDetails.getFirstName(),
                actorDetails.getLastName());
    }

    private FmsUser createFmsUser(String workloadUsername, String firstName, String lastName) {
        checkArgument(!Strings.isNullOrEmpty(workloadUsername));
        FmsUser fmsUser = new FmsUser();
        fmsUser.withName(workloadUsername);
        fmsUser.withFirstName(StringUtils.defaultIfBlank(firstName, NONE_STRING));
        fmsUser.withLastName(StringUtils.defaultIfBlank(lastName, NONE_STRING));
        return fmsUser;
    }
}
