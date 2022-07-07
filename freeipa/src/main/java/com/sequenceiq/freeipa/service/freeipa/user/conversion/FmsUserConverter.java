package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import static com.google.common.base.Preconditions.checkArgument;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;

@Component
public class FmsUserConverter {
    @VisibleForTesting
    static final String NONE_STRING = "None";

    public FmsUser toFmsUser(UserManagementProto.User umsUser) {
        return createFmsUser(umsUser.getWorkloadUsername(),
                umsUser.getFirstName(),
                umsUser.getLastName(),
                umsUser.getState(),
                umsUser.getCrn());
    }

    public FmsUser toFmsUser(UserManagementProto.MachineUser umsMachineUser) {
        // Machine users don't have a first and last name.
        // Store the machine user name and id instead.
        return createFmsUser(umsMachineUser.getWorkloadUsername(),
                umsMachineUser.getMachineUserName(),
                umsMachineUser.getMachineUserId(),
                umsMachineUser.getState(),
                umsMachineUser.getCrn());
    }

    public FmsUser toFmsUser(
            UserManagementProto.UserSyncActorDetails actorDetails) {
        return createFmsUser(actorDetails.getWorkloadUsername(),
                actorDetails.getFirstName(),
                actorDetails.getLastName(),
                actorDetails.getState(),
                actorDetails.getCrn());
    }

    private FmsUser createFmsUser(
            String workloadUsername, String firstName, String lastName,
            UserManagementProto.ActorState.Value actorState,
            String crn) {
        checkArgument(StringUtils.isNotBlank(workloadUsername));
        checkArgument(StringUtils.isNotBlank(crn));
        try {
            Crn.safeFromString(crn);
        } catch (CrnParseException e) {
            throw new IllegalArgumentException(String.format("Crn '%s' is not in the correct format", crn), e);
        }
        FmsUser fmsUser = new FmsUser();
        fmsUser.withName(workloadUsername);
        fmsUser.withFirstName(StringUtils.defaultIfBlank(StringUtils.strip(firstName), NONE_STRING));
        fmsUser.withLastName(StringUtils.defaultIfBlank(StringUtils.strip(lastName), NONE_STRING));
        fmsUser.withState(toFmsUserState(actorState));
        fmsUser.withCrn(crn);
        return fmsUser;
    }

    private FmsUser.State toFmsUserState(UserManagementProto.ActorState.Value actorState) {
        if (null == actorState) {
            return FmsUser.State.ENABLED;
        }
        switch (actorState) {
            case DEACTIVATED:
                return FmsUser.State.DISABLED;
            case ACTIVE:
            case CONTROL_PLANE_LOCKED_OUT:
            case DELETING:
            case UNRECOGNIZED:
            default:
                return FmsUser.State.ENABLED;
        }
    }
}
