package com.sequenceiq.environment.encryptionprofile.v1.converter;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.CIPHER_SUITES;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.DESCRIPTION;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.TLS_VERSIONS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileRequestToEncryptionProfileConverterTest {

    private EncryptionProfileRequestToEncryptionProfileConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new EncryptionProfileRequestToEncryptionProfileConverter();
    }

    @Test
    void testConvertName() {
        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());
        assertThat(result.getName()).isEqualTo(NAME);
    }

    @Test
    void testConvertDescription() {
        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void testConvertTlsVersions() {
        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());
        assertThat(result.getTlsVersions())
                .containsExactlyInAnyOrderElementsOf(TLS_VERSIONS);
    }

    @Test
    void testConvertCipherSuites() {
        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());
        assertThat(result.getCipherSuites())
                .containsExactlyInAnyOrderElementsOf(CIPHER_SUITES);
    }
}
