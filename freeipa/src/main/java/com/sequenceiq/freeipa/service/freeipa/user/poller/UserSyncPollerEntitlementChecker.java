package com.sequenceiq.freeipa.service.freeipa.user.poller;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;

@Component
class UserSyncPollerEntitlementChecker {
    @VisibleForTesting
    static final String INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Inject
    private EntitlementService entitlementService;

    public boolean isAccountEntitled(String accountId) {
        return entitlementService.automaticUsersyncPollerEnabled(INTERNAL_ACTOR_CRN, accountId);
    }
}
