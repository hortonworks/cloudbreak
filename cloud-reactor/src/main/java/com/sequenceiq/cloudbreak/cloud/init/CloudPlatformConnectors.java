package com.sequenceiq.cloudbreak.cloud.init;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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

    private final Map<Platform, Variant> defaultVariants = new HashMap<>();

    @Inject
    private List<CloudConnector> cloudConnectors;

    private final Map<CloudPlatformVariant, CloudConnector<Object>> map = new HashMap<>();

    private Multimap<Platform, Variant> platformToVariants;

    @PostConstruct
    public void cloudPlatformConnectors() {
        platformToVariants = HashMultimap.create();
        cloudConnectors.forEach(connector -> {
            map.put(new CloudPlatformVariant(connector.platform(), connector.variant()), connector);
            platformToVariants.put(connector.platform(), connector.variant());
        });
        Map<Platform, Variant> environmentDefaults = extractEnvironmentDefaultVariants();
        setupDefaultVariants(platformToVariants, environmentDefaults);
        LOGGER.debug(map.toString());
        LOGGER.debug(defaultVariants.toString());
    }

    private Map<Platform, Variant> extractEnvironmentDefaultVariants() {
        return toMap(platformDefaultVariants);
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

    public Variant getDefaultVariant(Platform platform) {
        return defaultVariants.get(platform);
    }

    public CloudConnector<Object> getDefault(Platform platform) {
        Variant variant = getDefaultVariant(platform);
        return map.get(new CloudPlatformVariant(platform, variant));
    }

    public CloudConnector<Object> get(Platform platform, Variant variant) {
        return get(new CloudPlatformVariant(platform, variant));
    }

    public CloudConnector<Object> get(CloudPlatformVariant variant) {
        CloudConnector<Object> cloudConnector = map.get(variant);
        return cloudConnector == null ? getDefault(variant.getPlatform()) : cloudConnector;
    }

    public PlatformVariants getPlatformVariants() {
        return new PlatformVariants(platformToVariants.asMap(), defaultVariants);
    }

}
