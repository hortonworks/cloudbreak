package com.sequenceiq.environment.api.v1.encryptionprofile.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tls.CipherSuiteProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileRequestValidatorTest {

    private ConstraintValidatorContext context;

    private CipherSuiteProvider cipherSuiteProvider = new CipherSuiteProvider();

    @Spy
    private EncryptionProfileProvider encryptionProfileProvider = new EncryptionProfileProvider(cipherSuiteProvider);

    @InjectMocks
    private EncryptionProfileRequestValidator underTest;

    @BeforeEach
    void setup() {
        context = mock(ConstraintValidatorContext.class);
        ConstraintViolationBuilder violationBuilder = mock(ConstraintViolationBuilder.class);
        NodeBuilderCustomizableContext nodeBuilder = mock(NodeBuilderCustomizableContext.class);

        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void testNullTlsVersions() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setTlsVersions(null);

        boolean result = underTest.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("tlsVersions is a mandatory field"));
    }

    @Test
    void testEmptyTlsVersions() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setTlsVersions(new HashSet<>());

        boolean result = underTest.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("tlsVersions is a mandatory field"));
    }

    @Test
    void testValidTlsVersionsNoCipherSuites() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_2);
        request.setTlsVersions(tls);

        boolean result = underTest.isValid(request, context);

        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate("cipherSuites is a mandatory field and must not be empty.");
    }

    @Test
    void testValidTlsVersionsValidCipherSuites() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_3);
        request.setTlsVersions(tls);

        List<String> ciphers = Arrays.asList("TLS_AES_128_GCM_SHA256");
        request.setCipherSuites(ciphers);

        boolean result = underTest.isValid(request, context);
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

        boolean result = underTest.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate("cipherSuites is invalid. Please use IANA names for the cipher suites.");
    }

    @Test
    void testValidInvalidCipherSuiteForTlsVersions() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        Set<TlsVersion> tls = new HashSet<>();
        tls.add(TlsVersion.TLS_1_2);
        request.setTlsVersions(tls);

        List<String> ciphers = Arrays.asList("TLS_CHACHA20_POLY1305_SHA256");
        request.setCipherSuites(ciphers);

        boolean result = underTest.isValid(request, context);
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(contains("Unsupported cipher suite(s) for the given TLS versions: TLS_CHACHA20_POLY1305_SHA256"));
    }
}



