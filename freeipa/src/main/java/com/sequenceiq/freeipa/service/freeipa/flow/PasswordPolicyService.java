package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;

@Service
public class PasswordPolicyService {

    public static final String MIN_PASSWORD_LIFETIME_HOURS = "krbminpwdlife";

    private static final String MIN_PASSWORD_LENGTH = "krbpwdminlength";

    private static final String MIN_CHARACTER_CLASSES = "krbpwdmindiffchars";

    private static final String MAX_PASSWORD_LIFETIME_DAYS = "krbmaxpwdlife";

    private static final String MAX_FAILURES_BEFORE_LOCK = "krbpwdmaxfailure";

    private static final String FAILURE_RESET_INTERVALL_SECONDS = "krbpwdfailurecountinterval";

    private static final String LOCKOUT_DURATION_SECONDS = "krbpwdlockoutduration";

    @Value("${freeipa.passwordpolicy.min-password-lenght}")
    private Integer minPasswordLength;

    @Value("${freeipa.passwordpolicy.min-character-classes}")
    private Integer minCharacterClasses;

    @Value("${freeipa.passwordpolicy.max-password-life}")
    private Integer maxPasswordLife;

    @Value("${freeipa.passwordpolicy.min-password-life}")
    private Integer minPasswordLife;

    @Value("${freeipa.passwordpolicy.max-failures-before-lock}")
    private Integer maxFailuresBeforeLock;

    @Value("${freeipa.passwordpolicy.failure-reset-interval}")
    private Integer failureResetInterval;

    @Value("${freeipa.passwordpolicy.lockout-duration}")
    private Integer lockoutDuration;

    void updatePasswordPolicy(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        PasswordPolicy config = freeIpaClient.getPasswordPolicy();
        Map<String, Object> params = getParameters(config);
        if (!params.isEmpty()) {
            freeIpaClient.updatePasswordPolicy(params);
        }
    }

    private Map<String, Object> getParameters(PasswordPolicy passwordPolicy) {
        Map<String, Object> params = new HashMap<>();
        if (!minPasswordLength.equals(passwordPolicy.getKrbpwdminlength())) {
            params.put(MIN_PASSWORD_LENGTH, minPasswordLength);
        }
        if (!minCharacterClasses.equals(passwordPolicy.getKrbpwdmindiffchars())) {
            params.put(MIN_CHARACTER_CLASSES, minCharacterClasses);
        }
        if (!minPasswordLife.equals(passwordPolicy.getKrbminpwdlife())) {
            params.put(MIN_PASSWORD_LIFETIME_HOURS, minPasswordLife);
        }
        if (!maxPasswordLife.equals(passwordPolicy.getKrbmaxpwdlife())) {
            params.put(MAX_PASSWORD_LIFETIME_DAYS, maxPasswordLife);
        }
        if (!maxFailuresBeforeLock.equals(passwordPolicy.getKrbpwdmaxfailure())) {
            params.put(MAX_FAILURES_BEFORE_LOCK, maxFailuresBeforeLock);
        }
        if (!failureResetInterval.equals(passwordPolicy.getKrbpwdfailurecountinterval())) {
            params.put(FAILURE_RESET_INTERVALL_SECONDS, failureResetInterval);
        }
        if (!lockoutDuration.equals(passwordPolicy.getKrbpwdlockoutduration())) {
            params.put(LOCKOUT_DURATION_SECONDS, lockoutDuration);
        }
        return params;
    }
}
