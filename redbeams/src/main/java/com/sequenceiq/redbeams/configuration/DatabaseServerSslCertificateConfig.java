package com.sequenceiq.redbeams.configuration;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
public class DatabaseServerSslCertificateConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateConfig.class);

    private static final Pattern CLOUD_PLATFORM_AND_REGION_PATTERN = Pattern.compile("(aws|azure)(?:\\.([a-zA-Z0-9-]+))?");

    private static final int GROUP_CLOUD_PLATFORM = 1;

    private static final int GROUP_CLOUD_REGION = 2;

    private static final String CLOUD_PLATFORM_AND_REGION_FORMAT = "%s.%s";

    private static final String DEFAULT_REGION = ".default";

    private static final String CERT_FILE_PATH_SEPARATOR_REGEX = "/";

    private static final int GROUP_PROVIDER_INDEX_FROM_LAST = 3;

    private static final int GROUP_REGION_INDEX_FROM_LAST = 2;

    private static final int GROUP_CERT_INDEX_FROM_LAST = 1;

    private static final int GROUP_CERT_NAME = 1;

    private static final String CERT_FILE_EXTENSION = ".yml";

    // See 20210219153728_CB-10722_Set_AWS_root_cert_ID_when_creating_RDS.sql
    private static final int CLOUD_PROVIDER_IDENTIFIER_MAX_LENGTH = 255;

    private static final int NO_CERTS = 0;

    private static final int DUMMY_VERSION = Integer.MIN_VALUE;

    private static final String PLATFORM_AWS = CloudPlatform.AWS.toString().toLowerCase();

    private static final String PLATFORM_AZURE = CloudPlatform.AZURE.toString().toLowerCase();

    private static final ResourcePatternResolver PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();

    private static final Map<String, Integer> CERT_LEGACY_MAX_VERSIONS_BY_CLOUD_PLATFORM = Map.ofEntries(
            Map.entry(PLATFORM_AWS, 0),
            Map.entry(PLATFORM_AZURE, 1));

    private static final Map<String, String> CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIERS_BY_CLOUD_PLATFORM = Map.ofEntries(
            Map.entry(PLATFORM_AWS, "rds-ca-2019"),
            Map.entry(PLATFORM_AWS + ".eu-south-1", "rds-ca-2019-eu-south-1"),
            Map.entry(PLATFORM_AWS + ".af-south-1", "rds-ca-2019-af-south-1"),
            Map.entry(PLATFORM_AWS + ".me-south-1", "rds-ca-2019-me-south-1"),
            Map.entry(PLATFORM_AWS + ".ap-east-1", "rds-ca-rsa2048-g1"),
            Map.entry(PLATFORM_AWS + ".ap-southeast-3", "rds-ca-rsa2048-g1"),
            Map.entry(PLATFORM_AZURE, "DigiCertGlobalRootG2"));

    @Value("${cert.path:/certs}")
    private String certPath;

    // Must not be renamed or made final, gets injected from application properties
    private Map<String, String> certs = new HashMap<>();

    private Map<String, Set<SslCertificateEntry>> certsByCloudPlatformCache = new HashMap<>();

    private Map<String, Map<Integer, SslCertificateEntry>> certsByCloudPlatformByVersionCache = new HashMap<>();

    private Map<String, Map<String, SslCertificateEntry>> certsByCloudPlatformByCloudProviderIdentifierCache = new HashMap<>();

    private Map<String, Integer> certMinVersionsByCloudPlatformCache = new HashMap<>();

    private Map<String, Integer> certMaxVersionsByCloudPlatformCache = new HashMap<>();

    @PostConstruct
    public void setupCertsCache() {
        try {
            certs = Arrays.stream(PATTERN_RESOLVER.getResources("classpath:" + certPath + "/**/*" + CERT_FILE_EXTENSION))
                    .map(resource -> {
                        try {
                            String[] path = resource.getURL().getPath().split(certPath);
                            String key = String.format("%s%s", certPath, path[GROUP_CERT_NAME]);
                            String value = FileReaderUtils.readFileFromClasspath(key);
                            return Map.entry(key, value);
                        } catch (IOException e) {
                            LOGGER.error("Could not load certificates from classpath: " + e.getMessage(), e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            LOGGER.error("Could not load certificates from classpath: " + e.getMessage(), e);
        }

        certsByCloudPlatformCache = certs.entrySet()
                .stream()
                .map(e -> buildCert(e.getKey().toLowerCase(Locale.ROOT), e.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(SslCertificateEntry::cloudKey))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new HashSet<>(e.getValue())));

        validateCloudProviderIdentifierUniqueness();

        certsByCloudPlatformByVersionCache = certsByCloudPlatformCache.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> buildCertsByVersionMap(e.getValue())));
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

    private String prettyPrintCloudPlatform(String cloudPlatform) {
        Matcher matcher = CLOUD_PLATFORM_AND_REGION_PATTERN.matcher(cloudPlatform);
        if (matcher.matches()) {
            String cloudPlatformBase = matcher.group(GROUP_CLOUD_PLATFORM);
            String region = matcher.group(GROUP_CLOUD_REGION);
            return Strings.isNullOrEmpty(region) ? cloudPlatformBase + " (global)" : String.format("%s (region %s)", cloudPlatformBase, region);
        } else {
            LOGGER.warn("Ignoring malformed cloud platform \"{}\"", cloudPlatform);
            return cloudPlatform + " (malformed)";
        }
    }

    private SslCertificateEntry buildCert(String filePath, String fileContent) {
        SslCertificateEntry result = parseCertEntry(filePath, fileContent);
        if (result.deprecated()) {
            LOGGER.info("Ignoring deprecated SSL certificate for cloud platform \"{}\": cert=\"{}\"", prettyPrintCloudPlatform(result.cloudKey()), result);
            result = null;
        }
        return result;
    }

    SslCertificateEntry parseCertEntry(String filePath, String fileContent) {
        Representer representer = new Representer(new DumperOptions());
        representer.getPropertyUtils().setSkipMissingProperties(true);
        SslContent content = new Yaml(representer).loadAs(fileContent, SslContent.class);
        String[] filePathSegments = filePath.split(CERT_FILE_PATH_SEPARATOR_REGEX);
        String[] segments = Arrays.stream(filePathSegments)
                .filter(i -> !Strings.isNullOrEmpty(i))
                .toArray(String[]::new);
        if (segments.length < GROUP_PROVIDER_INDEX_FROM_LAST) {
            throw new IllegalStateException(String.format("Cert file path format is not valid: %s", filePath));
        }
        int version = Integer.parseInt(segments[segments.length - GROUP_CERT_INDEX_FROM_LAST]
                .replace(CERT_FILE_EXTENSION, ""));
        String cloudPlatform = segments[segments.length - GROUP_PROVIDER_INDEX_FROM_LAST];
        String cloudRegion = segments[segments.length - GROUP_REGION_INDEX_FROM_LAST];
        String cloudProviderIdentifier = content.getName();
        String certPem = content.getCert();
        String cloudKey = String.format(CLOUD_PLATFORM_AND_REGION_FORMAT, cloudPlatform, cloudRegion)
                .toLowerCase(Locale.ROOT).replace(DEFAULT_REGION, "");
        validateCloudProviderIdentifierFormat(cloudKey, cloudProviderIdentifier);
        X509Certificate x509Cert = extractX509Certificate(cloudKey, certPem);
        return new SslCertificateEntry(version, cloudKey, cloudProviderIdentifier, cloudPlatform, certPem, x509Cert, content.getFingerprint(),
                content.isDeprecated());
    }

    private void validateCloudProviderIdentifierFormat(String cloudPlatform, String cloudProviderIdentifier) {
        if (Strings.isNullOrEmpty(cloudProviderIdentifier) || cloudProviderIdentifier.length() > CLOUD_PROVIDER_IDENTIFIER_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("Malformed SSL certificate CloudProviderIdentifier for cloud platform \"%s\": \"%s\". " +
                            "It should not be empty and its length must be less than %d characters.",
                    prettyPrintCloudPlatform(cloudPlatform), cloudProviderIdentifier, CLOUD_PROVIDER_IDENTIFIER_MAX_LENGTH));
        }
    }

    private X509Certificate extractX509Certificate(String cloudPlatform, String certPem) {
        try {
            return PkiUtil.fromCertificatePem(certPem);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error parsing SSL certificate PEM for cloud platform \"%s\": \"%s\"", prettyPrintCloudPlatform(cloudPlatform), certPem), e);
        }
    }

    private Map<Integer, SslCertificateEntry> buildCertsByVersionMap(Set<SslCertificateEntry> certsSet) {
        return certsSet
                .stream()
                .collect(Collectors.toMap(SslCertificateEntry::version, Function.identity()));
    }

    private Map<String, SslCertificateEntry> buildCertsByCloudProviderIdentifierMap(Set<SslCertificateEntry> certsSet) {
        return certsSet
                .stream()
                .collect(Collectors.toMap(SslCertificateEntry::cloudProviderIdentifier, Function.identity()));
    }

    private IllegalStateException createEmptyCertsByVersionMapException(String cloudPlatform) {
        return new IllegalStateException(String.format("Empty certsByVersionMap encountered for cloud platform \"%s\"",
                prettyPrintCloudPlatform(cloudPlatform)));
    }

    private int getCertMinVersion(String cloudPlatform, Map<Integer, SslCertificateEntry> certsByVersionMap) {
        return certsByVersionMap.keySet()
                .stream()
                .min(Integer::compareTo)
                .orElseThrow(() -> createEmptyCertsByVersionMapException(cloudPlatform));
    }

    private int getCertMaxVersion(String cloudPlatform, Map<Integer, SslCertificateEntry> certsByVersionMap) {
        return certsByVersionMap.keySet()
                .stream()
                .max(Integer::compareTo)
                .orElseThrow(() -> createEmptyCertsByVersionMapException(cloudPlatform));
    }

    private void validateCloudProviderIdentifierUniqueness() {
        // Not checking for global uniqueness; it might theoretically happen in the future that multiple cloud providers use a common cert identifier.
        for (Map.Entry<String, Set<SslCertificateEntry>> entry : certsByCloudPlatformCache.entrySet()) {
            String cloudPlatform = entry.getKey();
            Set<String> cloudProviderIdentifierSet = new HashSet<>();
            entry.getValue().forEach(e -> {
                if (!cloudProviderIdentifierSet.add(e.cloudProviderIdentifier())) {
                    throw new IllegalArgumentException(String.format("Duplicated SSL certificate CloudProviderIdentifier for cloud platform \"%s\": \"%s\"",
                            prettyPrintCloudPlatform(cloudPlatform), e.cloudProviderIdentifier()));
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
                if (!certPemSet.add(e.certPem())) {
                    throw new IllegalArgumentException(
                            String.format("Duplicated SSL certificate PEM for cloud platform \"%s\": \"%s\"", prettyPrintCloudPlatform(cloudPlatform),
                                    e.certPem()));
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
                throw new IllegalArgumentException(String.format("SSL certificate versions are not contiguous for cloud platform \"%s\"",
                        prettyPrintCloudPlatform(cloudPlatform)));
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
                    prettyPrintCloudPlatform(cloudPlatform), numCerts, minVersion, maxVersion, entry.getValue());
        }
        LOGGER.info("Total number of SSL certificates registered: {}", numCertsTotal);
    }

    public SslCertificateEntry getCertByCloudPlatformAndRegionAndVersion(String cloudPlatform, String region, int version) {
        Set<SslCertificateEntry> result = getCertsByCloudPlatformAndRegionAndVersions(cloudPlatform, region, version);
        return result.isEmpty() ? null : result.iterator().next();
    }

    public Set<SslCertificateEntry> getCertsByCloudPlatformAndRegionAndVersions(String cloudPlatform, String region, int... versions) {
        if (versions == null) {
            return new HashSet<>();
        } else {
            Set<SslCertificateEntry> result = new LinkedHashSet<>(versions.length);
            Optional.ofNullable(cloudPlatform)
                    .filter(c -> !Strings.isNullOrEmpty(c))
                    .map(c -> getValueByCloudPlatformAndRegion(certsByCloudPlatformByVersionCache, c, region))
                    .ifPresent(certsByVersionMap -> Arrays.stream(versions)
                            .mapToObj(certsByVersionMap::get)
                            .filter(Objects::nonNull)
                            .forEach(result::add));
            return result;
        }
    }

    private <V> V getValueByCloudPlatformAndRegion(Map<String, V> valuesByCloudPlatformMap, String cloudPlatform, String region) {
        String cloudPlatformNormalized = cloudPlatform.toLowerCase();
        V globalValue = valuesByCloudPlatformMap.get(cloudPlatformNormalized);
        if (!Strings.isNullOrEmpty(region)) {
            String cloudPlatformAndRegion = String.format(CLOUD_PLATFORM_AND_REGION_FORMAT, cloudPlatformNormalized, region.toLowerCase());
            V regionalValue = valuesByCloudPlatformMap.get(cloudPlatformAndRegion);
            return regionalValue == null ? globalValue : regionalValue;
        } else {
            return globalValue;
        }
    }

    public SslCertificateEntry getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(String cloudPlatform, String region, String cloudProviderIdentifier) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(certsByCloudPlatformByCloudProviderIdentifierCache, c, region))
                .map(certsByCloudProviderIdentifierMap -> certsByCloudProviderIdentifierMap.get(cloudProviderIdentifier))
                .orElse(null);
    }

    public int getNumberOfCertsTotal() {
        return certsByCloudPlatformCache.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    public int getNumberOfCertsByCloudPlatformAndRegion(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(certsByCloudPlatformCache, c, region))
                .map(Set::size)
                .orElse(NO_CERTS);
    }

    public int getMinVersionByCloudPlatformAndRegion(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(certMinVersionsByCloudPlatformCache, c, region))
                .orElse(DUMMY_VERSION);
    }

    public int getMaxVersionByCloudPlatformAndRegion(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(certMaxVersionsByCloudPlatformCache, c, region))
                .orElse(DUMMY_VERSION);
    }

    public int getLegacyMaxVersionByCloudPlatformAndRegion(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(CERT_LEGACY_MAX_VERSIONS_BY_CLOUD_PLATFORM, c, region))
                .orElse(DUMMY_VERSION);
    }

    public String getLegacyCloudProviderIdentifierByCloudPlatformAndRegion(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(CERT_LEGACY_CLOUD_PROVIDER_IDENTIFIERS_BY_CLOUD_PLATFORM, c, region))
                .orElse(null);
    }

    public Map<String, String> getCerts() {
        return certs;
    }

    public Set<SslCertificateEntry> getCertsByCloudPlatformAndRegion(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(certsByCloudPlatformCache, c, region))
                .orElse(new HashSet<>());
    }

    public boolean isCloudPlatformAndRegionSupportedForCerts(String cloudPlatform, String region) {
        return Optional.ofNullable(cloudPlatform)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .map(c -> getValueByCloudPlatformAndRegion(certsByCloudPlatformCache, c, region))
                .isPresent();
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

    public void modifyMockProviderCertCache(String mockZone, Set<SslCertificateEntry> certs) {
        String key = "mock." + mockZone;
        Integer maxVersion = certs.stream().map(SslCertificateEntry::version).max(Integer::compare).orElse(0);
        Integer minVersion = certs.stream().map(SslCertificateEntry::version).min(Integer::compare).orElse(0);

        certsByCloudPlatformCache.put(key, certs);
        certMaxVersionsByCloudPlatformCache.put(key, maxVersion);
        certMinVersionsByCloudPlatformCache.put(key, minVersion);
        certsByCloudPlatformByVersionCache.put(key, buildCertsByVersionMap(certs));
        certsByCloudPlatformByCloudProviderIdentifierCache.put(key,  buildCertsByCloudProviderIdentifierMap(certs));
    }

}
