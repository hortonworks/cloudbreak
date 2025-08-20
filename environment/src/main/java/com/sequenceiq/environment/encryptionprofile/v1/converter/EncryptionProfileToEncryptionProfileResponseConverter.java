package com.sequenceiq.environment.encryptionprofile.v1.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.config.EncryptionProfileConfig;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.dto.EncryptionProfileDto;

@Component
public class EncryptionProfileToEncryptionProfileResponseConverter {

    @Inject
    private EncryptionProfileConfig encryptionProfileConfig;

    public EncryptionProfileResponse convert(EncryptionProfile source) {
        if (source == null) {
            return null;
        }

        EncryptionProfileResponse response = new EncryptionProfileResponse();
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setCrn(source.getResourceCrn());
        response.setTlsVersions(source.getTlsVersions().stream().map(TlsVersion::getVersion).collect(Collectors.toSet()));
        response.setCipherSuites(getCipherSuiteMap(source.getCipherSuites(), source.getTlsVersions()));
        response.setClouderaInternalCipherSuites(getClouderaCipherSuiteMap(source.getTlsVersions()));
        response.setCreated(source.getCreated());
        response.setStatus(source.getResourceStatus().name());
        return response;
    }

    private Map<String, List<String>> getCipherSuiteMap(
            List<String> cipherSuites,
            Set<TlsVersion> tlsVersions) {
        if (shouldReturnEmptyMap(cipherSuites)) {
            return Collections.emptyMap();
        }

        return tlsVersions.stream().collect(Collectors.toMap(
                TlsVersion::getVersion,
                tlsVersion -> {
                    Set<String> availableCiphers = encryptionProfileConfig.getAvailableCiphers(tlsVersion);
                    List<String> filteredCiphers = cipherSuites.stream()
                            .filter(availableCiphers::contains)
                            .collect(Collectors.toList());
                    return filteredCiphers;
                }
        ));
    }

    private Map<String, List<String>> getClouderaCipherSuiteMap(Set<TlsVersion> tlsVersions) {

        return tlsVersions.stream().collect(Collectors.toMap(
                TlsVersion::getVersion,
                tlsVersion -> new ArrayList<>(encryptionProfileConfig.getRequiredCiphers(tlsVersion))
        ));
    }

    private boolean shouldReturnEmptyMap(List<String> cipherSuites) {
        return (cipherSuites == null || cipherSuites.isEmpty());
    }

    public EncryptionProfileResponse dtoToResponse(EncryptionProfileDto encryptionProfile) {
        if (encryptionProfile == null) {
            return null;
        }

        EncryptionProfileResponse response = new EncryptionProfileResponse();
        response.setName(encryptionProfile.getName());
        response.setDescription(encryptionProfile.getDescription());
        response.setCrn(encryptionProfile.getResourceCrn());
        response.setCreated(encryptionProfile.getCreated());
        response.setTlsVersions(encryptionProfile.getTlsVersions()
                .stream()
                .map(TlsVersion::getVersion)
                .collect(Collectors.toSet()));
        response.setCipherSuites(getCipherSuiteMap(encryptionProfile.getCipherSuites(), encryptionProfile.getTlsVersions()));
        response.setClouderaInternalCipherSuites(getClouderaCipherSuiteMap(encryptionProfile.getTlsVersions()));
        return response;
    }
}
