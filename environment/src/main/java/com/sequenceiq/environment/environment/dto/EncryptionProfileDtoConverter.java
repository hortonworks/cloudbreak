package com.sequenceiq.environment.environment.dto;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.dto.EncryptionProfileDto.Builder;

@Component
public class EncryptionProfileDtoConverter {

    public EncryptionProfile dtoToEncryptionProfile(EncryptionProfileDto encryptionProfileDto) {
        if (encryptionProfileDto == null) {
            return null;
        }

        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setId(encryptionProfileDto.getId());
        encryptionProfile.setName(encryptionProfileDto.getName());
        encryptionProfile.setDescription(encryptionProfileDto.getDescription());
        encryptionProfile.setAccountId(encryptionProfileDto.getAccountId());
        encryptionProfile.setResourceCrn(encryptionProfileDto.getResourceCrn());
        encryptionProfile.setTlsVersions(encryptionProfileDto.getTlsVersions());
        encryptionProfile.setCipherSuites(encryptionProfileDto.getCipherSuites());

        return encryptionProfile;
    }

    public EncryptionProfileDto encryptionProfileToDto(EncryptionProfile encryptionProfile) {
        if (encryptionProfile == null) {
            return null;
        }

        EncryptionProfileDto encryptionProfileDto = Builder
                .builder()
                .withId(encryptionProfile.getId())
                .withName(encryptionProfile.getName())
                .withDescription(encryptionProfile.getDescription())
                .withAccountId(encryptionProfile.getAccountId())
                .withResourceCrn(encryptionProfile.getResourceCrn())
                .withTlsVersions(encryptionProfile.getTlsVersions())
                .withCipherSuites(encryptionProfile.getCipherSuites())
                .build();

        return encryptionProfileDto;
    }
}
