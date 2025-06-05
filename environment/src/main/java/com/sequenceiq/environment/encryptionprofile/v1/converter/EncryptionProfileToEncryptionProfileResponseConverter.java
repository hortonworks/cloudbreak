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
        EncryptionProfileResponse response = new EncryptionProfileResponse();
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setCrn(source.getResourceCrn());
        response.setTlsVersions(source.getTlsVersions().stream().map(TlsVersion::getVersion).collect(Collectors.toSet()));
        response.setCipherSuites(getCipherSuiteMap(source.getCipherSuites(), source.getTlsVersions()));
        response.setCreated(source.getCreated());
        return response;
    }

    private Map<String, Set<String>> getCipherSuiteMap(Set<String> cipherSuites, Set<TlsVersion> tlsVersions) {
        if (cipherSuites == null || cipherSuites.isEmpty()) {
            return Collections.emptyMap();
        }

        return tlsVersions.stream()
                .collect(Collectors.toMap(
                        TlsVersion::getVersion,
                        tlsVersion -> {
                            Set<String> availableCiphers = encryptionProfileConfig.getAvailableCiphers(tlsVersion);
                            return cipherSuites.stream()
                                    .filter(availableCiphers::contains)
                                    .collect(Collectors.toSet());
                        }
                ));
    }
}
