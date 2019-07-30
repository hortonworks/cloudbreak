package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsState;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient umsClient;

    public UmsState getUmsState(String accountId, String actorCrn) {
        LOGGER.debug("Retrieving UMS state for all users and machineUsers in account {}", accountId);
        UmsState.Builder umsStateBuilder = new UmsState.Builder();
        try {
            umsClient.listAllUsers(actorCrn, accountId, Optional.empty())
                    .forEach(u -> umsStateBuilder.addUser(u, umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty())));

            umsClient.listAllMachineUsers(actorCrn, accountId, Optional.empty())
                    .forEach(u -> umsStateBuilder.addMachineUser(u, umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty())));

            umsClient.listAllGroups(actorCrn, accountId, Optional.empty())
                    .forEach(g -> umsStateBuilder.addGroup(g));
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: %s", e.getMessage()));
        }
        return umsStateBuilder.build();
    }

    public UmsState getUserFilteredUmsState(String accountId, String actorCrn, Collection<String> userCrns, Collection<String> machineUserCrns) {
        LOGGER.debug("Retrieving UMS state for users [{}] and machineUsers [{}] in account {}", userCrns, machineUserCrns, accountId);
        UmsState.Builder umsStateBuilder = new UmsState.Builder();
        try {
            Set<String> groupCrns = new HashSet<>();
            umsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns), Optional.empty())
                    .forEach(u -> {
                        GetRightsResponse rights = umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty());
                        umsStateBuilder.addUser(u, rights);
                        groupCrns.addAll(rights.getGroupCrnList());
                    });

            umsClient.listMachineUsers(actorCrn, accountId, List.copyOf(machineUserCrns), Optional.empty())
                    .forEach(u -> {
                        GetRightsResponse rights = umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty());
                        umsStateBuilder.addMachineUser(u, rights);
                        groupCrns.addAll(rights.getGroupCrnList());
                    });

            umsClient.listGroups(actorCrn, accountId, List.copyOf(groupCrns), Optional.empty())
                    .forEach(g -> umsStateBuilder.addGroup(g));
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: %s", e.getMessage()));
        }
        return umsStateBuilder.build();
    }
}
