package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;


@Component
public class AccountPreferencesToJsonConverter extends AbstractConversionServiceAwareConverter<AccountPreferences, AccountPreferencesJson> {
    private static final long HOUR_IN_MS = 3600000L;

    private static final long ZERO = 0L;

    @Override
    public AccountPreferencesJson convert(AccountPreferences source) {
        AccountPreferencesJson json = new AccountPreferencesJson();
        json.setMaxNumberOfClusters(source.getMaxNumberOfClusters());
        json.setMaxNumberOfNodesPerCluster(source.getMaxNumberOfNodesPerCluster());
        json.setAllowedInstanceTypes(source.getAllowedInstanceTypes());
        long clusterTimeToLive = source.getClusterTimeToLive() == ZERO ? ZERO : source.getClusterTimeToLive() / HOUR_IN_MS;
        json.setClusterTimeToLive(clusterTimeToLive);
        long userTimeToLive = source.getUserTimeToLive() == ZERO ? ZERO : source.getUserTimeToLive() / HOUR_IN_MS;
        json.setUserTimeToLive(userTimeToLive);
        json.setMaxNumberOfClustersPerUser(source.getMaxNumberOfClustersPerUser());
        json.setPlatforms(source.getPlatforms());
        return json;
    }
}
