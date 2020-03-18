package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
class UserSyncPollerEntitlementChecker {

    @Inject
    private EntitlementService entitlementService;

    public boolean isAccountEntitled(String accountId) {
        return entitlementService.automaticUsersyncPollerEnabled(INTERNAL_ACTOR_CRN, accountId);
    }
}
