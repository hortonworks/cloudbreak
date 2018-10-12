package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.FeatureSwitch.DISABLE_SHOW_BLUEPRINT;
import static com.sequenceiq.cloudbreak.api.model.FeatureSwitch.DISABLE_SHOW_CLI;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AccountPreferencesBase;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.api.model.FeatureSwitch;
import com.sequenceiq.cloudbreak.api.model.SupportedExternalDatabaseServiceEntryResponse;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseProvider;

@Component
public class AccountPreferencesToAccountPreferencesJsonConverter
        extends AbstractConversionServiceAwareConverter<AccountPreferences, AccountPreferencesResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountPreferencesToAccountPreferencesJsonConverter.class);

    private static final long HOUR_IN_MS = 3600000L;

    private static final long ZERO = 0L;

    @Value("${cb.smartsense.enabled:true}")
    private boolean smartsenseEnabled;

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    @Value("${cb.disable.show.cli:false}")
    private boolean disableShowCli;

    @Override
    public AccountPreferencesResponse convert(AccountPreferences source) {
        AccountPreferencesResponse json = new AccountPreferencesResponse();
        json.setPlatforms(source.getPlatforms());
        json.setSmartsenseEnabled(smartsenseEnabled);
        SupportedDatabaseProvider.supportedExternalDatabases().forEach(item ->
                json.getSupportedExternalDatabases().add(getConversionService().convert(item, SupportedExternalDatabaseServiceEntryResponse.class)));
        addSwitches(json.getFeatureSwitches());
        convertTags(json, source.getDefaultTags());
        return json;
    }

    private void addSwitches(Set<FeatureSwitch> featureSwitches) {
        if (disableShowBlueprint) {
            featureSwitches.add(DISABLE_SHOW_BLUEPRINT);
        }
        if (disableShowCli) {
            featureSwitches.add(DISABLE_SHOW_CLI);
        }
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
