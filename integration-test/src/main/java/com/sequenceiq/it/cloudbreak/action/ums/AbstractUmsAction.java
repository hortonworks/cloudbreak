package com.sequenceiq.it.cloudbreak.action.ums;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public abstract class AbstractUmsAction<U extends UmsTestDto> implements Action<U, UmsClient> {

    @Override
    public U action(TestContext testContext, U testDto, UmsClient client) throws Exception {
        Exception umsActionException = null;
        int retries = 0;
        int maxRetry = 10;
        long pollingInterval = 7000;
        while (retries <= maxRetry) {
            try {
                return umsAction(testContext, testDto, client);
            } catch (Exception e) {
                umsActionException = e;
                Thread.sleep(pollingInterval);
                retries++;
            }
        }
        if (umsActionException != null) {
            throw new TestFailException(String.format("EXCEEDING UMS ACTION MAX. RETRY (%s) ::: UMS action has been failed, because of: %s%n", retries,
                    umsActionException.getMessage()), umsActionException);
        } else {
            throw new TestFailException(String.format("EXCEEDING UMS ACTION MAX. RETRY (%s) ::: UMS action has been failed!", retries));
        }
    }

    protected abstract U umsAction(TestContext testContext, U testDto, UmsClient client) throws Exception;
}
