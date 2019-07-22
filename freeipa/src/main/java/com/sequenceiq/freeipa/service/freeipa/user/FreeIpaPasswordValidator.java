package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.controller.exception.BadRequestException;

@Component
class FreeIpaPasswordValidator {

    private static final Set<String> PATTERNS = Set.of("(?=.*[a-z]).*$", "(?=.*[A-Z]).*$", "(?=.*[0-9]).*$", ".*\\W+.*");

    @Value("${freeipa.passwordpolicy.min-password-lenght}")
    private Integer minPasswordLength;

    @Value("${freeipa.passwordpolicy.max-password-lenght}")
    private Integer maxPasswordLength;

    @Value("${freeipa.passwordpolicy.min-character-classes}")
    private Integer minCharacterClasses;

    void validate(String password) {
        validateLength(password);
        validateComplexity(password);
    }

    private void validateLength(String password) {
        if (password.length() < minPasswordLength || password.length() > maxPasswordLength) {
            throw new BadRequestException(
                    String.format("Password must be between minimum %d and maximum %d characters.", minPasswordLength, maxPasswordLength));
        }
    }

    private void validateComplexity(String password) {
        long matches = PATTERNS.stream().filter(password::matches).count();
        if (matches < minCharacterClasses) {
            throw new BadRequestException(String.format(
                    "There must be %d different classes. The classes are: Upper-case characters, Lower-case characters, Digits and Special characters.",
                    minCharacterClasses));
        }
    }

}
