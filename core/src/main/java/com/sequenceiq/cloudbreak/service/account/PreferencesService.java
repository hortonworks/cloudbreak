package com.sequenceiq.cloudbreak.service.account;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_CLI;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_BLUEPRINT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
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

    @Value("${cb.enabledgovplatforms:}")
    private String enabledGovPlatforms;

    @Inject
    private List<CloudConstant> cloudConstants;

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

    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

    public Set<String> enabledPlatforms() {
        Set<String> platforms;
        platforms = enabledPlatforms.isEmpty()
                ? cloudConstants.stream().map(cloudConstant -> cloudConstant.platform().value()).collect(Collectors.toSet())
                : Sets.newHashSet(enabledPlatforms.split(","));
        return platforms;
    }

    public Set<String> enabledGovPlatforms() {
        return Strings.isNullOrEmpty(enabledGovPlatforms)
                ? Sets.newHashSet()
                : Sets.newHashSet(enabledGovPlatforms.split(","));
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

    public Map<String, Boolean> govPlatformEnablement() {
        Map<String, Boolean> result = new HashMap<>();
        if (StringUtils.isEmpty(enabledGovPlatforms)) {
            for (CloudConstant cloudConstant : cloudConstants) {
                result.put(cloudConstant.platform().value(), true);
            }
        } else {
            for (String platform : enabledGovPlatforms()) {
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
