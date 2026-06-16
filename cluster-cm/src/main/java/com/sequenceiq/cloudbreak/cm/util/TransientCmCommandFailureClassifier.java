package com.sequenceiq.cloudbreak.cm.util;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class TransientCmCommandFailureClassifier {

    // Known FreeIPA 389-ds error fragments surfaced (via the failed GenerateCredentials sub-command result) when concurrent
    // service-add calls race. These are recoverable on a retry. See CB-33368 / OPSAPS-78167.
    private static final List<String> TRANSIENT_CREDENTIAL_GENERATION_FAILURE_FRAGMENTS = List.of(
            "error checking for attribute uniqueness",
            "databaseerror");

    public boolean isTransientCredentialGenerationFailure(String failureMessage) {
        if (StringUtils.isBlank(failureMessage)) {
            return false;
        }
        String normalizedMessage = failureMessage.toLowerCase(Locale.ROOT);
        return TRANSIENT_CREDENTIAL_GENERATION_FAILURE_FRAGMENTS.stream().anyMatch(normalizedMessage::contains);
    }
}
