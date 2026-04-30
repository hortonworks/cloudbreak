package com.sequenceiq.cloudbreak.tls;

import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.BLACKBOX_EXPORTER;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EncryptionProfileProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileProvider.class);

    private static final String COLON_SEPARATOR = ":";

    private final CipherSuiteProvider cipherSuiteProvider;

    public EncryptionProfileProvider(CipherSuiteProvider cipherSuiteProvider) {
        this.cipherSuiteProvider = cipherSuiteProvider;
    }

    public String getLegacyCipherSuites(List<String> userSuites, CipherSuiteOptions cipherSuiteOptions) {
        List<CipherSuite> givenSuites = EncryptionProfileConverter.toCipherSuites(userSuites);
        List<CipherSuite> effectiveSuites = filterCipherSuitesByLimitType(givenSuites, cipherSuiteOptions.cipherSuitesLimitType());
        return EncryptionProfileConverter.toString(effectiveSuites, cipherSuiteOptions.useIana(), COLON_SEPARATOR);
    }

    private List<CipherSuite> filterCipherSuitesByLimitType(List<CipherSuite> givenSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        List<CipherSuite> referenceList = cipherSuiteProvider.getLegacyCipherSuitesByLimitType(cipherSuitesLimitType);
        return givenSuites
                .stream()
                .filter(referenceList::contains)
                .distinct()
                .toList();
    }

    public Map<String, List<String>> getAllCipherSuitesAvailableByTlsVersion() {
        return getAllowedCipherSuites()
                .stream()
                .flatMap(cipher -> cipher.getTlsVersions()
                        .stream()
                        .map(tlsVersion -> Map.entry(tlsVersion, cipher.getIanaName())))
                .collect(Collectors.groupingBy(entry ->
                        entry.getKey().getVersion(), TreeMap::new, Collectors.mapping(Entry::getValue, Collectors.toList())));
    }

    public List<CipherSuite> getAllowedCipherSuites() {
        List<CipherSuite> result = new ArrayList<>(cipherSuiteProvider.getAllowedTls13CipherSuites());
        result.addAll(cipherSuiteProvider.getAllowedTls12CipherSuites());
        return result;
    }

    public Map<String, List<String>> getRecommendedCipherSuites() {
        return Map.of(
                TLS_1_2.getVersion(), EncryptionProfileConverter.toListString(cipherSuiteProvider.getRecommendedTls12CipherSuites()),
                TLS_1_3.getVersion(), EncryptionProfileConverter.toListString(cipherSuiteProvider.getRecommendedTls13CipherSuites())
        );
    }

    public String getIanaCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType,
            boolean legacyEncryptionProfile) {
        return getCipherSuites(userEncryptionProfileMap, new CipherSuiteOptions(cipherSuitesLimitType, legacyEncryptionProfile, true, true));
    }

    public String getOpenSslCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType,
            boolean legacyEncryptionProfile) {
        return getCipherSuites(userEncryptionProfileMap, new CipherSuiteOptions(cipherSuitesLimitType, legacyEncryptionProfile, false, false));
    }

    public String getCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuiteOptions cipherSuiteOptions) {
        String result = cipherSuiteOptions.legacyEncryptionProfile()
                ? getLegacyCipherSuites(userEncryptionProfileMap.get(TLS_1_2.getVersion()), cipherSuiteOptions)
                : getNewCipherSuites(userEncryptionProfileMap, cipherSuiteOptions);
        LOGGER.info("Cipher suites list: {}", result);
        return result;
    }

    private String getNewCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuiteOptions cipherSuiteOptions) {
        String tls13Ciphers = StringUtils.EMPTY;
        if (cipherSuiteOptions.addTls13()) {
            tls13Ciphers = getTls13CipherSuites(userEncryptionProfileMap);
        }
        String tls12Ciphers = getCipherSuiteByTlsVersion(TLS_1_2.getVersion(), userEncryptionProfileMap, cipherSuiteOptions.useIana());

        return EncryptionProfileConverter.mergeCipherSuites(tls13Ciphers, tls12Ciphers, COLON_SEPARATOR);
    }

    private String getCipherSuiteByTlsVersion(String tlsVersion, Map<String, List<String>> userEncryptionProfileMap, boolean ianaName) {
        String cipherSuites = StringUtils.EMPTY;
        if (userEncryptionProfileMap.containsKey(tlsVersion)) {
            List<String> ciphersList = userEncryptionProfileMap.get(tlsVersion);
            cipherSuites = EncryptionProfileConverter.fromListToString(ciphersList, ianaName, COLON_SEPARATOR);
        }
        return cipherSuites;
    }

    public String getDefaultTls12CipherSuites(boolean useIanaName) {
        return EncryptionProfileConverter.toString(cipherSuiteProvider
                .getRecommendedTls12CipherSuites(), useIanaName, COLON_SEPARATOR);
    }

    public String getTls13CipherSuites(Map<String, List<String>> userEncryptionProfileMap) {
        return getCipherSuiteByTlsVersion(TLS_1_3.getVersion(), userEncryptionProfileMap, true);
    }

    public List<String> getBlackboxCipherSuites() {
        return EncryptionProfileConverter.toListString(cipherSuiteProvider.getLegacyCipherSuitesByLimitType(BLACKBOX_EXPORTER));
    }

}
