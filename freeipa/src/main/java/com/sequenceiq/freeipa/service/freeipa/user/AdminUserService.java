package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory.ADMIN_USER;
import static com.sequenceiq.freeipa.service.freeipa.flow.PasswordPolicyService.MIN_PASSWORD_LIFETIME_HOURS;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;

@Service
public class AdminUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);

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
}
