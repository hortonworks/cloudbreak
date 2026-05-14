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

public class GrantEntitlementAction implements Action<UmsTestDto, UmsClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrantEntitlementAction.class);

    private static final int WAIT_IN_SECONDS = 90;

    private final String accountId;

    private final String entitlementName;

    private final int waitInSeconds;

    public GrantEntitlementAction(String accountId, String entitlementName) {
        this(accountId, entitlementName, WAIT_IN_SECONDS);
    }

    public GrantEntitlementAction(String accountId, String entitlementName, int waitInSeconds) {
        this.accountId = accountId;
        this.entitlementName = entitlementName;
        this.waitInSeconds = waitInSeconds;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        Log.when(LOGGER, format(" Grant UMS entitlement for account '%s'. ", accountId));
        try {
            client.getDefaultClient(testContext).grantEntitlement(accountId, entitlementName);
            //This is necessary because the ttl on the ums account caching
            Thread.sleep(Duration.of(waitInSeconds, ChronoUnit.SECONDS));
            Log.when(LOGGER, format(" UMS entitlement has been granted for account '%s'. ", accountId));
        } catch (StatusRuntimeException e) {
            Log.when(LOGGER, format(" Exception during calling UMS mock: '%s' ", e.getMessage()));
            throw new TestFailException("Exception during calling UMS mock.", e);
        }
        return testDto;
    }
}
