package com.sequenceiq.environment.encryptionprofile;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@Component
public class EncryptionProfileComplianceEvaluator {

    private static final String FIPS_140_3 = "FIPS_140_3";

    private final Map<TlsVersion, Set<String>> fips1403ApprovedCipherSuites;

    public EncryptionProfileComplianceEvaluator(EncryptionProfileProvider encryptionProfileProvider) {
        fips1403ApprovedCipherSuites = encryptionProfileProvider.getFips1403ApprovedCipherSuites();
    }

    public Set<String> evaluate(Collection<TlsVersion> tlsVersions, Collection<String> cipherSuites) {
        Set<String> standards = new TreeSet<>();
        if (isFips1403Compliant(tlsVersions, cipherSuites)) {
            standards.add(FIPS_140_3);
        }
        return standards;
    }

    private boolean isFips1403Compliant(Collection<TlsVersion> tlsVersions, Collection<String> cipherSuites) {
        if (tlsVersions == null || tlsVersions.isEmpty() || cipherSuites == null || cipherSuites.isEmpty()) {
            return false;
        }
        Set<String> allApproved = fips1403ApprovedCipherSuites.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        boolean allVersionsApproved = fips1403ApprovedCipherSuites.keySet().containsAll(tlsVersions);
        boolean allCiphersApproved = allApproved.containsAll(cipherSuites);
        boolean everyVersionServed = tlsVersions.stream()
                .allMatch(version -> cipherSuites.stream().anyMatch(fips1403ApprovedCipherSuites.get(version)::contains));
        return allVersionsApproved && allCiphersApproved && everyVersionServed;
    }
}
