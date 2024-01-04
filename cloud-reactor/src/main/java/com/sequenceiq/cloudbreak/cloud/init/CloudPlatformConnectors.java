package com.sequenceiq.cloudbreak.cloud.init;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class CloudPlatformConnectors {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPlatformConnectors.class);

    @Value("${cb.platform.default.variants:}")
    private String platformDefaultVariants;

    @Value("${cb.platform.default.govVariants:}")
    private String platformDefaultGovVariants;

    private final Map<Platform, Variant> defaultVariants = new HashMap<>();

    private final Map<Platform, Variant> defaultGovVariants = new HashMap<>();

    @Inject
    private List<CloudConnector> cloudConnectors;

    private final Map<CloudPlatformVariant, CloudConnector> map = new HashMap<>();

    private Multimap<Platform, Variant> platformToVariants;

    @PostConstruct
    public void cloudPlatformConnectors() {
        platformToVariants = HashMultimap.create();

        cloudConnectors.forEach(connector -> {
            map.put(new CloudPlatformVariant(connector.platform(), connector.variant()), connector);
            platformToVariants.put(connector.platform(), connector.variant());
        });
        setupDefaultVariants(platformToVariants, extractEnvironmentDefaultVariants());
        setupDefaultGovVariants(platformToVariants, extractEnvironmentDefaultGovVariants());
        LOGGER.debug(map.toString());
        LOGGER.debug(defaultVariants.toString());
        LOGGER.debug(defaultGovVariants.toString());
    }

    private Map<Platform, Variant> extractEnvironmentDefaultVariants() {
        return toMap(platformDefaultVariants);
    }

    private Map<Platform, Variant> extractEnvironmentDefaultGovVariants() {
        return toMap(platformDefaultGovVariants);
    }

    private Map<Platform, Variant> toMap(String s) {
        Map<Platform, Variant> result = Maps.newHashMap();
        if (!Strings.isNullOrEmpty(s)) {
            for (String entry : s.split(",")) {
                String[] keyValue = entry.split(":");
                result.put(platform(keyValue[0]), Variant.variant(keyValue[1]));
            }
        }
        return result;
    }

    private void setupDefaultVariants(Multimap<Platform, Variant> platformToVariants, Map<Platform, Variant> environmentDefaults) {
        for (Entry<Platform, Collection<Variant>> platformVariants : platformToVariants.asMap().entrySet()) {
            if (platformVariants.getValue().size() == 1) {
                Collection<Variant> value = platformVariants.getValue();
                defaultVariants.put(platformVariants.getKey(), value.toArray(new Variant[value.size()])[0]);
            } else {
                if (platformVariants.getValue().contains(environmentDefaults.get(platformVariants.getKey()))) {
                    defaultVariants.put(platformVariants.getKey(), environmentDefaults.get(platformVariants.getKey()));
                } else {
                    throw new IllegalStateException(String.format("No default variant is specified for platform: '%s'", platformVariants.getKey()));
                }
            }
        }
    }

    private void setupDefaultGovVariants(Multimap<Platform, Variant> platformToVariants, Map<Platform, Variant> environmentDefaults) {
        for (Entry<Platform, Collection<Variant>> platformVariants : platformToVariants.asMap().entrySet()) {
            if (platformVariants.getValue().size() == 1) {
                Collection<Variant> value = platformVariants.getValue();
                defaultGovVariants.put(platformVariants.getKey(), value.toArray(new Variant[value.size()])[0]);
            } else {
                if (platformVariants.getValue().contains(environmentDefaults.get(platformVariants.getKey()))) {
                    defaultGovVariants.put(platformVariants.getKey(), environmentDefaults.get(platformVariants.getKey()));
                }
            }
        }
    }

    public Variant getDefaultVariant(Platform platform) {
        return defaultVariants.get(platform);
    }

    public Variant getDefaultGovVariant(Platform platform) {
        return defaultGovVariants.get(platform);
    }

    public CloudConnector getDefault(Platform platform) {
        Variant variant = getDefaultVariant(platform);
        return map.get(new CloudPlatformVariant(platform, variant));
    }

    public CloudConnector getGovDefault(Platform platform) {
        Variant variant = getDefaultGovVariant(platform);
        return map.get(new CloudPlatformVariant(platform, variant));
    }

    public CloudConnector get(Platform platform, Variant variant) {
        return get(new CloudPlatformVariant(platform, variant));
    }

    public CloudConnector getGov(Platform platform, Variant variant) {
        return getGov(new CloudPlatformVariant(platform, variant));
    }

    public CloudConnector get(CloudPlatformVariant variant) {
        CloudConnector cloudConnector = map.get(variant);
        return cloudConnector == null ? getDefault(variant.getPlatform()) : cloudConnector;
    }

    public CloudConnector getGov(CloudPlatformVariant variant) {
        CloudConnector cloudConnector = map.get(variant);
        return cloudConnector == null ? getGovDefault(variant.getPlatform()) : cloudConnector;
    }

    public PlatformVariants getPlatformVariants() {
        return new PlatformVariants(platformToVariants.asMap(), defaultVariants);
    }

    public PlatformVariants getPlatformGovVariants() {
        return new PlatformVariants(platformToVariants.asMap(), defaultGovVariants);
    }

}
