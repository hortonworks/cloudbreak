package com.sequenceiq.environment.api.v1.encryptionprofile.validation;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
        Set<String> cipherSuites = request.getCipherSuites();

        // Validate mandatory tlsVersions
        if (tlsVersions == null || tlsVersions.isEmpty()) {
            ValidatorUtil.addConstraintViolation(context,
                    "tlsVersions is a mandatory field and must not be empty.");
            return false;
        }

        // If cipherSuites are provided, validate them against available ciphers for the specified TLS versions
        if (cipherSuites != null && !cipherSuites.isEmpty()) {
            Set<String> availableCipherSuites = encryptionProfileConfig.getAvailableCipherSet(tlsVersions);
            Set<String> unsupportedCiphers = cipherSuites.stream()
                    .filter(cipher -> !availableCipherSuites.contains(cipher))
                    .collect(Collectors.toSet());

            if (!unsupportedCiphers.isEmpty()) {
                String message = "Unsupported cipher suite(s) for the given TLS versions: " +
                        String.join(", ", unsupportedCiphers);
                ValidatorUtil.addConstraintViolation(context, message);
                return false;
            }
        }

        return true;
    }

    public EncryptionProfileConfig getEncryptionProfileConfig() {
        return encryptionProfileConfig;
    }

    public void setEncryptionProfileConfig(EncryptionProfileConfig encryptionProfileConfig) {
        this.encryptionProfileConfig = encryptionProfileConfig;
    }
}
