package com.sequenceiq.environment.environment.validation.validators;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class PublicKeyValidator {

    private static final List<String> SUPPORTED_ALGORITHMS
            = List.of("ssh-rsa", "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384", "ecdsa-sha2-nistp521", "ssh-ed25519", "ssh-dss");

    private static final String ALGORITHM_TEXT_LIST = String.join(", ", SUPPORTED_ALGORITHMS);

    private static final Pattern SSH_PUBLIC_KEY_PATTERN = Pattern.compile("^(ssh-rsa AAAAB3NzaC1yc2|ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNT|"
            + "ecdsa-sha2-nistp384 AAAAE2VjZHNhLXNoYTItbmlzdHAzODQAAAAIbmlzdHAzOD|ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1Mj|"
            + "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5|ssh-dss AAAAB3NzaC1kc3)[0-9A-Za-z+\\/]+[=]{0,3}( .*)?$");

    public ValidationResult validatePublicKey(String publicKey) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (StringUtils.isNotEmpty(publicKey)) {
            Matcher matcher = SSH_PUBLIC_KEY_PATTERN.matcher(publicKey.trim());
            if (!matcher.matches()) {
                validationResultBuilder.error(String.format(
                        "The uploaded SSH Public Key is invalid. Correct format is <algorithm> <key> <comment> where <algorithm> can be one of"
                                + " %n'%s' and the <comment> is optional.", ALGORITHM_TEXT_LIST));
            }
        }
        return validationResultBuilder.build();
    }
}
