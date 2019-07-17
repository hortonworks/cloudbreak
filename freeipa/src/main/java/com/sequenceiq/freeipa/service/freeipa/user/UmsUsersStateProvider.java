package com.sequenceiq.freeipa.service.freeipa.user;

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
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsState;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient umsClient;

    public UmsState getUmsState(String accountId, String actorCrn) {
        UmsState.Builder umsStateBuilder = new UmsState.Builder();
        umsClient.listAllUsers(actorCrn, accountId, Optional.empty())
                .forEach(u -> umsStateBuilder.addUser(u, umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty())));

        umsClient.listAllMachineUsers(actorCrn, accountId, Optional.empty())
                .forEach(u -> umsStateBuilder.addMachineUser(u, umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty())));

        umsClient.listAllGroups(actorCrn, accountId, Optional.empty())
                .forEach(g -> umsStateBuilder.addGroup(g));

        return umsStateBuilder.build();
    }

    public UmsState getUserFilteredUmsState(String accountId, String actorCrn, Set<String> userCrns) {
        // TODO allow filtering on machine users as well once that's exposed in the API
        UmsState.Builder umsStateBuilder = new UmsState.Builder();

        Set<String> groupCrns = new HashSet<>();

        umsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns), Optional.empty())
                .forEach(u -> {
                    GetRightsResponse rights = umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty());
                    umsStateBuilder.addUser(u, rights);
                    groupCrns.addAll(rights.getGroupCrnList());
                });

        umsClient.listGroups(actorCrn, accountId, List.copyOf(groupCrns), Optional.empty())
                .forEach(g -> umsStateBuilder.addGroup(g));

        return umsStateBuilder.build();
    }
}
