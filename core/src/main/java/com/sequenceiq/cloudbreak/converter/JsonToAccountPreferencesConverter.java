package com.sequenceiq.cloudbreak.converter;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import org.springframework.stereotype.Component;

@Component
public class JsonToAccountPreferencesConverter extends AbstractConversionServiceAwareConverter<AccountPreferencesJson, AccountPreferences> {

    @Override
    public AccountPreferences convert(AccountPreferencesJson source) {
        AccountPreferences target = new AccountPreferences();
        target.setMaxNumberOfClusters(source.getMaxNumberOfClusters());
        target.setMaxNumberOfNodesPerCluster(source.getMaxNumberOfNodesPerCluster());
        List<String> allowedInstanceTypes = source.getAllowedInstanceTypes();
        if (allowedInstanceTypes != null && !allowedInstanceTypes.isEmpty()) {
            target.setAllowedInstanceTypes(allowedInstanceTypes);
        }
        target.setClusterTimeToLive(source.getClusterTimeToLive());
        target.setUserTimeToLive(source.getUserTimeToLive());
        target.setMaxNumberOfClustersPerUser(source.getMaxNumberOfClustersPerUser());
        return target;
    }

}
