package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory.ADMIN_USER;
import static com.sequenceiq.freeipa.service.freeipa.flow.PasswordPolicyService.MIN_PASSWORD_LIFETIME_HOURS;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.polling.Poller;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaAdminUserReplicatedPoller;

@Service
public class AdminUserService {

    public static final int ADMIN_USER_REPLICATION_WAIT_DURATION_IN_MINUTES = 5;

    public static final int ADMIN_USER_REPLICATION_WAIT_SLEEP_TIME_IN_SECONDS = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);

    @Inject
    private Poller<Void> poller;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void updateAdminUserPassword(String password, FreeIpaClient freeIpaClient) throws RetryableFreeIpaClientException {
        try {
            LOGGER.info("Update password for admin user on FreeIPA");
            Integer minPwdLife = freeIpaClient.getPasswordPolicy().getKrbminpwdlife();
            if (minPwdLife != 0) {
                freeIpaClient.updatePasswordPolicy(Map.of(MIN_PASSWORD_LIFETIME_HOURS, 0L));
            }
            freeIpaClient.userSetPasswordWithExpiration(ADMIN_USER, password, Optional.empty());
        } catch (RetryableFreeIpaClientException retryableFreeIpaClientException) {
            throw retryableFreeIpaClientException;
        } catch (FreeIpaClientException e) {
            if (!FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                LOGGER.error("Failed to update admin password");
                throw new CloudbreakRuntimeException("Failed to update admin password", e);
            }
        }
    }

    public void waitAdminUserPasswordReplication(Stack stack) {
        poller.runPollerDontStopOnException(ADMIN_USER_REPLICATION_WAIT_SLEEP_TIME_IN_SECONDS, ADMIN_USER_REPLICATION_WAIT_DURATION_IN_MINUTES,
                new FreeIpaAdminUserReplicatedPoller(stack, freeIpaClientFactory));
    }
}
