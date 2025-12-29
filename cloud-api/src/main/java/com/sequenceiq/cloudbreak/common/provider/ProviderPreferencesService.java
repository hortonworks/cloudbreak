package com.sequenceiq.cloudbreak.common.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;

@Service
public class ProviderPreferencesService {

    @Value("${cb.enabledplatforms:}")
    private String enabledPlatforms;

    @Value("${cb.enabledgovplatforms:}")
    private String enabledGovPlatforms;

    @Autowired(required = false)
    private List<CloudConstant> cloudConstants = new ArrayList<>();

    @Inject
    private CommonGovService commonGovService;

    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

    public Set<String> enabledPlatforms() {
        Set<String> platforms = Collections.emptySet();
        if (enabledPlatformConfigurationsAreEmpty()) {
            platforms = cloudConstants.stream().map(cloudConstant -> cloudConstant.platform().value()).collect(Collectors.toSet());
        } else if (!Strings.isNullOrEmpty(enabledPlatforms)) {
            platforms = Sets.newHashSet(enabledPlatforms.split(","));
        }
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
                result.put(cloudConstant.platform().value(), false);
            }
        } else {
            for (String platform : enabledPlatforms()) {
                result.put(platform, true);
            }
            for (CloudConstant cloudConstant : cloudConstants) {
                if (!result.containsKey(cloudConstant.platform().value())) {
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
                result.put(cloudConstant.platform().value(), false);
            }
        } else {
            for (String platform : enabledGovPlatforms()) {
                result.put(platform, true);
            }
            for (CloudConstant cloudConstant : cloudConstants) {
                if (!result.containsKey(cloudConstant.platform().value())) {
                    result.put(cloudConstant.platform().value(), false);
                }
            }
        }
        return result;
    }

    public Optional<CloudConstant> cloudConstantByName(String name) {
        return cloudConstants.stream()
                .filter(cloudConstant -> cloudConstant.platform().value().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean isGovCloudDeployment() {
        return commonGovService.govCloudDeployment(enabledGovPlatforms(), enabledPlatforms());
    }

    private boolean enabledPlatformConfigurationsAreEmpty() {
        return Strings.isNullOrEmpty(enabledPlatforms) && Strings.isNullOrEmpty(enabledGovPlatforms);
    }
}
