package com.sequenceiq.environment.encryptionprofile.v1.converter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.config.EncryptionProfileConfig;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Component
public class EncryptionProfileToEncryptionProfileResponseConverter {

    @Autowired
    private EncryptionProfileConfig encryptionProfileConfig;

    public EncryptionProfileResponse convert(EncryptionProfile source) {
        return convert(source, true);
    }

    public EncryptionProfileResponse convert(EncryptionProfile source, boolean useDefaultCipherSuitesIfEmpty) {
        EncryptionProfileResponse response = new EncryptionProfileResponse();
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setCrn(source.getResourceCrn());
        response.setTlsVersions(source.getTlsVersions().stream().map(TlsVersion::getVersion).collect(Collectors.toSet()));
        response.setCipherSuites(getCipherSuiteMap(source.getCipherSuites(), source.getTlsVersions(), useDefaultCipherSuitesIfEmpty));
        response.setCreated(source.getCreated());
        response.setStatus(source.getResourceStatus().name());
        return response;
    }

    private Map<String, Set<String>> getCipherSuiteMap(Set<String> cipherSuites,
                                                    Set<TlsVersion> tlsVersions,
                                                    boolean useDefaultCipherSuitesIfEmpty) {
        if (shouldReturnEmptyMap(cipherSuites, useDefaultCipherSuitesIfEmpty)) {
            return Collections.emptyMap();
        }

        return tlsVersions.stream().collect(Collectors.toMap(
                TlsVersion::getVersion,
                tlsVersion -> {
                    Set<String> availableCiphers = encryptionProfileConfig.getAvailableCiphers(tlsVersion);
                    Set<String> filteredCiphers = cipherSuites.stream()
                            .filter(availableCiphers::contains)
                            .collect(Collectors.toSet());
                    return filteredCiphers.isEmpty() && useDefaultCipherSuitesIfEmpty
                            ? encryptionProfileConfig.getRecommendedCiphers(tlsVersion)
                            : filteredCiphers;
                }
        ));
    }

    private boolean shouldReturnEmptyMap(Set<String> cipherSuites, boolean useDefaultCipherSuitesIfEmpty) {
        return !useDefaultCipherSuitesIfEmpty && (cipherSuites == null || cipherSuites.isEmpty());
    }
}
