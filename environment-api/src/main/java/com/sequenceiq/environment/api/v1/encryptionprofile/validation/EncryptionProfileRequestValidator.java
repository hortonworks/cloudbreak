package com.sequenceiq.environment.api.v1.encryptionprofile.validation;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.environment.api.v1.encryptionprofile.config.EncryptionProfileConfig;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;

public class EncryptionProfileRequestValidator implements ConstraintValidator<ValidEncryptionProfileRequest, EncryptionProfileRequest> {

    @Autowired
    private EncryptionProfileConfig encryptionProfileConfig;

    @Override
    public boolean isValid(EncryptionProfileRequest request, ConstraintValidatorContext context) {
        Set<TlsVersion> tlsVersions = request.getTlsVersions();

        // Validate mandatory tlsVersions
        if (CollectionUtils.isEmpty(tlsVersions)) {
            ValidatorUtil.addConstraintViolation(context,
                    "tlsVersions is a mandatory field and must not be empty.");
            return false;
        }

        if (CollectionUtils.isEmpty(request.getCipherSuites())) {
            ValidatorUtil.addConstraintViolation(context,
                    "cipherSuites is a mandatory field and must not be empty.");
            return false;
        }

        // Validate them against available ciphers for the specified TLS versions
            Set<String> availableCipherSuites = encryptionProfileConfig.getAvailableCipherSet(tlsVersions);
            Set<String> unsupportedCiphers = request.getCipherSuites()
                    .stream()
                    .filter(cipher -> !availableCipherSuites.contains(cipher))
                    .collect(Collectors.toSet());

            if (!unsupportedCiphers.isEmpty()) {
                String message = "Unsupported cipher suite(s) for the given TLS versions: " +
                        String.join(", ", unsupportedCiphers);
                ValidatorUtil.addConstraintViolation(context, message);
                return false;
            }

        return true;
    }

    public void setEncryptionProfileConfig(EncryptionProfileConfig encryptionProfileConfig) {
        this.encryptionProfileConfig = encryptionProfileConfig;
    }
}
