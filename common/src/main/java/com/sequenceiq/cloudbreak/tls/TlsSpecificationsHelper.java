package com.sequenceiq.cloudbreak.tls;

import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TlsSpecificationsHelper {

    private TlsSpecificationsHelper() {
    }

    public static String getTlsVersions(String separator) {
        return getTlsVersions(null, separator);
    }

    public static String getTlsVersions(Set<String> userTlsVersions, String separator) {
        String[] tlsVersions;
        if (userTlsVersions != null && !userTlsVersions.isEmpty()) {
            tlsVersions = userTlsVersions.stream().sorted().toArray(String[]::new);
        } else {
            tlsVersions = new String[]{TLS_1_2.getVersion(), TLS_1_3.getVersion()};
        }
        return String.join(separator, tlsVersions);
    }

    public static String getCipherSuiteString(CipherSuitesLimitType cipherSuitesLimitType, String separator) {
        String[] referenceList = getDefaultCipherSuiteList(cipherSuitesLimitType);
        return String.join(separator, referenceList);
    }

    public static String[] getDefaultCipherSuiteList(CipherSuitesLimitType cipherSuitesLimitType) {
        Set<String> referenceList;
        switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL:
            case CipherSuitesLimitType.REDHAT_VERSION8:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"));
                break;
            case CipherSuitesLimitType.BLACKBOX_EXPORTER:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"));
                break;
            case CipherSuitesLimitType.JAVA_INTERMEDIATE2018:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_RSA_WITH_AES_256_CBC_SHA"));
                break;
            case CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018:
                referenceList = new LinkedHashSet<>(List.of(
                        "ECDHE-ECDSA-AES128-GCM-SHA256",
                        "ECDHE-RSA-AES128-GCM-SHA256",
                        "ECDHE-ECDSA-AES256-GCM-SHA384",
                        "ECDHE-RSA-AES256-GCM-SHA384",
                        "DHE-RSA-AES128-GCM-SHA256",
                        "DHE-RSA-AES256-GCM-SHA384",
                        "ECDHE-ECDSA-AES128-SHA256",
                        "ECDHE-ECDSA-AES128-SHA",
                        "ECDHE-RSA-AES128-SHA",
                        "ECDHE-ECDSA-AES256-SHA384",
                        "ECDHE-ECDSA-AES256-SHA",
                        "ECDHE-RSA-AES256-SHA",
                        "DHE-RSA-AES128-SHA256",
                        "DHE-RSA-AES128-SHA",
                        "DHE-RSA-AES256-SHA256",
                        "DHE-RSA-AES256-SHA",
                        "AES128-SHA",
                        "AES256-SHA"));
                break;
            case CipherSuitesLimitType.DEFAULT:
            default:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"));
                break;
        }
        return referenceList.toArray(new String[0]);

    }

    public static String getTlsCipherSuites(Map<String, Set<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType, String separator,
            boolean useIanaNames) {
        String[] userCipherSuits = getCipherSuitAsArray(userEncryptionProfileMap);
        return getTlsCipherSuites(userCipherSuits, cipherSuitesLimitType, separator, useIanaNames);
    }

    public static List<String> getTlsCipherSuitesIanaList(String[] userSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        String[] suites = userSuites;
        if (suites == null || suites.length == 0) {
            suites = getDefaultCipherSuiteList(cipherSuitesLimitType);
        }
        List<CipherSuite> givenSuites = validateCipherSuites(new HashSet<>(Arrays.stream(suites).toList()));
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        return effectiveSuites.stream().map(CipherSuite::getIanaName).collect(Collectors.toList());
    }

    public static List<String> getTlsCipherSuitesIanaList(Map<String, Set<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType) {
        String[] userCipherSuits = getCipherSuitAsArray(userEncryptionProfileMap);
        return getTlsCipherSuitesIanaList(userCipherSuits, cipherSuitesLimitType);
    }

    public static String getTlsCipherSuites(String[] userSuites, CipherSuitesLimitType cipherSuitesLimitType, String separator, boolean useIanaNames) {
        String[] suites = userSuites;
        if (suites == null || suites.length == 0) {
            suites = getDefaultCipherSuiteList(cipherSuitesLimitType);
        }
        List<CipherSuite> givenSuites = validateCipherSuites(new HashSet<>(Arrays.stream(suites).toList()));
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        if (useIanaNames) {
            return effectiveSuites.stream().map(CipherSuite::getIanaName).collect(Collectors.joining(separator));
        } else {
            return effectiveSuites.stream().map(CipherSuite::getName).collect(Collectors.joining(separator));
        }
    }

    public static String getTlsCipherSuites(String[] suites, CipherSuitesLimitType cipherSuitesLimitType, String separator) {
        return getTlsCipherSuites(suites, cipherSuitesLimitType, separator, false);
    }

    public static Map<String, Set<String>> getAllCipherSuitesAvailableByTlsVersion() {
        return getCipherSuiteList()
                .stream()
                .flatMap(cipher -> cipher.getTlsVersions()
                        .stream()
                        .map(tlsVersion -> Map.entry(tlsVersion, cipher.getIanaName())))
                .collect(Collectors.groupingBy(entry ->
                        entry.getKey().getVersion(), TreeMap::new, Collectors.mapping(Entry::getValue, Collectors.toSet())));
    }

    public static Map<String, Set<String>> getRecommendedCipherSuites() {
        return Map.of(TLS_1_2.getVersion(), Set.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
                        "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", "TLS_ECCPWD_WITH_AES_128_GCM_SHA256",
                        "TLS_ECCPWD_WITH_AES_256_GCM_SHA384"),
                TLS_1_3.getVersion(), Set.of("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", "TLS_ECCPWD_WITH_AES_128_GCM_SHA256", "TLS_ECCPWD_WITH_AES_256_GCM_SHA384",
                        "TLS_CHACHA20_POLY1305_SHA256"));
    }

    private static String[] getCipherSuitAsArray(Map<String, Set<String>> userEncryptionProfileMap) {
        String[] userCipherSuits = null;
        if (userEncryptionProfileMap != null && !userEncryptionProfileMap.isEmpty()) {
            userCipherSuits = userEncryptionProfileMap
                    .entrySet()
                    .stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .flatMap(Set::stream)
                    .toArray(String[]::new);
        }
        return userCipherSuits;
    }

    private static List<CipherSuite> validateCipherSuites(Set<String> suites) {
        return getCipherSuiteList()
                .stream()
                .filter(cs -> suites.contains(cs.getIanaName()))
                .toList();
    }

    private static List<CipherSuite> getEffectiveCipherSuites(List<CipherSuite> givenSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        Set<String> referenceList = new HashSet<>();
        switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL:
            case CipherSuitesLimitType.REDHAT_VERSION8:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"));
                break;
            case CipherSuitesLimitType.BLACKBOX_EXPORTER:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"));
                break;
            case CipherSuitesLimitType.JAVA_INTERMEDIATE2018:
                referenceList = new LinkedHashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                            "TLS_RSA_WITH_AES_128_CBC_SHA",
                            "TLS_RSA_WITH_AES_256_CBC_SHA"));
                break;
            case CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018:
                referenceList = new LinkedHashSet<>(List.of(
                    "ECDHE-ECDSA-AES128-GCM-SHA256",
                            "ECDHE-RSA-AES128-GCM-SHA256",
                            "ECDHE-ECDSA-AES256-GCM-SHA384",
                            "ECDHE-RSA-AES256-GCM-SHA384",
                            "DHE-RSA-AES128-GCM-SHA256",
                            "DHE-RSA-AES256-GCM-SHA384",
                            "ECDHE-ECDSA-AES128-SHA256",
                            "ECDHE-ECDSA-AES128-SHA",
                            "ECDHE-RSA-AES128-SHA",
                            "ECDHE-ECDSA-AES256-SHA384",
                            "ECDHE-ECDSA-AES256-SHA",
                            "ECDHE-RSA-AES256-SHA",
                            "DHE-RSA-AES128-SHA256",
                            "DHE-RSA-AES128-SHA",
                            "DHE-RSA-AES256-SHA256",
                            "DHE-RSA-AES256-SHA",
                            "AES128-SHA",
                            "AES256-SHA"));
                break;
            case CipherSuitesLimitType.DEFAULT:
            default:
                referenceList = givenSuites.stream().map(CipherSuite::getIanaName).collect(Collectors.toSet());
                break;
        }
        Set<String> finalReferenceList = referenceList;
        List<CipherSuite> effectiveSuites = givenSuites
                .stream()
                .filter(gs -> finalReferenceList.contains(gs.getIanaName()))
                .toList();
        return effectiveSuites;
    }

    private static List<CipherSuite> getCipherSuiteList() {
        return List.of(
                new CipherSuite("TLS_AES_256_GCM_SHA384", "TLS_AES_256_GCM_SHA384", Set.of(TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-PSK-CHACHA20-POLY1305", "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-PSK-AES256-GCM-SHA384", "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-AES256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-AES128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("TLS_AES_128_CCM_SHA256", "TLS_AES_128_CCM_SHA256", Set.of(TLS_1_3)),
                new CipherSuite("TLS_CHACHA20_POLY1305_SHA256", "TLS_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_3)),
                new CipherSuite("TLS_AES_128_GCM_SHA256", "TLS_AES_128_GCM_SHA256", Set.of(TLS_1_3)),
                new CipherSuite("TLS_AES_128_CCM_8_SHA256", "TLS_AES_128_CCM_8_SHA256", Set.of(TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-CAMELLIA128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-ARIA256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-ARIA128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-PSK-AES128-GCM-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-CHACHA20-POLY1305", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-CAMELLIA256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECCPWD-AES128-GCM-SHA256", "TLS_ECCPWD_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECCPWD-AES256-GCM-SHA384", "TLS_ECCPWD_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-ARIA128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECCPWD-AES256-CCM-SHA384", "TLS_ECCPWD_WITH_AES_256_CCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES256-CCM-8", "TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES256-CCM", "TLS_ECDHE_ECDSA_WITH_AES_256_CCM", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES128-CCM-8", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES128-CCM", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-CHACHA20-POLY1305", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-CAMELLIA256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-CAMELLIA128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-ARIA256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECCPWD-AES128-CCM-SHA256", "TLS_ECCPWD_WITH_AES_128_CCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-PSK-AES128-CCM-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-PSK-AES128-CCM-8-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("DHE-RSA-AES128-GCM-SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("DHE-RSA-AES256-GCM-SHA384", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES128-SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES256-SHA", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES128-SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-ECDSA-AES256-SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("DHE-RSA-AES128-SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("DHE-RSA-AES256-SHA256", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-AES128-SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("ECDHE-RSA-AES256-SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("DHE-RSA-AES128-SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("DHE-RSA-AES256-SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("AES128-SHA", "TLS_RSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                new CipherSuite("AES256-SHA", "TLS_RSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)));
    }

    public enum CipherSuitesLimitType {
        DEFAULT,
        MINIMAL,
        REDHAT_VERSION8,
        BLACKBOX_EXPORTER,
        OPENSSL_INTERMEDIATE2018,
        JAVA_INTERMEDIATE2018,
    }
}
