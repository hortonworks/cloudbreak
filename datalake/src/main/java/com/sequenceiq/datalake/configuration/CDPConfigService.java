package com.sequenceiq.datalake.configuration;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class CDPConfigService {

    public static final int RUNTIME_GROUP = 1;

    public static final int CLOUDPLATFORM_GROUP = 2;

    public static final int CLUSTERSHAPE_GROUP = 3;

    public static final long DEFAULT_WORKSPACE_ID = 0;

    @VisibleForTesting
    static final Pattern RESOURCE_TEMPLATE_PATTERN = Pattern.compile(".*/duties/(.[\\d.?]+]*)/([a-z]+)/([a-z_]+)+\\.json");

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPConfigService.class);

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    private static final String RUNTIME_SUPPORTIG_CENTOS7_AND_RHEL8 = "7.2.17";

    // Defines what is the default runtime version (UI / API)
    @Value("${datalake.runtimes.default}")
    private String defaultRuntime;

    // Defines what versions shall be advertised for the UI, if it is empty then it shall be the same as supported versions
    @Value("${datalake.runtimes.advertised}")
    private Set<String> advertisedRuntimes;

    // Defines what versions are actually supported
    @Value("${datalake.runtimes.supported}")
    private Set<String> supportedRuntimes;

    private final Map<CDPConfigKey, String> cdpStackRequests = new HashMap<>();

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ProviderPreferencesService preferencesService;

    @Inject
    private CommonGovService commonGovService;

    @PostConstruct
    public void initCdpStackRequests() {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath:duties/*/**/*.json");
            for (Resource resource : resources) {
                Matcher matcher = RESOURCE_TEMPLATE_PATTERN.matcher(resource.getURL().getPath());
                if (matcher.find()) {
                    String runtimeVersion = matcher.group(RUNTIME_GROUP);
                    if (supportedRuntimes.isEmpty() || supportedRuntimes.contains(runtimeVersion)) {
                        CloudPlatform cloudPlatform = CloudPlatform.valueOf(matcher.group(CLOUDPLATFORM_GROUP).toUpperCase(Locale.ROOT));
                        String clusterShapeString = matcher.group(CLUSTERSHAPE_GROUP).toUpperCase(Locale.ROOT);
                        Architecture architecture = Architecture.X86_64;
                        if (clusterShapeString.contains("_ARM")) {
                            clusterShapeString = clusterShapeString.replace("_ARM", "");
                            architecture = Architecture.ARM64;
                        }
                        SdxClusterShape sdxClusterShape = SdxClusterShape.valueOf(clusterShapeString);
                        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, sdxClusterShape, runtimeVersion, architecture);
                        String templateString = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
                        if (!cdpStackRequests.containsKey(cdpConfigKey)) {
                            cdpStackRequests.put(cdpConfigKey, templateString);
                        }
                    }
                }
            }
            LOGGER.info("Cdp configs for datalakes: {}", cdpStackRequests);
        } catch (IOException e) {
            LOGGER.error("Can't read CDP template files", e);
            throw new IllegalStateException("Can't read CDP template files", e);
        }
    }

    public StackV4Request getConfigForKey(CDPConfigKey cdpConfigKey) {
        try {
            String cdpStackRequest = cdpStackRequests.get(cdpConfigKey);
            if (cdpStackRequest == null) {
                return null;
            } else {
                return JsonUtil.readValue(cdpStackRequest, StackV4Request.class);
            }
        } catch (IOException e) {
            LOGGER.error("Can't convert json to StackV4Request", e);
            throw new IllegalStateException("Can't convert json to StackV4Request", e);
        }
    }

    public String getDefaultRuntime() {
        if (Strings.isNullOrEmpty(defaultRuntime)) {
            return getDatalakeVersions(null, null).stream().findFirst()
                    .orElseThrow(() -> new CloudbreakServiceException("Runtime not found in the default image catalog"));
        } else {
            return defaultRuntime;
        }
    }

    public List<String> getDatalakeVersions(String cloudPlatform, String os) {
        List<String> runtimeVersions = getRuntimeVersions(cloudPlatform);
        boolean govCloudDeployment = commonGovService.govCloudDeployment(
                preferencesService.enabledGovPlatforms(),
                preferencesService.enabledPlatforms());
        if (Strings.isNullOrEmpty(defaultRuntime)) {
            List<String> defaultImageCatalogRuntimeVersions = imageCatalogService.getDefaultImageCatalogRuntimeVersions(DEFAULT_WORKSPACE_ID);
            LOGGER.debug("Generate runtime versions by checking available runtimes in the default image catalog ('{}').",
                    String.join(",", defaultImageCatalogRuntimeVersions));
            runtimeVersions = runtimeVersions.stream()
                    .filter(defaultImageCatalogRuntimeVersions::contains)
                    .collect(Collectors.toList());
        }
        if (govCloudDeployment) {
            runtimeVersions = runtimeVersions.stream()
                    .filter(e -> commonGovService.govCloudCompatibleVersion(e))
                    .collect(Collectors.toList());
        } else {
            runtimeVersions = supportedOsFilter(runtimeVersions, os);
        }

        LOGGER.debug("Available runtime versions for datalake: {}", runtimeVersions);
        return runtimeVersions;
    }

    private List<String> supportedOsFilter(List<String> runtimeVersions, String os) {
        List<String> ret;
        if (StringUtils.isBlank(os)) {
            ret = runtimeVersions;
        } else if (RHEL8.getOs().equals(os)) {
            ret = runtimeVersions.stream()
                    .filter(r -> VERSION_COMPARATOR.compare(() -> r, () -> RUNTIME_SUPPORTIG_CENTOS7_AND_RHEL8) >= 0).collect(Collectors.toList());
        } else if (CENTOS7.getOs().equals(os)) {
            ret = runtimeVersions.stream()
                    .filter(r -> VERSION_COMPARATOR.compare(() -> r, () -> RUNTIME_SUPPORTIG_CENTOS7_AND_RHEL8) <= 0).collect(Collectors.toList());
        } else {
            ret = new ArrayList<>();
        }
        return ret;
    }

    private List<String> getRuntimeVersions(String cloudPlatform) {
        return cdpStackRequests.keySet().stream()
                .filter(filterByCloudPlatformIfPresent(cloudPlatform))
                .map(CDPConfigKey::getRuntimeVersion)
                .distinct()
                .map(version -> (Versioned) () -> version)
                .sorted(VERSION_COMPARATOR.reversed())
                .map(Versioned::getVersion)
                .collect(Collectors.toList());
    }

    private Predicate<CDPConfigKey> filterByCloudPlatformIfPresent(String cloudPlatform) {
        return cdpStack -> StringUtils.isEmpty(cloudPlatform) || cdpStack.getCloudPlatform().equalsIgnoreCase(cloudPlatform);
    }

    public List<AdvertisedRuntime> getAdvertisedRuntimes(String cloudPlatform, String os) {
        List<String> runtimeVersions = getDatalakeVersions(cloudPlatform, os).stream()
                .filter(runtimeVersion -> advertisedRuntimes.isEmpty() || advertisedRuntimes.contains(runtimeVersion)).collect(Collectors.toList());
        Optional<String> calculatedDefault
                = Strings.isNullOrEmpty(this.defaultRuntime) ? runtimeVersions.stream().findFirst() : Optional.ofNullable(this.defaultRuntime);

        List<AdvertisedRuntime> calculatedAdvertisedRuntimes = new ArrayList<>();
        for (String runtimeVersion : runtimeVersions) {
            AdvertisedRuntime advertisedRuntime = new AdvertisedRuntime();
            advertisedRuntime.setRuntimeVersion(runtimeVersion);
            if (calculatedDefault.map(r -> r.equals(runtimeVersion)).orElse(false)) {
                advertisedRuntime.setDefaultRuntimeVersion(true);
            }
            calculatedAdvertisedRuntimes.add(advertisedRuntime);
        }
        LOGGER.debug("Advertised runtime versions for datalake: {}", calculatedAdvertisedRuntimes);
        return calculatedAdvertisedRuntimes;
    }
}
