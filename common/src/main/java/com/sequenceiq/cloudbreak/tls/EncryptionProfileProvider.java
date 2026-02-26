package com.sequenceiq.cloudbreak.tls;

import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.TLS_1_2_RECOMMENDED;
import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.TLS_1_3_RECOMMENDED;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class EncryptionProfileProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileProvider.class);

    private static final String COLON_SEPARATOR = ":";

    private final CipherSuiteProvider cipherSuiteProvider;

    public EncryptionProfileProvider(CipherSuiteProvider cipherSuiteProvider) {
        this.cipherSuiteProvider = cipherSuiteProvider;
    }

    public String getTlsVersions(Set<String> userTlsVersions, String separator) {
        String[] tlsVersions;
        if (!CollectionUtils.isEmpty(userTlsVersions)) {
            tlsVersions = userTlsVersions.stream().sorted().toArray(String[]::new);
        } else {
            tlsVersions = new String[]{TLS_1_2.getVersion()};
        }
        return String.join(separator, tlsVersions);
    }

    public String getCipherSuiteString(CipherSuitesLimitType cipherSuitesLimitType, String separator, boolean useIanaNames) {
        List<String> referenceList = getCipherSuiteNamesByLimitType(cipherSuitesLimitType, useIanaNames);
        return String.join(separator, referenceList);
    }

    private List<String> getCipherSuiteNamesByLimitType(CipherSuitesLimitType cipherSuitesLimitType) {
        return getCipherSuiteNamesByLimitType(cipherSuitesLimitType, true);
    }

    private List<String> getCipherSuiteNamesByLimitType(CipherSuitesLimitType cipherSuitesLimitType, boolean useIanaNames) {
        return cipherSuiteProvider.getCipherSuitesByLimitType(cipherSuitesLimitType)
                .stream()
                .map(useIanaNames ? CipherSuite::getIanaName : CipherSuite::getOpenSslName)
                .toList();
    }

    public List<String> getTlsCipherSuitesIanaList(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType) {
        List<String> userCipherSuits = getCipherSuitesFromMap(userEncryptionProfileMap);
        return getTlsCipherSuitesIanaList(userCipherSuits, cipherSuitesLimitType);
    }

    public List<String> getTlsCipherSuitesIanaList(List<String> userSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        if (CollectionUtils.isEmpty(userSuites)) {
            userSuites = getCipherSuiteNamesByLimitType(cipherSuitesLimitType);
        }
        List<CipherSuite> givenSuites = validCipherSuites(userSuites);
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        return effectiveSuites
                .stream()
                .map(CipherSuite::getIanaName)
                .distinct()
                .collect(Collectors.toList());
    }

    public String getTlsCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType, String separator,
            boolean useIanaNames) {
        List<String> userCipherSuits = getCipherSuitesFromMap(userEncryptionProfileMap);
        return getTlsCipherSuites(userCipherSuits, cipherSuitesLimitType, separator, useIanaNames);
    }

    public String getTlsCipherSuites(List<String> userSuites, CipherSuitesLimitType cipherSuitesLimitType, String separator, boolean useIanaNames) {
        if (CollectionUtils.isEmpty(userSuites)) {
            userSuites = getCipherSuiteNamesByLimitType(cipherSuitesLimitType);
        }
        List<CipherSuite> givenSuites = validCipherSuites(userSuites);
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        return  effectiveSuites
                .stream()
                .map(useIanaNames ? CipherSuite::getIanaName : CipherSuite::getOpenSslName)
                .distinct()
                .collect(Collectors.joining(separator));
    }

    public Map<String, List<String>> getAllCipherSuitesAvailableByTlsVersion() {
        return Arrays.stream(CipherSuite.values())
                .flatMap(cipher -> cipher.getTlsVersions()
                        .stream()
                        .map(tlsVersion -> Map.entry(tlsVersion, cipher.getIanaName())))
                .collect(Collectors.groupingBy(entry ->
                        entry.getKey().getVersion(), TreeMap::new, Collectors.mapping(Entry::getValue, Collectors.toList())));
    }

    public Map<String, List<String>> getRecommendedCipherSuites() {
        return Map.of(
                TLS_1_2.getVersion(), getCipherSuiteNamesByLimitType(CipherSuitesLimitType.TLS_1_2_RECOMMENDED),
                TLS_1_3.getVersion(), getCipherSuiteNamesByLimitType(CipherSuitesLimitType.TLS_1_3_RECOMMENDED)
        );
    }

    private List<String> getCipherSuitesFromMap(Map<String, List<String>> userEncryptionProfileMap) {
        List<String> userCipherSuits = null;
        if (!CollectionUtils.isEmpty(userEncryptionProfileMap)) {
            userCipherSuits = userEncryptionProfileMap
                    .entrySet()
                    .stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
        }
        return userCipherSuits;
    }

    private List<CipherSuite> validCipherSuites(List<String> suites) {
        Map<String, CipherSuite> validCipherSuites = Arrays.stream(CipherSuite.values())
                .collect(Collectors.toMap(CipherSuite::getIanaName, cs -> cs));
        return suites.stream()
                .filter(validCipherSuites::containsKey)
                .map(validCipherSuites::get)
                .toList();
    }

    private List<CipherSuite> getEffectiveCipherSuites(List<CipherSuite> givenSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        List<String> referenceList = getCipherSuiteNamesByLimitType(cipherSuitesLimitType);
        return givenSuites
                .stream()
                .filter(gs -> referenceList.contains(gs.getIanaName()))
                .distinct()
                .toList();
    }

    public List<String> convertCipherSuitesToIana(List<String> cipherSuites) {
        if (CollectionUtils.isEmpty(cipherSuites)) {
            return null;
        }
        List<String> result = new ArrayList<>();
        List<String> notAllowedCipherSuites = new ArrayList<>();
        Map<String, CipherSuite> cipherSuiteByOpenSslMap = Arrays.stream(CipherSuite.values())
                .collect(Collectors.toUnmodifiableMap(
                        CipherSuite::getOpenSslName,
                        Function.identity()
                ));
        for (String cipherSuite : cipherSuites) {
            if (CipherSuite.fromIanaName(cipherSuite).isPresent()) {
                result.add(cipherSuite);
            } else if (cipherSuiteByOpenSslMap.containsKey(cipherSuite)) {
                result.add(cipherSuiteByOpenSslMap.get(cipherSuite).getIanaName());
            } else {
                notAllowedCipherSuites.add(cipherSuite);
            }
        }

        if (!notAllowedCipherSuites.isEmpty()) {
            throw new IllegalArgumentException("The following cipher(s) are not allowed: " + notAllowedCipherSuites);
        }

        return result;
    }

    public String getIanaCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType defaultCipherSuitesLimitType, boolean addTls13,
            Set<String> userTlsVersions, boolean defaultEncryptionProfile) {
        return getCipherSuites(userEncryptionProfileMap, defaultCipherSuitesLimitType, COLON_SEPARATOR, true, addTls13, userTlsVersions,
                defaultEncryptionProfile);
    }

    public String getOpenSslCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType defaultCipherSuitesLimitType,
            boolean addTls13, Set<String> userTlsVersions, boolean defaultEncryptionProfile) {
        return getCipherSuites(userEncryptionProfileMap, defaultCipherSuitesLimitType, COLON_SEPARATOR, false, addTls13, userTlsVersions,
                defaultEncryptionProfile);
    }

    public String getCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType defaultCipherSuitesLimitType,
            String separator, boolean useIana, boolean addTls13, Set<String> userTlsVersions, boolean defaultEncryptionProfile) {
        String result;
        if (!defaultEncryptionProfile && !CollectionUtils.isEmpty(userTlsVersions)) {
            String tls13Ciphers = "";
            if (addTls13) {
                tls13Ciphers = getTls13CipherSuites(userEncryptionProfileMap, userTlsVersions);
            }
            String tls12Ciphers = getRecommendedCipherSuiteByTlsVersion(TLS_1_2.getVersion(), userEncryptionProfileMap, separator, useIana, userTlsVersions);
            result = Stream.of(tls13Ciphers, tls12Ciphers)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.joining(separator));
        } else {
            result = getTlsCipherSuites(
                    userEncryptionProfileMap,
                    defaultCipherSuitesLimitType,
                    separator,
                    useIana);
        }
        LOGGER.info("Cipher suites list: {}", result);
        return result;
    }

    public String getDefaultRecommendedTls12CipherSuites(boolean useIanaNames) {
        return getTlsCipherSuites(
                (Map<String, List<String>>) null,
                TLS_1_2_RECOMMENDED,
                COLON_SEPARATOR,
                useIanaNames);
    }

    public String getTls13CipherSuites(Map<String, List<String>> userEncryptionProfileMap, Set<String> userTlsVersions) {
        return getRecommendedCipherSuiteByTlsVersion(TLS_1_3.getVersion(), userEncryptionProfileMap, COLON_SEPARATOR, true, userTlsVersions);
    }

    private String getRecommendedCipherSuiteByTlsVersion(String tlsVersion, Map<String, List<String>> userEncryptionProfileMap, String separator,
            boolean ianaNames, Set<String> userTlsVersions) {
        String cipherSuites = "";
        if (!CollectionUtils.isEmpty(userTlsVersions) && userTlsVersions.contains(tlsVersion)) {
            List<String> ciphersList = null;
            if (!CollectionUtils.isEmpty(userEncryptionProfileMap) &&
                    userEncryptionProfileMap.containsKey(tlsVersion)) {
                ciphersList = userEncryptionProfileMap.get(tlsVersion);
            }
            cipherSuites = getTlsCipherSuites(
                    ciphersList,
                    tlsVersion.equalsIgnoreCase(TLS_1_3.getVersion()) ? TLS_1_3_RECOMMENDED : TLS_1_2_RECOMMENDED,
                    separator,
                    ianaNames);
        }
        LOGGER.debug("Cipher suites selected from recommended: {}", cipherSuites);
        return cipherSuites;
    }
}
