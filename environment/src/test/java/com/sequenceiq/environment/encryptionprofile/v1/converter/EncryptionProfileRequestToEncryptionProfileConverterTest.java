package com.sequenceiq.environment.encryptionprofile.v1.converter;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.CIPHER_SUITES;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.DESCRIPTION;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.TLS_VERSIONS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tls.DefaultEncryptionProfileProvider;
import com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileRequestToEncryptionProfileConverterTest {

    @Mock
    private DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    @InjectMocks
    private EncryptionProfileRequestToEncryptionProfileConverter underTest;

    @Test
    void testConvertName() {
        when(defaultEncryptionProfileProvider.convertCipherSuitesToIana(new ArrayList<>(CIPHER_SUITES)))
                .thenReturn(new ArrayList<>(CIPHER_SUITES));

        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());

        assertThat(result.getName()).isEqualTo(NAME);
    }

    @Test
    void testConvertDescription() {
        when(defaultEncryptionProfileProvider.convertCipherSuitesToIana(new ArrayList<>(CIPHER_SUITES)))
                .thenReturn(new ArrayList<>(CIPHER_SUITES));

        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());

        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void testConvertTlsVersions() {
        when(defaultEncryptionProfileProvider.convertCipherSuitesToIana(new ArrayList<>(CIPHER_SUITES)))
                .thenReturn(new ArrayList<>(CIPHER_SUITES));

        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());

        assertThat(result.getTlsVersions())
                .containsExactlyInAnyOrderElementsOf(TLS_VERSIONS);
    }

    @Test
    void testConvertCipherSuites() {
        when(defaultEncryptionProfileProvider.convertCipherSuitesToIana(new ArrayList<>(CIPHER_SUITES)))
                .thenReturn(new ArrayList<>(CIPHER_SUITES));

        EncryptionProfile result = underTest.convert(
                EncryptionProfileTestConstants.getTestEncryptionProfileRequest());

        assertThat(result.getCipherSuites())
                .containsExactlyInAnyOrderElementsOf(CIPHER_SUITES);
    }
}
