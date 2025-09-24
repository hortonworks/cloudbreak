package com.sequenceiq.environment.api.v1.encryptionprofile.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.tls.DefaultEncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.config.EncryptionProfileConfig;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;

public class EncryptionProfileRequestValidatorTest {

    private EncryptionProfileRequestValidator validator;

    private ConstraintValidatorContext context;

    @BeforeEach
    void setup() {
        EncryptionProfileConfig encryptionProfileConfig = buildTestEncryptionProfileConfig();
        DefaultEncryptionProfileProvider defaultEncryptionProfileProvider = new DefaultEncryptionProfileProvider();
        validator = new EncryptionProfileRequestValidator();
        validator.setEncryptionProfileConfig(encryptionProfileConfig);
        validator.setDefaultEncryptionProfileProvider(defaultEncryptionProfileProvider);

        context = mock(ConstraintValidatorContext.class);
        ConstraintViolationBuilder violationBuilder = mock(ConstraintViolationBuilder.class);
        NodeBuilderCustomizableContext nodeBuilder = mock(NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    private EncryptionProfileConfig buildTestEncryptionProfileConfig() {
        EncryptionProfileConfig config = new EncryptionProfileConfig();
        Map<TlsVersion, EncryptionProfileConfig.TlsCipherGroup> mapping = new HashMap<>();

        EncryptionProfileConfig.TlsCipherGroup tls12 = new EncryptionProfileConfig.TlsCipherGroup();
        tls12.setAvailable(new HashSet<>(Arrays.asList(
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
        )));
        tls12.setAvailable(new HashSet<>(Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_256_CBC_SHA")));
        tls12.setRequired(new HashSet<>(Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")));

        EncryptionProfileConfig.TlsCipherGroup tls13 = new EncryptionProfileConfig.TlsCipherGroup();
        tls13.setAvailable(new HashSet<>(Arrays.asList("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")));
        tls13.setRequired(new HashSet<>(Arrays.asList("TLS_AES_128_GCM_SHA256")));

        mapping.put(TlsVersion.TLS_1_2, tls12);
        mapping.put(TlsVersion.TLS_1_3, tls13);

        config.setTlsCipherMapping(mapping);
        return config;
    }

    @Test
    void testNullTlsVersions() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setTlsVersions(null);

        boolean result = validator.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("tlsVersions is a mandatory field"));
    }

    @Test
    void testEmptyTlsVersions() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setTlsVersions(new HashSet<>());

        boolean result = validator.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("tlsVersions is a mandatory field"));
    }

    @Test
    void testValidTlsVersionsNoCipherSuites() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_2);
        request.setTlsVersions(tls);

        boolean result = validator.isValid(request, context);
        assertTrue(result);
    }

    @Test
    void testValidTlsVersionsValidCipherSuites() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_3);
        request.setTlsVersions(tls);

        List<String> ciphers = Arrays.asList("TLS_AES_128_GCM_SHA256");
        request.setCipherSuites(ciphers);

        boolean result = validator.isValid(request, context);
        assertTrue(result);
    }

    @Test
    void testInvalidCipherSuites() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_2);
        request.setTlsVersions(tls);

        List<String> ciphers = Arrays.asList("INVALID_CIPHER");
        request.setCipherSuites(ciphers);

        boolean result = validator.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("The following cipher(s) are not allowed: [INVALID_CIPHER]"));
    }

    @Test
    void testValidInvalidCipherSuiteForTlsVersions() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_2);
        request.setTlsVersions(tls);

        List<String> ciphers = Arrays.asList("TLS_CHACHA20_POLY1305_SHA256");
        request.setCipherSuites(ciphers);

        boolean result = validator.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("Unsupported cipher suite(s) for the given TLS versions: TLS_CHACHA20_POLY1305_SHA256"));
    }
}



