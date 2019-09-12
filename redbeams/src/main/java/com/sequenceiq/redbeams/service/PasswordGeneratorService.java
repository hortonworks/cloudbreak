package com.sequenceiq.redbeams.service;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service
public class PasswordGeneratorService {

    @VisibleForTesting
    static final int AWS_MAX_LENGTH = 30;

    @VisibleForTesting
    static final int AZURE_MAX_LENGTH = 128;

    private static final Pattern LETTERS = Pattern.compile("[A-Za-z]");

    private static final Pattern DIGITS = Pattern.compile("[0-9]");

    @Inject
    private UuidGeneratorService uuidGeneratorService;

    /**
     * Generates a password based on a random UUID. Provider-specific limitations, which are needed
     * for proper admin / root password generation:
     *
     * <ul>
     * <li>AWS: password may be up to 30 characters</li>
     * <li>Azure: password must be 8 - 128 characters, with chars from three of:
     *     uppercase, lowercase, digits, and non-alphanumeric</li>
     * </ul>
     *
     * If no cloud platform is passed, then this method follows arbitrary rules to create a
     * password.
     *
     * @param cloudPlatform cloud provider whose rules must be followed, if known
     * @return random password
     */
    public String generatePassword(Optional<CloudPlatform> cloudPlatform) {
        if (cloudPlatform.isPresent()) {
            switch (cloudPlatform.get()) {
                case AWS:
                    return uuidGeneratorService.uuidVariableParts(AWS_MAX_LENGTH);
                case AZURE:
                    String candidatePassword;
                    do {
                        candidatePassword = StringUtils.left(uuidGeneratorService.randomUuid(), AZURE_MAX_LENGTH);
                    } while (!passesAzureCharacterRules(candidatePassword));
                    return candidatePassword;
                default:
                    throw new UnsupportedOperationException("Password generation for " + cloudPlatform.get()
                        + " not yet implemented");
            }
        } else {
            return uuidGeneratorService.randomUuid();
        }
    }

    @VisibleForTesting
    static boolean passesAzureCharacterRules(String s) {
        // do not check for special characters, because hyphen should be present
        return LETTERS.matcher(s).find() && DIGITS.matcher(s).find();
    }
}
