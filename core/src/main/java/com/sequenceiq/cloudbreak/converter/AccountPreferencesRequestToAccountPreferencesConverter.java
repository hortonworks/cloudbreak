package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AccountPreferencesRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class AccountPreferencesRequestToAccountPreferencesConverter
        extends AbstractConversionServiceAwareConverter<AccountPreferencesRequest, AccountPreferences> {
    private static final long HOUR_IN_MS = 3600000L;

    @Override
    public AccountPreferences convert(AccountPreferencesRequest source) {
        AccountPreferences target = new AccountPreferences();
        target.setMaxNumberOfClusters(source.getMaxNumberOfClusters());
        target.setMaxNumberOfNodesPerCluster(source.getMaxNumberOfNodesPerCluster());
        List<String> allowedInstanceTypes = source.getAllowedInstanceTypes();
        if (allowedInstanceTypes != null && !allowedInstanceTypes.isEmpty()) {
            target.setAllowedInstanceTypes(allowedInstanceTypes);
        }
        target.setClusterTimeToLive(source.getClusterTimeToLive() * HOUR_IN_MS);
        target.setUserTimeToLive(source.getUserTimeToLive() * HOUR_IN_MS);
        target.setMaxNumberOfClustersPerUser(source.getMaxNumberOfClustersPerUser());
        target.setPlatforms(source.getPlatforms());
        target.setDefaultTags(getTags(source.getDefaultTags()));
        return target;
    }

    private Json getTags(Map<String, String> tags) {
        try {
            if (tags == null || tags.isEmpty()) {
                return new Json(new HashMap<>());
            }
            return new Json(tags);
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

}
