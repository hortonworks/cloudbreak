package com.sequenceiq.cloudbreak.tls;

import static com.sequenceiq.cloudbreak.tls.CipherSuite.cipherSuite;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class EncryptionProfileProvider {

    public String getTlsVersions(String separator) {
        return getTlsVersions(null, separator);
    }

    public String getTlsVersions(Set<String> userTlsVersions, String separator) {
        String[] tlsVersions;
        if (userTlsVersions != null && !userTlsVersions.isEmpty()) {
            tlsVersions = userTlsVersions.stream().sorted().toArray(String[]::new);
        } else {
            tlsVersions = new String[] {
                    TLS_1_2.getVersion(),
                    TLS_1_3.getVersion()
            };
        }
        return String.join(separator, tlsVersions);
    }

    public String getCipherSuiteString(CipherSuitesLimitType cipherSuitesLimitType, String separator) {
        String[] referenceList = getDefaultCipherSuiteList(cipherSuitesLimitType);
        return String.join(separator, referenceList);
    }

    public String[] getDefaultCipherSuiteList(CipherSuitesLimitType cipherSuitesLimitType) {
        List<String> referenceList;
        switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL:
            case CipherSuitesLimitType.REDHAT_VERSION8:
                referenceList = List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
                break;
            case CipherSuitesLimitType.BLACKBOX_EXPORTER:
                referenceList = List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
                break;
            case CipherSuitesLimitType.JAVA_INTERMEDIATE2018:
                referenceList = List.of(
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
                        "TLS_RSA_WITH_AES_256_CBC_SHA");
                break;
            case CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018:
                referenceList = List.of(
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
                        "AES256-SHA");
                break;
            case CipherSuitesLimitType.DEFAULT:
            default:
                referenceList = List.of(
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
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
                break;
        }
        return referenceList.toArray(new String[0]);

    }

    public String getTlsCipherSuites(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType, String separator,
            boolean useIanaNames) {
        String[] userCipherSuits = getCipherSuitAsArray(userEncryptionProfileMap);
        return getTlsCipherSuites(userCipherSuits, cipherSuitesLimitType, separator, useIanaNames);
    }

    public List<String> getTlsCipherSuitesIanaList(String[] userSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        if (userSuites == null || userSuites.length == 0) {
            userSuites = getDefaultCipherSuiteList(cipherSuitesLimitType);
        }
        List<CipherSuite> givenSuites = validateCipherSuites(Arrays.stream(userSuites).toList());
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        return effectiveSuites.stream().map(CipherSuite::ianaName).collect(Collectors.toList());
    }

    public List<String> getTlsCipherSuitesIanaList(Map<String, List<String>> userEncryptionProfileMap, CipherSuitesLimitType cipherSuitesLimitType) {
        String[] userCipherSuits = getCipherSuitAsArray(userEncryptionProfileMap);
        return getTlsCipherSuitesIanaList(userCipherSuits, cipherSuitesLimitType);
    }

    private String getTlsCipherSuites(String[] userSuites, CipherSuitesLimitType cipherSuitesLimitType, String separator, boolean useIanaNames) {
        if (userSuites == null || userSuites.length == 0) {
            userSuites = getDefaultCipherSuiteList(cipherSuitesLimitType);
        }
        List<CipherSuite> givenSuites = validateCipherSuites(Arrays.stream(userSuites).toList());
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        if (useIanaNames) {
            return effectiveSuites.stream().map(CipherSuite::ianaName).collect(Collectors.joining(separator));
        } else {
            return effectiveSuites.stream().map(CipherSuite::name).collect(Collectors.joining(separator));
        }
    }

    public String getTlsCipherSuites(String[] suites, CipherSuitesLimitType cipherSuitesLimitType, String separator) {
        return getTlsCipherSuites(suites, cipherSuitesLimitType, separator, false);
    }

    public Map<String, List<String>> getAllCipherSuitesAvailableByTlsVersion() {
        return getCipherSuiteList()
                .stream()
                .flatMap(cipher -> cipher.tlsVersions()
                        .stream()
                        .map(tlsVersion -> Map.entry(tlsVersion, cipher.ianaName())))
                .collect(Collectors.groupingBy(entry ->
                        entry.getKey().getVersion(), TreeMap::new, Collectors.mapping(Entry::getValue, Collectors.toList())));
    }

    public Map<String, List<String>> getRecommendedCipherSuites() {
        return Map.of(
                TLS_1_2.getVersion(),

                List.of(
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA"
                ),
                TLS_1_3.getVersion(),
                List.of(
                        "TLS_AES_256_GCM_SHA384",
                        "TLS_AES_128_GCM_SHA256",
                        "TLS_CHACHA20_POLY1305_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA"
                )
        );
    }

    private String[] getCipherSuitAsArray(Map<String, List<String>> userEncryptionProfileMap) {
        String[] userCipherSuits = null;
        if (userEncryptionProfileMap != null && !userEncryptionProfileMap.isEmpty()) {
            userCipherSuits = userEncryptionProfileMap
                    .entrySet()
                    .stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .flatMap(List::stream)
                    .toArray(String[]::new);
        }
        return userCipherSuits;
    }

    private List<CipherSuite> validateCipherSuites(List<String> suites) {
        Map<String, CipherSuite> validCipherSuites = getCipherSuiteList().stream()
                .collect(Collectors.toMap(CipherSuite::ianaName, cs -> cs));

        return suites.stream()
                .filter(validCipherSuites::containsKey)
                .map(validCipherSuites::get)
                .toList();
    }

    private List<CipherSuite> getEffectiveCipherSuites(List<CipherSuite> givenSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        List<String> referenceList = new ArrayList<>();
        switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL:
            case CipherSuitesLimitType.REDHAT_VERSION8:
                referenceList = List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
                break;
            case CipherSuitesLimitType.BLACKBOX_EXPORTER:
                referenceList = List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
                break;
            case CipherSuitesLimitType.JAVA_INTERMEDIATE2018:
                referenceList = List.of(
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
                            "TLS_RSA_WITH_AES_256_CBC_SHA");
                break;
            case CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018:
                referenceList = List.of(
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
                            "AES256-SHA");
                break;
            case CipherSuitesLimitType.DEFAULT:
            default:
                referenceList = givenSuites.stream().map(CipherSuite::ianaName).toList();
                break;
        }
        List<String> finalReferenceList = referenceList;
        List<CipherSuite> effectiveSuites = givenSuites
                .stream()
                .filter(gs -> finalReferenceList.contains(gs.ianaName()))
                .toList();
        return effectiveSuites;
    }

    private List<CipherSuite> getCipherSuiteList() {
        return List.of(
                cipherSuite("TLS_AES_256_GCM_SHA384", "TLS_AES_256_GCM_SHA384", Set.of(TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-PSK-CHACHA20-POLY1305", "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-PSK-AES256-GCM-SHA384", "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-AES256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-AES128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("TLS_AES_128_CCM_SHA256", "TLS_AES_128_CCM_SHA256", Set.of(TLS_1_3)),
                cipherSuite("TLS_CHACHA20_POLY1305_SHA256", "TLS_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_3)),
                cipherSuite("TLS_AES_128_GCM_SHA256", "TLS_AES_128_GCM_SHA256", Set.of(TLS_1_3)),
                cipherSuite("TLS_AES_128_CCM_8_SHA256", "TLS_AES_128_CCM_8_SHA256", Set.of(TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-CAMELLIA128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-ARIA256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-ARIA128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-PSK-AES128-GCM-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-CHACHA20-POLY1305", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-CAMELLIA256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECCPWD-AES128-GCM-SHA256", "TLS_ECCPWD_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECCPWD-AES256-GCM-SHA384", "TLS_ECCPWD_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-ARIA128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECCPWD-AES256-CCM-SHA384", "TLS_ECCPWD_WITH_AES_256_CCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES256-CCM-8", "TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES256-CCM", "TLS_ECDHE_ECDSA_WITH_AES_256_CCM", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES128-CCM-8", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES128-CCM", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-CHACHA20-POLY1305", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-CAMELLIA256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-CAMELLIA128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-ARIA256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECCPWD-AES128-CCM-SHA256", "TLS_ECCPWD_WITH_AES_128_CCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-PSK-AES128-CCM-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-PSK-AES128-CCM-8-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("DHE-RSA-AES128-GCM-SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("DHE-RSA-AES256-GCM-SHA384", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES128-SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES256-SHA", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES128-SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-ECDSA-AES256-SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("DHE-RSA-AES128-SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("DHE-RSA-AES256-SHA256", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-AES128-SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("ECDHE-RSA-AES256-SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("DHE-RSA-AES128-SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("DHE-RSA-AES256-SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("AES128-SHA", "TLS_RSA_WITH_AES_128_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)),
                cipherSuite("AES256-SHA", "TLS_RSA_WITH_AES_256_CBC_SHA", Set.of(TLS_1_2, TLS_1_3)));
    }

    public List<String> convertCipherSuitesToIana(List<String> inputCipherSuites) {
        if (inputCipherSuites == null || inputCipherSuites.isEmpty()) {
            return inputCipherSuites;
        }

        List<String> result = new ArrayList<>();
        List<String> notAllowedCipherSuites = new ArrayList<>();
        List<CipherSuite> allowedCipherSuites = getCipherSuiteList();

        for (String input : inputCipherSuites) {
            Optional<String> ianaName = allowedCipherSuites
                    .stream()
                    .filter(cs -> cs.ianaName().equals(input) || cs.name().equals(input))
                    .map(CipherSuite::ianaName)
                    .findFirst();
            if (ianaName.isPresent()) {
                result.add(ianaName.get());
            } else {
                notAllowedCipherSuites.add(input);
            }
        }

        if (!notAllowedCipherSuites.isEmpty()) {
            throw new IllegalArgumentException("The following cipher(s) are not allowed: " + notAllowedCipherSuites);
        }

        return result;
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
