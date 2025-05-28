package com.sequenceiq.environment.environment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.dto.EncryptionProfileDto.Builder;

class EncryptionProfileDtoConverterTest {

    private final EncryptionProfileDtoConverter underTest = new EncryptionProfileDtoConverter();

    @Test
    void testDtoToEncryptionProfile() {
        EncryptionProfileDto dto = Builder
                .builder()
                .withName("profName")
                .withDescription("description")
                .withResourceCrn("crn")
                .withAccountId("accountId")
                .withTlsVersions(Arrays.stream(TlsVersion.values()).collect(Collectors.toSet()))
                .withCipherSuites(Set.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"))
                .build();

        EncryptionProfile result = underTest.dtoToEncryptionProfile(dto);

        assertEquals(dto.getName(), result.getName());
        assertEquals(dto.getDescription(), result.getDescription());
        assertEquals(dto.getResourceCrn(), result.getResourceCrn());
        assertEquals(dto.getAccountId(), result.getAccountId());
        assertEquals(dto.getTlsVersions(), result.getTlsVersions());
        assertEquals(dto.getCipherSuites(), result.getCipherSuites());
    }

    @Test
    void testEncryptionProfileToDto() {
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName("profName");
        encryptionProfile.setDescription("description");
        encryptionProfile.setResourceCrn("crn");
        encryptionProfile.setAccountId("accountId");
        encryptionProfile.setTlsVersions(Arrays.stream(TlsVersion.values()).collect(Collectors.toSet()));
        encryptionProfile.setCipherSuites(Set.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"));

        EncryptionProfileDto result = underTest.encryptionProfileToDto(encryptionProfile);

        assertEquals(encryptionProfile.getName(), result.getName());
        assertEquals(encryptionProfile.getDescription(), result.getDescription());
        assertEquals(encryptionProfile.getResourceCrn(), result.getResourceCrn());
        assertEquals(encryptionProfile.getAccountId(), result.getAccountId());
        assertEquals(encryptionProfile.getTlsVersions(), result.getTlsVersions());
        assertEquals(encryptionProfile.getCipherSuites(), result.getCipherSuites());
    }
}
