package com.sequenceiq.cloudbreak.converter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson;

@Component
public class JsonToAccountPreferencesConverter extends AbstractConversionServiceAwareConverter<AccountPreferencesJson, AccountPreferences> {
    private static final long HOUR_IN_MS = 3600000L;

    @Override
    public AccountPreferences convert(AccountPreferencesJson source) {
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
        return target;
    }

}
