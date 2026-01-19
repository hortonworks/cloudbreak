package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

import io.grpc.StatusRuntimeException;

public class RevokeEntitlementAction implements Action<UmsTestDto, UmsClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevokeEntitlementAction.class);

    private static final int WAIT_IN_SECONDS = 90;

    private final String accountId;

    private final String entitlementName;

    public RevokeEntitlementAction(String accountId, String entitlementName) {
        this.accountId = accountId;
        this.entitlementName = entitlementName;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        Log.when(LOGGER, format(" Revoke UMS entitlement for account '%s'. ", accountId));
        try {
            client.getDefaultClient(testContext).revokeEntitlement(accountId, entitlementName);
            //This is necessary because the ttl on the ums account caching
            Thread.sleep(Duration.of(WAIT_IN_SECONDS, ChronoUnit.SECONDS));
            Log.when(LOGGER, format(" UMS entitlement has been revoked for account '%s'. ", accountId));
        } catch (StatusRuntimeException e) {
            Log.when(LOGGER, format(" Exception during calling UMS mock: '%s' ", e.getMessage()));
            throw new TestFailException("Exception during calling UMS mock.", e);
        }
        return testDto;
    }
}
