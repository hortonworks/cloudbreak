package com.sequenceiq.cloudbreak.common.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class PreferencesService {

    @Value("${cb.enabledplatforms:AZURE,AWS,GCP,MOCK}")
    private String enabledPlatforms;

    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

    public Set<String> enabledPlatforms() {
        Set<String> platforms;
        platforms = enabledPlatforms.isEmpty()
                ? Collections.emptySet()
                : Sets.newHashSet(enabledPlatforms.split(","));
        return platforms;
    }

    public Map<String, Boolean> platformEnablement() {
        Map<String, Boolean> result = new HashMap<>();

        for (String platform : enabledPlatforms()) {
            result.put(platform, true);
        }
        return result;
    }

}
