package com.sequenceiq.environment.api.v1.encryptionprofile.validation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileConverter;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;

public class EncryptionProfileRequestValidator implements ConstraintValidator<ValidEncryptionProfileRequest, EncryptionProfileRequest> {

    private static final Set<TlsVersion> TLS_1_2_ONLY = Set.of(TlsVersion.TLS_1_2);

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

    @Override
    public boolean isValid(EncryptionProfileRequest request, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (!validateTlsVersions(request, context) || !validateCipherSuites(request, context)) {
            return false;
        }
        Map<String, List<String>> cipherSuitesByTlsVersion = encryptionProfileProvider.getAllCipherSuitesAvailableByTlsVersion();
        return validateCipherSuitesSupported(request, cipherSuitesByTlsVersion, context)
                && validateEachTlsVersionHasCipherSuite(request, cipherSuitesByTlsVersion, context);
    }

    private boolean validateTlsVersions(EncryptionProfileRequest request, ConstraintValidatorContext context) {
        Set<TlsVersion> tlsVersions = request.getTlsVersions();

        if (CollectionUtils.isEmpty(tlsVersions)) {
            ValidatorUtil.addConstraintViolation(context,
                    "tlsVersions is a mandatory field and must not be empty.");
            return false;
        }

        if (TLS_1_2_ONLY.equals(tlsVersions)) {
            ValidatorUtil.addConstraintViolation(context,
                    "tlsVersions cannot be TLS 1.2 only.");
            return false;
        }
        return true;
    }

    private boolean validateCipherSuites(EncryptionProfileRequest request, ConstraintValidatorContext context) {
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
        return true;
    }

    private boolean validateCipherSuitesSupported(EncryptionProfileRequest request, Map<String, List<String>> cipherSuitesByTlsVersion,
            ConstraintValidatorContext context) {
        Set<String> availableCipherSuites = request.getTlsVersions()
                .stream()
                .map(tlsVersion -> cipherSuitesByTlsVersion.getOrDefault(tlsVersion.getVersion(), List.of()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        String unsupportedCiphers = request.getCipherSuites()
                .stream()
                .filter(cipher -> !availableCipherSuites.contains(cipher))
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotEmpty(unsupportedCiphers)) {
            ValidatorUtil.addConstraintViolation(context,
                    "Unsupported cipher suite(s) for the given TLS versions: " + unsupportedCiphers);
            return false;
        }
        return true;
    }

    private boolean validateEachTlsVersionHasCipherSuite(EncryptionProfileRequest request, Map<String, List<String>> cipherSuitesByTlsVersion,
            ConstraintValidatorContext context) {
        String uncoveredTlsVersions = request.getTlsVersions()
                .stream()
                .map(TlsVersion::getVersion)
                .filter(version -> request.getCipherSuites()
                        .stream()
                        .noneMatch(cipherSuitesByTlsVersion.getOrDefault(version, List.of())::contains))
                .sorted()
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotEmpty(uncoveredTlsVersions)) {
            ValidatorUtil.addConstraintViolation(context,
                    "cipherSuites must contain at least one cipher suite for each TLS version. Missing cipher suite(s) for: " + uncoveredTlsVersions);
            return false;
        }
        return true;
    }
}
