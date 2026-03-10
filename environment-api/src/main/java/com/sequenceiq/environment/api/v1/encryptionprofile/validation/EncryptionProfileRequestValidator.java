package com.sequenceiq.environment.api.v1.encryptionprofile.validation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileConverter;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;

public class EncryptionProfileRequestValidator implements ConstraintValidator<ValidEncryptionProfileRequest, EncryptionProfileRequest> {

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

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

        try {
            EncryptionProfileConverter.toCipherSuites(request.getCipherSuites());
        } catch (Exception ex) {
            ValidatorUtil.addConstraintViolation(context,
                    "cipherSuites is invalid. Please use IANA names for the cipher suites.");
            return false;
        }

        // Validate them against available ciphers for the specified TLS versions
            Set<String> availableCipherSuites = encryptionProfileProvider
                    .getAllCipherSuitesAvailableByTlsVersion()
                    .entrySet()
                    .stream()
                    .filter(k -> tlsVersions.contains(TlsVersion.fromString(k.getKey())))
                    .map(Map.Entry::getValue)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

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
}
