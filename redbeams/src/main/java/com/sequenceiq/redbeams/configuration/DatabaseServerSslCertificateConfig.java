package com.sequenceiq.redbeams.configuration;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Configuration
@ConfigurationProperties(prefix = "redbeams.ssl")
public class DatabaseServerSslCertificateConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateConfig.class);

    private static final String CERT_LIST_SEPARATOR_REGEX = ";";

    private static final int GROUP_VERSION = 1;

    private static final int GROUP_CLOUD_PROVIDER_IDENTIFIER = 2;

    private static final int GROUP_CERT_PEM = 3;

    private static final Pattern CERT_ENTRY_PATTERN = Pattern.compile("([-+]?[0-9]+):([^:]+):([^:]+)");

    // See 20210219153728_CB-10722_Set_AWS_root_cert_ID_when_creating_RDS.sql
    private static final int CLOUD_PROVIDER_IDENTIFIER_MAX_LENGTH = 255;

    private static final int NO_CERTS = 0;

    private static final int DUMMY_VERSION = Integer.MIN_VALUE;

    private static final Map<String, Integer> CERT_LEGACY_MAX_VERSIONS_BY_CLOUD_PLATFORM = Map.ofEntries(
            Map.entry(CloudPlatform.AWS.toString().toLowerCase(), 0),
            Map.entry(CloudPlatform.AZURE.toString().toLowerCase(), 1));

    private static final Map<String, String> CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIERS_BY_CLOUD_PLATFORM = Map.ofEntries(
            Map.entry(CloudPlatform.AWS.toString().toLowerCase(), "rds-ca-2019"),
            Map.entry(CloudPlatform.AZURE.toString().toLowerCase(), "DigiCertGlobalRootG2"));

    // Must not be renamed or made final, gets injected from application properties
    private Map<String, String> certs = new HashMap<>();

    private Map<String, Set<SslCertificateEntry>> certsByCloudPlatformCache = new HashMap<>();

    private Map<String, Map<Integer, SslCertificateEntry>> certsByCloudPlatformByVersionCache = new HashMap<>();

    private Map<String, Map<String, SslCertificateEntry>> certsByCloudPlatformByCloudProviderIdentifierCache = new HashMap<>();

    private Map<String, Integer> certMinVersionsByCloudPlatformCache = new HashMap<>();

    private Map<String, Integer> certMaxVersionsByCloudPlatformCache = new HashMap<>();

    @PostConstruct
    public void setupCertsCache() {
        certsByCloudPlatformCache = certs.entrySet()
                .stream()
                .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e -> buildCertsSet(e.getKey(), e.getValue())));
        certsByCloudPlatformByVersionCache = certsByCloudPlatformCache.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> buildCertsByVersionMap(e.getValue())));
        validateCloudProviderIdentifierUniqueness();
        certsByCloudPlatformByCloudProviderIdentifierCache = certsByCloudPlatformCache.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> buildCertsByCloudProviderIdentifierMap(e.getValue())));
        certMinVersionsByCloudPlatformCache = certsByCloudPlatformByVersionCache.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> getCertMinVersion(e.getKey(), e.getValue())));
        certMaxVersionsByCloudPlatformCache = certsByCloudPlatformByVersionCache.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> getCertMaxVersion(e.getKey(), e.getValue())));

        validateCertPemUniqueness();
        validateCertVersionRanges();
        logCerts();
    }

    private Set<SslCertificateEntry> buildCertsSet(String cloudPlatform, String entries) {
        List<SslCertificateEntry> certsList = Arrays.stream(entries.split(CERT_LIST_SEPARATOR_REGEX))
                .map(String::trim)
                .map(e -> parseCertEntry(cloudPlatform, e))
                .collect(Collectors.toList());
        Set<SslCertificateEntry> result = new HashSet<>();
        // Not checking for global uniqueness; it is permitted to have common versions for multiple cloud providers.
        certsList.forEach(e -> {
            if (!result.add(e)) {
                throw new IllegalArgumentException(
                        String.format("Duplicated SSL certificate version %d for cloud platform \"%s\"", e.getVersion(), cloudPlatform));
            }
        });
        return result;
    }

    private SslCertificateEntry parseCertEntry(String cloudPlatform, String entry) {
        Matcher matcher = CERT_ENTRY_PATTERN.matcher(entry);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Malformed SSL certificate entry for cloud platform \"%s\": \"%s\"", cloudPlatform, entry));
        }
        int version = Integer.parseInt(matcher.group(GROUP_VERSION));
        String cloudProviderIdentifier = matcher.group(GROUP_CLOUD_PROVIDER_IDENTIFIER);
        validateCloudProviderIdentifierFormat(cloudPlatform, cloudProviderIdentifier);
        String certPem = matcher.group(GROUP_CERT_PEM);
        X509Certificate x509Cert = extractX509Certificate(cloudPlatform, certPem);
        return new SslCertificateEntry(version, cloudProviderIdentifier, certPem, x509Cert);
    }

    private void validateCloudProviderIdentifierFormat(String cloudPlatform, String cloudProviderIdentifier) {
        if (Strings.isNullOrEmpty(cloudProviderIdentifier) || cloudProviderIdentifier.length() > CLOUD_PROVIDER_IDENTIFIER_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("Malformed SSL certificate CloudProviderIdentifier for cloud platform \"%s\": \"%s\". " +
                            "It should not be empty and its length must be less than %d characters.",
                    cloudPlatform, cloudProviderIdentifier, CLOUD_PROVIDER_IDENTIFIER_MAX_LENGTH));
        }
    }

    private X509Certificate extractX509Certificate(String cloudPlatform, String certPem) {
        try {
            return PkiUtil.fromCertificatePem(certPem);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error parsing SSL certificate PEM for cloud platform \"%s\": \"%s\"", cloudPlatform, certPem), e);
        }
    }

    private Map<Integer, SslCertificateEntry> buildCertsByVersionMap(Set<SslCertificateEntry> certsSet) {
        return certsSet
                .stream()
                .collect(Collectors.toMap(SslCertificateEntry::getVersion, Function.identity()));
    }

    private Map<String, SslCertificateEntry> buildCertsByCloudProviderIdentifierMap(Set<SslCertificateEntry> certsSet) {
        return certsSet
                .stream()
                .collect(Collectors.toMap(SslCertificateEntry::getCloudProviderIdentifier, Function.identity()));
    }

    private IllegalStateException createIseForEmptyCertsByVersionMap(String cloudPlatform) {
        return new IllegalStateException(String.format("Empty certsByVersionMap encountered for cloud platform \"%s\"", cloudPlatform));
    }

    private int getCertMinVersion(String cloudPlatform, Map<Integer, SslCertificateEntry> certsByVersionMap) {
        return certsByVersionMap.keySet()
                .stream()
                .min(Integer::compareTo)
                .orElseThrow(() -> createIseForEmptyCertsByVersionMap(cloudPlatform));
    }

    private int getCertMaxVersion(String cloudPlatform, Map<Integer, SslCertificateEntry> certsByVersionMap) {
        return certsByVersionMap.keySet()
                .stream()
                .max(Integer::compareTo)
                .orElseThrow(() -> createIseForEmptyCertsByVersionMap(cloudPlatform));
    }

    private void validateCloudProviderIdentifierUniqueness() {
        // Not checking for global uniqueness; it might theoretically happen in the future that multiple cloud providers use a common cert identifier.
        for (Map.Entry<String, Set<SslCertificateEntry>> entry : certsByCloudPlatformCache.entrySet()) {
            String cloudPlatform = entry.getKey();
            Set<String> cloudProviderIdentifierSet = new HashSet<>();
            entry.getValue().forEach(e -> {
                if (!cloudProviderIdentifierSet.add(e.getCloudProviderIdentifier())) {
                    throw new IllegalArgumentException(String.format("Duplicated SSL certificate CloudProviderIdentifier for cloud platform \"%s\": \"%s\"",
                            cloudPlatform, e.getCloudProviderIdentifier()));
                }
            });
        }
    }

    private void validateCertPemUniqueness() {
        // Not checking for global uniqueness; it might theoretically happen in the future that multiple cloud providers use a common cert.
        for (Map.Entry<String, Set<SslCertificateEntry>> entry : certsByCloudPlatformCache.entrySet()) {
            String cloudPlatform = entry.getKey();
            Set<String> certPemSet = new HashSet<>();
            entry.getValue().forEach(e -> {
                if (!certPemSet.add(e.getCertPem())) {
                    throw new IllegalArgumentException(
                            String.format("Duplicated SSL certificate PEM for cloud platform \"%s\": \"%s\"", cloudPlatform, e.getCertPem()));
                }
            });
        }
    }

    private void validateCertVersionRanges() {
        for (Map.Entry<String, Set<SslCertificateEntry>> entry : certsByCloudPlatformCache.entrySet()) {
            String cloudPlatform = entry.getKey();
            int numCerts = entry.getValue().size();
            int versionsRangeSize = certMaxVersionsByCloudPlatformCache.get(cloudPlatform) - certMinVersionsByCloudPlatformCache.get(cloudPlatform) + 1;
            if (numCerts != versionsRangeSize) {
                throw new IllegalArgumentException(String.format("SSL certificate versions are not contiguous for cloud platform \"%s\"", cloudPlatform));
            }
        }
    }

    private void logCerts() {
        int numCertsTotal = 0;
        for (Map.Entry<String, Set<SslCertificateEntry>> entry : certsByCloudPlatformCache.entrySet()) {
            String cloudPlatform = entry.getKey();
            int numCerts = entry.getValue().size();
            numCertsTotal += numCerts;
            int minVersion = certMinVersionsByCloudPlatformCache.get(cloudPlatform);
            int maxVersion = certMaxVersionsByCloudPlatformCache.get(cloudPlatform);
            LOGGER.info("Found SSL certificates registered for cloud platform \"{}\": numberOfCerts={}, minVersion={}, maxVersion={}, certs=\"{}\"",
                    cloudPlatform, numCerts, minVersion, maxVersion, entry.getValue());
        }
        LOGGER.info("Total number of SSL certificates registered: {}", numCertsTotal);
    }

    public SslCertificateEntry getCertByPlatformAndVersion(String cloudPlatform, int version) {
        Set<SslCertificateEntry> result = getCertsByPlatformAndVersions(cloudPlatform, version);
        return result.isEmpty() ? null : result.iterator().next();
    }

    public Set<SslCertificateEntry> getCertsByPlatformAndVersions(String cloudPlatform, int... versions) {
        if (versions == null) {
            return new HashSet<>();
        } else {
            Set<SslCertificateEntry> result = new LinkedHashSet<>(versions.length);
            Optional.ofNullable(cloudPlatform)
                    .filter(c -> !Strings.isNullOrEmpty(c))
                    .map(c -> certsByCloudPlatformByVersionCache.get(c.toLowerCase()))
                    .ifPresent(certsByVersionMap -> Arrays.stream(versions)
                            .mapToObj(certsByVersionMap::get)
                            .filter(Objects::nonNull)
                            .forEach(result::add));
            return result;
        }
    }

    public SslCertificateEntry getCertByPlatformAndCloudProviderIdentifier(String cloudPlatform, String cloudProviderIdentifier) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> certsByCloudPlatformByCloudProviderIdentifierCache.get(c.toLowerCase()))
                .map(certsByCloudProviderIdentifierMap -> certsByCloudProviderIdentifierMap.get(cloudProviderIdentifier))
                .orElse(null);
    }

    public int getNumberOfCertsByPlatform(String cloudPlatform) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> certsByCloudPlatformCache.get(c.toLowerCase()))
                .map(Set::size)
                .orElse(NO_CERTS);
    }

    public int getMinVersionByPlatform(String cloudPlatform) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> certMinVersionsByCloudPlatformCache.get(c.toLowerCase()))
                .orElse(DUMMY_VERSION);
    }

    public int getMaxVersionByPlatform(String cloudPlatform) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> certMaxVersionsByCloudPlatformCache.get(c.toLowerCase()))
                .orElse(DUMMY_VERSION);
    }

    public int getLegacyMaxVersionByPlatform(String cloudPlatform) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> CERT_LEGACY_MAX_VERSIONS_BY_CLOUD_PLATFORM.get(c.toLowerCase()))
                .orElse(DUMMY_VERSION);
    }

    public String getLegacyCloudProviderIdentifierByPlatform(String cloudPlatform) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIERS_BY_CLOUD_PLATFORM.get(c.toLowerCase()))
                .orElse(null);
    }

    public Map<String, String> getCerts() {
        return certs;
    }

    public Set<SslCertificateEntry> getCertsByPlatform(String cloudPlatform) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> certsByCloudPlatformCache.get(c.toLowerCase()))
                .orElse(new HashSet<>());
    }

    public Set<String> getSupportedPlatformsForCerts() {
        return new HashSet<>(certsByCloudPlatformCache.keySet());
    }

    public Set<String> getSupportedPlatformsForLegacyMaxVersion() {
        return new HashSet<>(CERT_LEGACY_MAX_VERSIONS_BY_CLOUD_PLATFORM.keySet());
    }

    public Set<String> getSupportedPlatformsForLegacyCloudProviderIdentifier() {
        return new HashSet<>(CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIERS_BY_CLOUD_PLATFORM.keySet());
    }

}
