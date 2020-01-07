package com.sequenceiq.cloudbreak.auth.altus;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
public class EntitlementService {

    @Inject
    private GrpcUmsClient umsClient;

    public boolean ccmEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, "CDP_REVERSE_SSH_TUNNEL");
    }

    public boolean azureEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, "CDP_AZURE");
    }

    public boolean baseImageEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, "CDP_BASE_IMAGE");
    }

    public boolean automaticUsersyncPollerEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, "CDP_AUTOMATIC_USERSYNC_POLLER");
    }

    public boolean fmsClusterProxyEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, "CDP_FMS_CLUSTER_PROXY");
    }

    public List<String> getEntitlements(String actorCrn, String accountId) {
        return getAccount(actorCrn, accountId).getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .collect(Collectors.toList());
    }

    private UserManagementProto.Account getAccount(String actorCrn, String accountId) {
        return umsClient.getAccountDetails(actorCrn, accountId, MDCUtils.getRequestId());
    }

    private boolean isEntitlementRegistered(String actorCrn, String accountId, String entitlement) {
        return getEntitlements(actorCrn, accountId)
                .stream()
                .anyMatch(e -> e.equalsIgnoreCase(entitlement));
    }

}
