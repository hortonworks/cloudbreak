package com.sequenceiq.cloudbreak.service.account;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_CLUSTER_DEFINITION;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_CLI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;

@Service
public class PreferencesService {

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    @Value("${cb.disable.show.cli:false}")
    private boolean disableShowCli;

    @Value("${cb.enabledplatforms:}")
    private String enabledPlatforms;

    @Inject
    private List<CloudConstant> cloudConstants;

    public Set<FeatureSwitchV4> getFeatureSwitches() {
        Set<FeatureSwitchV4> featureSwitchV4s = Sets.newHashSet();
        if (disableShowBlueprint) {
            featureSwitchV4s.add(DISABLE_SHOW_CLUSTER_DEFINITION);
        }
        if (disableShowCli) {
            featureSwitchV4s.add(DISABLE_SHOW_CLI);
        }
        return featureSwitchV4s;
    }

    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

    public Set<String> enabledPlatforms() {
        Set<String> platforms;
        if (enabledPlatforms.isEmpty()) {
            platforms = cloudConstants.stream()
                    .map(cloudConstant -> cloudConstant.platform().value())
                    .collect(Collectors.toSet());
        } else {
            platforms = Sets.newHashSet(enabledPlatforms.split(","));
        }
        return platforms;
    }

    public Map<String, Boolean> platformEnablement() {
        Map<String, Boolean> result = new HashMap<>();
        if (StringUtils.isEmpty(enabledPlatforms)) {
            for (CloudConstant cloudConstant : cloudConstants) {
                result.put(cloudConstant.platform().value(), true);
            }
        } else {
            for (String platform : enabledPlatforms()) {
                result.put(platform, true);
            }
            for (CloudConstant cloudConstant : cloudConstants) {
                if (!result.keySet().contains(cloudConstant.platform().value())) {
                    result.put(cloudConstant.platform().value(), false);
                }
            }
        }
        return result;
    }

}
