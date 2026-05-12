package com.sequenceiq.environment.encryptionprofile.v1.converter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Component
public class EncryptionProfileToEncryptionProfileResponseConverter {

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

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
        response.setClouderaInternalCipherSuites(Collections.emptyMap());
        response.setCreated(source.getCreated());
        response.setStatus(source.getResourceStatus().name());
        return response;
    }

    private Map<String, List<String>> getCipherSuiteMap(
            List<String> cipherSuites,
            Set<TlsVersion> tlsVersions) {
        Map<String, List<String>> availableCipherSuites = encryptionProfileProvider.getAllCipherSuitesAvailableByTlsVersion();
        return tlsVersions.stream().collect(Collectors.toMap(
                TlsVersion::getVersion,
                tlsVersion -> {
                    List<String> availableCiphers = availableCipherSuites.get(tlsVersion.getVersion());

                    return cipherSuites
                            .stream()
                            .filter(availableCiphers::contains)
                            .collect(Collectors.toList());
                }
        ));
    }
}
