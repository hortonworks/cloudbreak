package com.sequenceiq.cloudbreak.service.account;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_BLUEPRINT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_CLI;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.platform.PlatformConfig;

@Service
public class PreferencesService {

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    @Value("${cb.disable.show.cli:false}")
    private boolean disableShowCli;

    @Inject
    private PlatformConfig platformConfig;

    public Set<FeatureSwitchV4> getFeatureSwitches() {
        Set<FeatureSwitchV4> featureSwitchV4s = Sets.newHashSet();
        if (disableShowBlueprint) {
            featureSwitchV4s.add(DISABLE_SHOW_BLUEPRINT);
        }
        if (disableShowCli) {
            featureSwitchV4s.add(DISABLE_SHOW_CLI);
        }
        return featureSwitchV4s;
    }

    public Set<CloudPlatform> getAllPossiblePlatforms() {
        return platformConfig.getAllPossiblePlatforms();
    }

    public Set<String> getAllPossiblePlatformsAsString() {
        return platformConfig.getAllPossiblePlatforms().stream().map(CloudPlatform::toString).collect(Collectors.toSet());
    }

    public Map<String, Boolean> platformEnablement() {
        Map<String, Boolean> result = new HashMap<>();
        for (CloudPlatform platform : getAllPossiblePlatforms()) {
            result.put(platform.toString(), true);
        }
        return result;
    }

}
