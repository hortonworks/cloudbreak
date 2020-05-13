package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.RazConfigurationResponse;

@Service
public class RazHelper {

    @Inject
    private EntitlementService entilementService;

    public boolean shouldRazBeConfigured(boolean razEnabled, String actorCrn, Crn accountId, CloudPlatform cloudPlatform) {
        return razEnabled
                && entilementService.razEnabled(actorCrn, accountId.toString())
                && CloudPlatform.AZURE == cloudPlatform;
    }

    public Map<String, String> buildUserMapping(RazConfigurationResponse razConfiguration) {
        Map<String, String> razMap = Map.of();
        if (razConfiguration != null && razConfiguration.isRazEnabled()) {
            razMap =  new HashMap();
            razMap.put("rangerraz", razConfiguration.getSecurityGroupIdForRaz());
        }
        return razMap;
    }
}
