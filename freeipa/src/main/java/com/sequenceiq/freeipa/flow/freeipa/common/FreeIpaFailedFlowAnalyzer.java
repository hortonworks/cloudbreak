package com.sequenceiq.freeipa.flow.freeipa.common;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Locale;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class FreeIpaFailedFlowAnalyzer {

    @Inject
    private FreeIpaValidationProperties validationFailedMessages;

    public boolean isValidationFailedError(FreeIpaFailureEvent payload) {
        return isValidationFailedError(payload.getFailureType(), payload.getException());
    }

    private boolean isValidationFailedError(FailureType failureType, Exception exception) {
        return VALIDATION.equals(failureType) || isValidationFailedError(exception);
    }

    private boolean isValidationFailedError(Exception exceptionMessage) {
        if (exceptionMessage == null || exceptionMessage.getMessage() == null || validationFailedMessages.getFailedMessages() == null) {
            return false;
        }
        return validationFailedMessages
                .getFailedMessages()
                .stream()
                .anyMatch(filterIfContains(exceptionMessage));
    }

    private Predicate<String> filterIfContains(Exception exceptionMessage) {
        return message -> {
            if (exceptionMessage.getMessage().toLowerCase(Locale.ROOT).contains(message.toLowerCase(Locale.ROOT))) {
                return true;
            }
            return false;
        };
    }
}
