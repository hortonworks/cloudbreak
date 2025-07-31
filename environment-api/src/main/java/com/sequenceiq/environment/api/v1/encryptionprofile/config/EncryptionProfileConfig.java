package com.sequenceiq.environment.api.v1.encryptionprofile.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
@Component
@ConfigurationProperties(prefix = "encryption-profile")
public class EncryptionProfileConfig {

    private Map<TlsVersion, TlsCipherGroup> tlsCipherMapping;

    public Map<TlsVersion, TlsCipherGroup> getTlsCipherMapping() {
        return tlsCipherMapping;
    }

    public void setTlsCipherMapping(Map<TlsVersion, TlsCipherGroup> tlsCipherMapping) {
        this.tlsCipherMapping = tlsCipherMapping;
    }

    public Set<TlsVersion> getSupportedTlsVersions() {
        return tlsCipherMapping != null ? tlsCipherMapping.keySet() : Collections.emptySet();
    }

    public Set<String> getAvailableCiphers(TlsVersion tlsVersion) {
        TlsCipherGroup group = tlsCipherMapping.get(tlsVersion);
        return group != null ? group.getAvailable() : Collections.emptySet();
    }

    public Set<String> getAvailableCipherSet(Set<TlsVersion> tlsVersions) {
        return tlsVersions.stream()
                .flatMap(tls -> getAvailableCiphers(tls).stream())
                .collect(Collectors.toSet());
    }

    public Set<String> getRequiredCiphers(TlsVersion tlsVersion) {
        TlsCipherGroup group = tlsCipherMapping.get(tlsVersion);
        return group != null ? group.getRequired() : Collections.emptySet();
    }

    public static class TlsCipherGroup {
        private Set<String> available;

        private Set<String> required;

        public Set<String> getAvailable() {
            return available;
        }

        public void setAvailable(Set<String> available) {
            this.available = available;
        }

        public Set<String> getRequired() {
            return required;
        }

        public void setRequired(Set<String> required) {
            this.required = required;
        }
    }
}

