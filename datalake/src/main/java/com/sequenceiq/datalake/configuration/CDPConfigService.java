package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class CDPConfigService {

    public static final int RUNTIME_GROUP = 1;

    public static final int CLOUDPLATFORM_GROUP = 2;

    public static final int CLUSTERSHAPE_GROUP = 3;

    public static final int PROFILER_GROUP = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPConfigService.class);

    // Defines what is the default runtime version (UI / API)
    @Value("${datalake.runtimes.default}")
    private String defaultRuntime;

    // Defines what versions shall be advertised for the UI, if it is empty then it shall be the same as supported versions
    @Value("${datalake.runtimes.advertised}")
    private Set<String> advertisedRuntimes;

    // Defines what versions are actually supported
    @Value("${datalake.runtimes.supported}")
    private Set<String> supportedRuntimes;

    private Map<CDPConfigKey, String> cdpStackRequests = new HashMap<>();

    @Inject
    private EntitlementService entitlementService;

    @PostConstruct
    public void initCdpStackRequests() {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath:duties/*/*/*.json");
            for (Resource resource : resources) {
                Matcher matcher = Pattern.compile(".*/duties/(.*)/(.*)/(.*?)(_profiler)?.json").matcher(resource.getURL().getPath());
                if (matcher.find()) {
                    String runtimeVersion = matcher.group(RUNTIME_GROUP);
                    if (supportedRuntimes.isEmpty() || supportedRuntimes.contains(runtimeVersion)) {
                        CloudPlatform cloudPlatform = CloudPlatform.valueOf(matcher.group(CLOUDPLATFORM_GROUP).toUpperCase());
                        SdxClusterShape sdxClusterShape = SdxClusterShape.valueOf(matcher.group(CLUSTERSHAPE_GROUP).toUpperCase());
                        boolean profilerPresent = matcher.group(PROFILER_GROUP) != null;
                        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, sdxClusterShape, runtimeVersion, profilerPresent);
                        String templateString = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8.name());
                        cdpStackRequests.put(cdpConfigKey, templateString);
                    }
                }
            }
            LOGGER.info("Cdp configs for datalakes: {}", cdpStackRequests);
        } catch (IOException e) {
            LOGGER.error("Can't read CDP template files", e);
            throw new IllegalStateException("Can't read CDP template files", e);
        }
    }

    @VisibleForTesting
    String getStackRequestForProfiler(CDPConfigKey cdpConfigKey) {
        String cdpStackRequest = null;
        if (cdpConfigKey.getClusterShape() == SdxClusterShape.MEDIUM_DUTY_HA) {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            boolean datalakeMediumDutyWithProfilerEnabled = entitlementService.isDatalakeMediumDutyWithProfilerEnabled(accountId);
            if (datalakeMediumDutyWithProfilerEnabled && !cdpConfigKey.isProfilerPresent()) {
                CDPConfigKey cdpConfigKeyWithProfiler = new CDPConfigKey(cdpConfigKey.getCloudPlatform(),
                        cdpConfigKey.getClusterShape(), cdpConfigKey.getRuntimeVersion(), true);
                cdpStackRequest = cdpStackRequests.get(cdpConfigKeyWithProfiler);
            }
        }
        return cdpStackRequest;
    }

    public StackV4Request getConfigForKey(CDPConfigKey cdpConfigKey) {
        try {
            String cdpStackRequest = cdpStackRequests.get(cdpConfigKey);
            if (cdpStackRequest == null) {
                return null;
            } else {
                String cdpStackRequestWithProfiler = getStackRequestForProfiler(cdpConfigKey);
                return JsonUtil.readValue(Objects.requireNonNullElse(cdpStackRequestWithProfiler, cdpStackRequest), StackV4Request.class);
            }
        } catch (IOException e) {
            LOGGER.error("Can't convert json to StackV4Request", e);
            throw new IllegalStateException("Can't convert json to StackV4Request", e);
        }
    }

    public String getDefaultRuntime() {
        return defaultRuntime;
    }

    public List<String> getDatalakeVersions(String cloudPlatform) {
        List<String> runtimeVersions = cdpStackRequests.keySet().stream()
                .filter(filterByCloudPlatformIfPresent(cloudPlatform))
                .map(CDPConfigKey::getRuntimeVersion)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        LOGGER.debug("Available runtime versions for datalake: {}", runtimeVersions);
        return runtimeVersions;
    }

    public Predicate<CDPConfigKey> filterByCloudPlatformIfPresent(String cloudPlatform) {
        return cdpStack -> {
            if (StringUtils.isEmpty(cloudPlatform) || cdpStack.getCloudPlatform().equalsIgnoreCase(cloudPlatform)) {
                return true;
            }
            return false;
        };
    }

    public List<AdvertisedRuntime> getAdvertisedRuntimes(String cloudPlatform) {
        List<String> runtimeVersions = getDatalakeVersions(cloudPlatform).stream()
                .filter(runtimeVersion -> advertisedRuntimes.isEmpty() || advertisedRuntimes.contains(runtimeVersion)).collect(Collectors.toList());

        List<AdvertisedRuntime> advertisedRuntimes = new ArrayList<>();
        for (String runtimeVersion : runtimeVersions) {
            AdvertisedRuntime advertisedRuntime = new AdvertisedRuntime();
            advertisedRuntime.setRuntimeVersion(runtimeVersion);
            if (runtimeVersion.equals(defaultRuntime)) {
                advertisedRuntime.setDefaultRuntimeVersion(true);
            }
            advertisedRuntimes.add(advertisedRuntime);
        }
        LOGGER.debug("Advertised runtime versions for datalake: {}", advertisedRuntimes);
        return advertisedRuntimes;
    }
}
