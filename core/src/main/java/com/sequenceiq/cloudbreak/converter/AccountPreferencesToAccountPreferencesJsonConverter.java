package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AccountPreferencesBase;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.api.model.SupportedExternalDatabaseServiceEntryResponse;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.cluster.SupportedDatabaseProvider;


@Component
public class AccountPreferencesToAccountPreferencesJsonConverter
        extends AbstractConversionServiceAwareConverter<AccountPreferences, AccountPreferencesResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountPreferencesToAccountPreferencesJsonConverter.class);

    private static final long HOUR_IN_MS = 3600000L;

    private static final long ZERO = 0L;

    @Value("${cb.smartsense.enabled:true}")
    private boolean smartsenseEnabled;

    @Inject
    private SupportedDatabaseProvider supportedDatabaseProvider;

    @Override
    public AccountPreferencesResponse convert(AccountPreferences source) {
        AccountPreferencesResponse json = new AccountPreferencesResponse();
        json.setMaxNumberOfClusters(source.getMaxNumberOfClusters());
        json.setMaxNumberOfNodesPerCluster(source.getMaxNumberOfNodesPerCluster());
        json.setAllowedInstanceTypes(source.getAllowedInstanceTypes());
        long clusterTimeToLive = source.getClusterTimeToLive() == ZERO ? ZERO : source.getClusterTimeToLive() / HOUR_IN_MS;
        json.setClusterTimeToLive(clusterTimeToLive);
        long userTimeToLive = source.getUserTimeToLive() == ZERO ? ZERO : source.getUserTimeToLive() / HOUR_IN_MS;
        json.setUserTimeToLive(userTimeToLive);
        json.setMaxNumberOfClustersPerUser(source.getMaxNumberOfClustersPerUser());
        json.setPlatforms(source.getPlatforms());
        json.setSmartsenseEnabled(smartsenseEnabled);
        supportedDatabaseProvider.get().forEach(item ->
                json.getSupportedExternalDatabases().add(getConversionService().convert(item, SupportedExternalDatabaseServiceEntryResponse.class)));
        convertTags(json, source.getDefaultTags());
        return json;
    }

    private void convertTags(AccountPreferencesBase apJson, Json tag) {
        Map<String, String> tags = new HashMap<>();
        try {
            if (tag != null && tag.getValue() != null) {
                apJson.setDefaultTags(tag.get(Map.class));
            } else {
                apJson.setDefaultTags(tags);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert default tags.", e);
            apJson.setDefaultTags(tags);
        }
    }
}
