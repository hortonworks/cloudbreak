package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import org.springframework.stereotype.Component;

@Component
public class AccountPreferencesToJsonConverter extends AbstractConversionServiceAwareConverter<AccountPreferences, AccountPreferencesJson> {

    @Override
    public AccountPreferencesJson convert(AccountPreferences source) {
        AccountPreferencesJson json = new AccountPreferencesJson();
        json.setMaxNumberOfClusters(source.getMaxNumberOfClusters());
        json.setMaxNumberOfNodesPerCluster(source.getMaxNumberOfNodesPerCluster());
        json.setAllowedInstanceTypes(source.getAllowedInstanceTypes());
        json.setClusterTimeToLive(source.getClusterTimeToLive());
        json.setUserTimeToLive(source.getUserTimeToLive());
        json.setMaxNumberOfClustersPerUser(source.getMaxNumberOfClustersPerUser());
        return json;
    }
}
