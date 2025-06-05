package com.sequenceiq.cloudbreak.tls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@Service
public class TlsSpecificationsHelper {

    private TlsSpecificationsHelper() {
    }

    public static String getTlsVersions(String separator) {
        String[] tlsVersions = new String [] {TlsVersion.TLS_1_2.getVersion(), TlsVersion.TLS_1_3.getVersion()};
        return String.join(separator, tlsVersions);
    }

    public static String getCipherSuiteString(CipherSuitesLimitType cipherSuitesLimitType, String separator) {
        String[] referenceList = getDefaultCipherSuiteList(cipherSuitesLimitType);
        return String.join(separator, referenceList);
    }

    public static String[] getDefaultCipherSuiteList(CipherSuitesLimitType cipherSuitesLimitType) {
        Set<String> referenceList = new HashSet<>();
        switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL:
            case CipherSuitesLimitType.REDHAT_VERSION8:
                referenceList = new HashSet<>(List.of(
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
                referenceList = new HashSet<>(List.of(
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
                referenceList = new HashSet<>(List.of(
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
                referenceList = new HashSet<>(List.of(
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
                referenceList = new HashSet<>(List.of(
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

    public static List<String> getTlsCipherSuitesIanaList(String[] suites, CipherSuitesLimitType cipherSuitesLimitType) {
        List<CipherSuite> givenSuites = validateCipherSuites(new HashSet<>(Arrays.stream(suites).toList()));
        List<CipherSuite> effectiveSuites = getEffectiveCipherSuites(givenSuites, cipherSuitesLimitType);
        return effectiveSuites.stream().map(CipherSuite::getIanaName).collect(Collectors.toList());
    }

    public static String getTlsCipherSuites(String[] suites, CipherSuitesLimitType cipherSuitesLimitType, String separator, boolean useIanaNames) {
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

    private static List<CipherSuite> validateCipherSuites(Set<String> suites) {
        List<CipherSuite> cipherSuites = List.of(
                new CipherSuite("ADH-AES128-GCM-SHA256", "TLS_DH_anon_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("ADH-AES128-SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA"),
                new CipherSuite("ADH-AES128-SHA256", "TLS_DH_anon_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("ADH-AES256-GCM-SHA384", "TLS_DH_anon_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("ADH-AES256-SHA", "TLS_DH_anon_WITH_AES_256_CBC_SHA"),
                new CipherSuite("ADH-AES256-SHA256", "TLS_DH_anon_WITH_AES_256_CBC_SHA256"),
                new CipherSuite("ADH-CAMELLIA128-SHA", "TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA"),
                new CipherSuite("ADH-CAMELLIA128-SHA256", "TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("ADH-CAMELLIA256-SHA", "TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA"),
                new CipherSuite("ADH-DES-CBC-SHA", "TLS_DH_anon_WITH_DES_CBC_SHA"),
                new CipherSuite("ADH-DES-CBC3-SHA", "TLS_DH_anon_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("ADH-RC4-MD5", "TLS_DH_anon_WITH_RC4_128_MD5"),
                new CipherSuite("ADH-SEED-SHA", "TLS_DH_anon_WITH_SEED_CBC_SHA"),
                new CipherSuite("AECDH-AES128-SHA", "TLS_ECDH_anon_WITH_AES_128_CBC_SHA"),
                new CipherSuite("AECDH-AES256-SHA", "TLS_ECDH_anon_WITH_AES_256_CBC_SHA"),
                new CipherSuite("AECDH-DES-CBC3-SHA", "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("AECDH-NULL-SHA", "TLS_ECDH_anon_WITH_NULL_SHA"),
                new CipherSuite("AECDH-RC4-SHA", "TLS_ECDH_anon_WITH_RC4_128_SHA"),
                new CipherSuite("AES128-CCM", "TLS_RSA_WITH_AES_128_CCM"),
                new CipherSuite("AES128-CCM8", "TLS_RSA_WITH_AES_128_CCM_8"),
                new CipherSuite("AES128-GCM-SHA256", "TLS_RSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("AES128-SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("AES128-SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("AES256-CCM", "TLS_RSA_WITH_AES_256_CCM"),
                new CipherSuite("AES256-CCM8", "TLS_RSA_WITH_AES_256_CCM_8"),
                new CipherSuite("AES256-GCM-SHA384", "TLS_RSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("AES256-SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("AES256-SHA256", "TLS_RSA_WITH_AES_256_CBC_SHA256"),
                new CipherSuite("CAMELLIA128-SHA", "TLS_RSA_WITH_CAMELLIA_128_CBC_SHA"),
                new CipherSuite("CAMELLIA128-SHA256", "TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("CAMELLIA256-SHA", "TLS_RSA_WITH_CAMELLIA_256_CBC_SHA"),
                new CipherSuite("DES-CBC-MD5", "SSL_CK_DES_64_CBC_WITH_MD5"),
                new CipherSuite("DES-CBC-SHA", "SSL_CK_DES_64_CBC_WITH_SHA"),
                new CipherSuite("DES-CBC-SHA", "TLS_RSA_WITH_DES_CBC_SHA"),
                new CipherSuite("DES-CBC3-MD5", "SSL_CK_DES_192_EDE3_CBC_WITH_MD5"),
                new CipherSuite("DES-CBC3-SHA", "SSL_CK_DES_192_EDE3_CBC_WITH_SHA"),
                new CipherSuite("DES-CBC3-SHA", "TLS_RSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("DES-CFB-M1", "SSL_CK_DES_64_CFB64_WITH_MD5_1"),
                new CipherSuite("DH-DSS-AES128-GCM-SHA256", "TLS_DH_DSS_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("DH-DSS-AES128-SHA", "TLS_DH_DSS_WITH_AES_128_CBC_SHA"),
                new CipherSuite("DH-DSS-AES128-SHA256", "TLS_DH_DSS_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("DH-DSS-AES256-GCM-SHA384", "TLS_DH_DSS_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("DH-DSS-AES256-SHA", "TLS_DH_DSS_WITH_AES_256_CBC_SHA"),
                new CipherSuite("DH-DSS-AES256-SHA256", "TLS_DH_DSS_WITH_AES_256_CBC_SHA256"),
                new CipherSuite("DH-DSS-CAMELLIA128-SHA", "TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA"),
                new CipherSuite("DH-DSS-CAMELLIA128-SHA256", "TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("DH-DSS-CAMELLIA256-SHA", "TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA"),
                new CipherSuite("DH-DSS-DES-CBC-SHA", "TLS_DH_DSS_WITH_DES_CBC_SHA"),
                new CipherSuite("DH-DSS-DES-CBC3-SHA", "TLS_DH_DSS_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("DH-DSS-SEED-SHA", "TLS_DH_DSS_WITH_SEED_CBC_SHA"),
                new CipherSuite("DH-RSA-AES128-GCM-SHA256", "TLS_DH_RSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("DH-RSA-AES128-SHA", "TLS_DH_RSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("DH-RSA-AES128-SHA256", "TLS_DH_RSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("DH-RSA-AES256-GCM-SHA384", "TLS_DH_RSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("DH-RSA-AES256-SHA", "TLS_DH_RSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("DH-RSA-AES256-SHA256", "TLS_DH_RSA_WITH_AES_256_CBC_SHA256"),
                new CipherSuite("DH-RSA-CAMELLIA128-SHA", "TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA"),
                new CipherSuite("DH-RSA-CAMELLIA128-SHA256", "TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("DH-RSA-CAMELLIA256-SHA", "TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA"),
                new CipherSuite("DH-RSA-DES-CBC-SHA", "TLS_DH_RSA_WITH_DES_CBC_SHA"),
                new CipherSuite("DH-RSA-DES-CBC3-SHA", "TLS_DH_RSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("DH-RSA-SEED-SHA", "TLS_DH_RSA_WITH_SEED_CBC_SHA"),
                new CipherSuite("DHE-DSS-AES128-GCM-SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("DHE-DSS-AES128-SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA"),
                new CipherSuite("DHE-DSS-AES128-SHA256", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("DHE-DSS-AES256-GCM-SHA384", "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("DHE-DSS-AES256-SHA", "TLS_DHE_DSS_WITH_AES_256_CBC_SHA"),
                new CipherSuite("DHE-DSS-AES256-SHA256", "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256"),
                new CipherSuite("DHE-DSS-CAMELLIA128-SHA", "TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA"),
                new CipherSuite("DHE-DSS-CAMELLIA128-SHA256", "TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("DHE-DSS-CAMELLIA256-SHA", "TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA"),
                new CipherSuite("DHE-DSS-RC4-SHA", "TLS_DHE_DSS_WITH_RC4_128_SHA"),
                new CipherSuite("DHE-DSS-SEED-SHA", "TLS_DHE_DSS_WITH_SEED_CBC_SHA"),
                new CipherSuite("DHE-PSK-AES128-CCM", "TLS_DHE_PSK_WITH_AES_128_CCM"),
                new CipherSuite("DHE-PSK-AES128-CCM8", "TLS_PSK_DHE_WITH_AES_128_CCM_8"),
                new CipherSuite("DHE-PSK-AES256-CCM", "TLS_DHE_PSK_WITH_AES_256_CCM"),
                new CipherSuite("DHE-PSK-AES256-CCM8", "TLS_PSK_DHE_WITH_AES_256_CCM_8"),
                new CipherSuite("DHE-PSK-CAMELLIA128-SHA256", "TLS_DHE_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("DHE-PSK-CAMELLIA256-SHA384", "TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("DHE-PSK-CHACHA20-POLY1305", "TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("DHE-PSK-NULL-SHA", "TLS_DHE_PSK_WITH_NULL_SHA"),
                new CipherSuite("DHE-RSA-AES128-CCM", "TLS_DHE_RSA_WITH_AES_128_CCM"),
                new CipherSuite("DHE-RSA-AES128-CCM8", "TLS_DHE_RSA_WITH_AES_128_CCM_8"),
                new CipherSuite("DHE-RSA-AES128-GCM-SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("DHE-RSA-AES128-SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("DHE-RSA-AES128-SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("DHE-RSA-AES256-CCM", "TLS_DHE_RSA_WITH_AES_256_CCM"),
                new CipherSuite("DHE-RSA-AES256-CCM8", "TLS_DHE_RSA_WITH_AES_256_CCM_8"),
                new CipherSuite("DHE-RSA-AES256-GCM-SHA384", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("DHE-RSA-AES256-SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("DHE-RSA-AES256-SHA256", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"),
                new CipherSuite("DHE-RSA-CAMELLIA128-SHA", "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA"),
                new CipherSuite("DHE-RSA-CAMELLIA128-SHA256", "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("DHE-RSA-CAMELLIA256-SHA", "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA"),
                new CipherSuite("DHE-RSA-CHACHA20-POLY1305", "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("DHE-RSA-CHACHA20-POLY1305-OLD", "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256_OLD"),
                new CipherSuite("DHE-RSA-SEED-SHA", "TLS_DHE_RSA_WITH_SEED_CBC_SHA"),
                new CipherSuite("ECDH-ECDSA-AES128-GCM-SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("ECDH-ECDSA-AES128-SHA", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("ECDH-ECDSA-AES128-SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("ECDH-ECDSA-AES256-GCM-SHA384", "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("ECDH-ECDSA-AES256-SHA", "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("ECDH-ECDSA-AES256-SHA384", "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384"),
                new CipherSuite("ECDH-ECDSA-CAMELLIA128-SHA256", "TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("ECDH-ECDSA-CAMELLIA256-SHA384", "TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("ECDH-ECDSA-DES-CBC3-SHA", "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("ECDH-ECDSA-NULL-SHA", "TLS_ECDH_ECDSA_WITH_NULL_SHA"),
                new CipherSuite("ECDH-ECDSA-RC4-SHA", "TLS_ECDH_ECDSA_WITH_RC4_128_SHA"),
                new CipherSuite("ECDH-RSA-AES128-GCM-SHA256", "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("ECDH-RSA-AES128-SHA", "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("ECDH-RSA-AES128-SHA256", "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("ECDH-RSA-AES256-GCM-SHA384", "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("ECDH-RSA-AES256-SHA", "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("ECDH-RSA-AES256-SHA384", "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384"),
                new CipherSuite("ECDH-RSA-CAMELLIA128-SHA256", "TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("ECDH-RSA-CAMELLIA256-SHA384", "TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("ECDH-RSA-DES-CBC3-SHA", "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("ECDH-RSA-NULL-SHA", "TLS_ECDH_RSA_WITH_NULL_SHA"),
                new CipherSuite("ECDH-RSA-RC4-SHA", "TLS_ECDH_RSA_WITH_RC4_128_SHA"),
                new CipherSuite("ECDHE-ECDSA-AES128-CCM", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM"),
                new CipherSuite("ECDHE-ECDSA-AES128-CCM8", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8"),
                new CipherSuite("ECDHE-ECDSA-AES128-GCM-SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("ECDHE-ECDSA-AES128-SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("ECDHE-ECDSA-AES128-SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("ECDHE-ECDSA-AES256-CCM", "TLS_ECDHE_ECDSA_WITH_AES_256_CCM"),
                new CipherSuite("ECDHE-ECDSA-AES256-CCM8", "TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8"),
                new CipherSuite("ECDHE-ECDSA-AES256-GCM-SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("ECDHE-ECDSA-AES256-SHA", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("ECDHE-ECDSA-AES256-SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384"),
                new CipherSuite("ECDHE-ECDSA-CAMELLIA128-SHA256", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("ECDHE-ECDSA-CAMELLIA256-SHA38", "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("ECDHE-ECDSA-CHACHA20-POLY1305", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("ECDHE-ECDSA-CHACHA20-POLY1305-OLD", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256_OLD"),
                new CipherSuite("ECDHE-ECDSA-DES-CBC3-SHA", "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("ECDHE-ECDSA-NULL-SHA", "TLS_ECDHE_ECDSA_WITH_NULL_SHA"),
                new CipherSuite("ECDHE-ECDSA-RC4-SHA", "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA"),
                new CipherSuite("ECDHE-PSK-3DES-EDE-CBC-SHA", "TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("ECDHE-PSK-AES128-CBC-SHA", "TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA"),
                new CipherSuite("ECDHE-PSK-AES128-CBC-SHA256", "TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("ECDHE-PSK-AES256-CBC-SHA", "TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA"),
                new CipherSuite("ECDHE-PSK-AES256-CBC-SHA384", "TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384"),
                new CipherSuite("ECDHE-PSK-CAMELLIA128-SHA256", "TLS_ECDHE_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("ECDHE-PSK-CAMELLIA256-SHA384", "TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("ECDHE-PSK-CHACHA20-POLY1305", "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("ECDHE-PSK-NULL-SHA", "TLS_ECDHE_PSK_WITH_NULL_SHA"),
                new CipherSuite("ECDHE-PSK-NULL-SHA256", "TLS_ECDHE_PSK_WITH_NULL_SHA256"),
                new CipherSuite("ECDHE-PSK-NULL-SHA384", "TLS_ECDHE_PSK_WITH_NULL_SHA384"),
                new CipherSuite("ECDHE-PSK-RC4-SHA", "TLS_ECDHE_PSK_WITH_RC4_128_SHA"),
                new CipherSuite("ECDHE-RSA-AES128-GCM-SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"),
                new CipherSuite("ECDHE-RSA-AES128-SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("ECDHE-RSA-AES128-SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"),
                new CipherSuite("ECDHE-RSA-AES256-GCM-SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                new CipherSuite("ECDHE-RSA-AES256-SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("ECDHE-RSA-AES256-SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"),
                new CipherSuite("ECDHE-RSA-CAMELLIA128-SHA256", "TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("ECDHE-RSA-CAMELLIA256-SHA384", "TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("ECDHE-RSA-CHACHA20-POLY1305", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("ECDHE-RSA-CHACHA20-POLY1305-OLD", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256_OLD"),
                new CipherSuite("ECDHE-RSA-DES-CBC3-SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("ECDHE-RSA-NULL-SHA", "TLS_ECDHE_RSA_WITH_NULL_SHA"),
                new CipherSuite("ECDHE-RSA-RC4-SHA", "TLS_ECDHE_RSA_WITH_RC4_128_SHA"),
                new CipherSuite("EDH-DSS-DES-CBC-SHA", "TLS_DHE_DSS_WITH_DES_CBC_SHA"),
                new CipherSuite("EDH-DSS-DES-CBC3-SHA", "TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("EDH-RSA-DES-CBC-SHA", "TLS_DHE_RSA_WITH_DES_CBC_SHA"),
                new CipherSuite("EDH-RSA-DES-CBC3-SHA", "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("EXP-ADH-DES-CBC-SHA", "TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA"),
                new CipherSuite("EXP-ADH-RC4-MD5", "TLS_DH_anon_EXPORT_WITH_RC4_40_MD5"),
                new CipherSuite("EXP-DES-CBC-SHA", "TLS_RSA_EXPORT_WITH_DES40_CBC_SHA"),
                new CipherSuite("EXP-DH-DSS-DES-CBC-SHA", "TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA"),
                new CipherSuite("EXP-DH-RSA-DES-CBC-SHA", "TLS_DH_RSA_EXPORT_WITH_DES40_CBC_SHA"),
                new CipherSuite("EXP-EDH-DSS-DES-CBC-SHA", "TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"),
                new CipherSuite("EXP-EDH-RSA-DES-CBC-SHA", "TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA"),
                new CipherSuite("EXP-KRB5-DES-CBC-MD5", "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5"),
                new CipherSuite("EXP-KRB5-DES-CBC-SHA", "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA"),
                new CipherSuite("EXP-KRB5-RC2-CBC-MD5", "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5"),
                new CipherSuite("EXP-KRB5-RC2-CBC-SHA", "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA"),
                new CipherSuite("EXP-KRB5-RC4-MD5", "TLS_KRB5_EXPORT_WITH_RC4_40_MD5"),
                new CipherSuite("EXP-KRB5-RC4-SHA", "TLS_KRB5_EXPORT_WITH_RC4_40_SHA"),
                new CipherSuite("EXP-RC2-CBC-MD5", "SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5"),
                new CipherSuite("EXP-RC2-CBC-MD5", "TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5"),
                new CipherSuite("EXP-RC4-MD5", "SSL_CK_RC4_128_EXPORT40_WITH_MD5"),
                new CipherSuite("EXP-RC4-MD5", "TLS_RSA_EXPORT_WITH_RC4_40_MD5"),
                new CipherSuite("EXP1024-DES-CBC-SHA", "TLS_RSA_EXPORT1024_WITH_DES_CBC_SHA"),
                new CipherSuite("EXP1024-DHE-DSS-DES-CBC-SHA", "TLS_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA"),
                new CipherSuite("EXP1024-DHE-DSS-RC4-SHA", "TLS_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA"),
                new CipherSuite("EXP1024-RC2-CBC-MD5", "TLS_RSA_EXPORT1024_WITH_RC2_CBC_56_MD5"),
                new CipherSuite("EXP1024-RC4-MD5", "TLS_RSA_EXPORT1024_WITH_RC4_56_MD5"),
                new CipherSuite("EXP1024-RC4-SHA", "TLS_RSA_EXPORT1024_WITH_RC4_56_SHA"),
                new CipherSuite("GOST-GOST94", "TLS_RSA_WITH_28147_CNT_GOST94"),
                new CipherSuite("GOST-MD5", "TLS_GOSTR341094_RSA_WITH_28147_CNT_MD5"),
                new CipherSuite("GOST2001-GOST89-GOST89", "TLS_GOSTR341094_WITH_NULL_GOSTR3411"),
                new CipherSuite("GOST2001-GOST89-GOST89", "TLS_GOSTR341001_WITH_28147_CNT_IMIT"),
                new CipherSuite("GOST94-GOST89-GOST89", "TLS_GOSTR341094_WITH_28147_CNT_IMIT"),
                new CipherSuite("GOST94-NULL-GOST94", "TLS_GOSTR341001_WITH_NULL_GOSTR3411"),
                new CipherSuite("IDEA-CBC-MD5", "SSL_CK_IDEA_128_CBC_WITH_MD5"),
                new CipherSuite("IDEA-CBC-SHA", "TLS_RSA_WITH_IDEA_CBC_SHA"),
                new CipherSuite("KRB5-DES-CBC-MD5", "TLS_KRB5_WITH_DES_CBC_MD5"),
                new CipherSuite("KRB5-DES-CBC-SHA", "TLS_KRB5_WITH_DES_CBC_SHA"),
                new CipherSuite("KRB5-DES-CBC3-MD5", "TLS_KRB5_WITH_3DES_EDE_CBC_MD5"),
                new CipherSuite("KRB5-DES-CBC3-SHA", "TLS_KRB5_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("KRB5-IDEA-CBC-MD5", "TLS_KRB5_WITH_IDEA_CBC_MD5"),
                new CipherSuite("KRB5-IDEA-CBC-SHA", "TLS_KRB5_WITH_IDEA_CBC_SHA"),
                new CipherSuite("KRB5-RC4-MD5", "TLS_KRB5_WITH_RC4_128_MD5"),
                new CipherSuite("KRB5-RC4-SHA", "TLS_KRB5_WITH_RC4_128_SHA"),
                new CipherSuite("NULL", "SSL_CK_NULL"),
                new CipherSuite("NULL-MD5", "TLS_RSA_WITH_NULL_MD5"),
                new CipherSuite("NULL-SHA", "TLS_RSA_WITH_NULL_SHA"),
                new CipherSuite("NULL-SHA256", "TLS_RSA_WITH_NULL_SHA256"),
                new CipherSuite("PSK-3DES-EDE-CBC-SHA", "TLS_PSK_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("PSK-AES128-CBC-SHA", "TLS_PSK_WITH_AES_128_CBC_SHA"),
                new CipherSuite("PSK-AES128-CCM", "TLS_PSK_WITH_AES_128_CCM"),
                new CipherSuite("PSK-AES128-CCM8", "TLS_PSK_WITH_AES_128_CCM_8"),
                new CipherSuite("PSK-AES256-CBC-SHA", "TLS_PSK_WITH_AES_256_CBC_SHA"),
                new CipherSuite("PSK-AES256-CCM", "TLS_PSK_WITH_AES_256_CCM"),
                new CipherSuite("PSK-AES256-CCM8", "TLS_PSK_WITH_AES_256_CCM_8"),
                new CipherSuite("PSK-CAMELLIA128-SHA256", "TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("PSK-CAMELLIA256-SHA384", "TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("PSK-CHACHA20-POLY1305", "TLS_PSK_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("PSK-NULL-SHA", "TLS_PSK_WITH_NULL_SHA"),
                new CipherSuite("PSK-RC4-SHA", "TLS_PSK_WITH_RC4_128_SHA"),
                new CipherSuite("RC2-CBC-MD5", "SSL_CK_RC2_128_CBC_WITH_MD5"),
                new CipherSuite("RC4-64-MD5", "SSL_CK_RC4_64_WITH_MD5"),
                new CipherSuite("RC4-MD5", "SSL_CK_RC4_128_WITH_MD5"),
                new CipherSuite("RC4-MD5", "TLS_RSA_WITH_RC4_128_MD5"),
                new CipherSuite("RC4-SHA", "TLS_RSA_WITH_RC4_128_SHA"),
                new CipherSuite("RSA-PSK-CAMELLIA128-SHA256", "TLS_RSA_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                new CipherSuite("RSA-PSK-CAMELLIA256-SHA384", "TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384"),
                new CipherSuite("RSA-PSK-CHACHA20-POLY1305", "TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("RSA-PSK-NULL-SHA", "TLS_RSA_PSK_WITH_NULL_SHA"),
                new CipherSuite("SEED-SHA", "TLS_RSA_WITH_SEED_CBC_SHA"),
                new CipherSuite("SRP-3DES-EDE-CBC-SHA", "TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("SRP-AES-128-CBC-SHA", "TLS_SRP_SHA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("SRP-AES-256-CBC-SHA", "TLS_SRP_SHA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("SRP-DSS-3DES-EDE-CBC-SHA", "TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("SRP-DSS-AES-128-CBC-SHA", "TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA"),
                new CipherSuite("SRP-DSS-AES-256-CBC-SHA", "TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA"),
                new CipherSuite("SRP-RSA-3DES-EDE-CBC-SHA", "TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA"),
                new CipherSuite("SRP-RSA-AES-128-CBC-SHA", "TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA"),
                new CipherSuite("SRP-RSA-AES-256-CBC-SHA", "TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA"),
                new CipherSuite("TLS_AES_128_CCM_8_SHA256", "TLS_AES_128_CCM_8_SHA256"),
                new CipherSuite("TLS_AES_128_CCM_SHA256", "TLS_AES_128_CCM_SHA256"),
                new CipherSuite("TLS_AES_128_GCM_SHA256", "TLS_AES_128_GCM_SHA256"),
                new CipherSuite("TLS_AES_256_GCM_SHA384", "TLS_AES_256_GCM_SHA384"),
                new CipherSuite("TLS_CHACHA20_POLY1305_SHA256", "TLS_CHACHA20_POLY1305_SHA256"),
                new CipherSuite("TLS_FALLBACK_SCSV", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV")
        );

        return cipherSuites.stream()
                .filter(cs -> suites.contains(cs.getIanaName()))
                .toList();
    }

    private static List<CipherSuite> getEffectiveCipherSuites(List<CipherSuite> givenSuites, CipherSuitesLimitType cipherSuitesLimitType) {
        Set<String> referenceList = new HashSet<>();
        switch (cipherSuitesLimitType) {
            case CipherSuitesLimitType.MINIMAL:
            case CipherSuitesLimitType.REDHAT_VERSION8:
                referenceList = new HashSet<>(List.of(
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"));
                break;
            case CipherSuitesLimitType.BLACKBOX_EXPORTER:
                referenceList = new HashSet<>(List.of(
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
                referenceList = new HashSet<>(List.of(
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
                referenceList = new HashSet<>(List.of(
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

    public enum CipherSuitesLimitType {
        DEFAULT,
        MINIMAL,
        REDHAT_VERSION8,
        BLACKBOX_EXPORTER,
        OPENSSL_INTERMEDIATE2018,
        JAVA_INTERMEDIATE2018,
    }
}
