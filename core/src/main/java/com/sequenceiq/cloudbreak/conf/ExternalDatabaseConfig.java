package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfigKey;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseType;

@Configuration
public class ExternalDatabaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDatabaseConfig.class);

    @Value("${cb.externaldatabase.supported.platform:AWS,GCP}")
    private Set<CloudPlatform> dbServiceSupportedPlatforms;

    @Value("${cb.externaldatabase.experimental.platform:AZURE,MOCK}")
    private Set<CloudPlatform> dbServiceExperimentalPlatforms;

    @Value("${cb.externaldatabase.pause.supported.platform:AWS,AZURE,GCP}")
    private Set<CloudPlatform> dbServicePauseSupportedPlatforms;

    @Value("${cb.externaldatabase.sslenforcement.supported.platform:GCP,AWS,AZURE}")
    private Set<CloudPlatform> dbServiceSslEnforcementSupportedPlatforms;

    private final Map<CloudPlatform, Set<? extends DatabaseType>> databaseTypeMap = Map.of(CloudPlatform.AZURE, EnumSet.allOf(AzureDatabaseType.class));

    @Inject
    private Set<DatabaseServerParameterDecorator> databaseServerParameterDecorators;

    private Set<CloudPlatform> allPossibleExternalDbPlatforms;

    public boolean isExternalDatabaseSupportedFor(CloudPlatform cloudPlatform) {
        return dbServiceSupportedPlatforms.contains(cloudPlatform);
    }

    public boolean isExternalDatabasePauseSupportedFor(CloudPlatform cloudPlatform, DatabaseType databaseType) {
        return dbServicePauseSupportedPlatforms.contains(cloudPlatform) &&
                Optional.ofNullable(databaseType).map(DatabaseType::isDatabasePauseSupported).orElse(true);
    }

    public boolean isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform cloudPlatform) {
        return dbServiceSslEnforcementSupportedPlatforms.contains(cloudPlatform);
    }

    public Set<CloudPlatform> getSupportedExternalDatabasePlatforms() {
        return dbServiceSupportedPlatforms;
    }

    public Set<CloudPlatform> getPauseSupportedExternalDatabasePlatforms() {
        return dbServicePauseSupportedPlatforms;
    }

    public Set<CloudPlatform> getAllPossibleExternalDbPlatforms() {
        return allPossibleExternalDbPlatforms;
    }

    public boolean isExternalDatabaseSupportedOrExperimental(CloudPlatform cloudPlatform) {
        return allPossibleExternalDbPlatforms.contains(cloudPlatform);
    }

    @PostConstruct
    public void createAllPossibleDatabasePlatform() {
        allPossibleExternalDbPlatforms = new HashSet<>();
        allPossibleExternalDbPlatforms.addAll(dbServiceSupportedPlatforms);
        allPossibleExternalDbPlatforms.addAll(dbServiceExperimentalPlatforms);
    }

    @Bean
    public Map<CloudPlatform, DatabaseServerParameterDecorator> databaseParameterSetters() {
        ImmutableMap.Builder<CloudPlatform, DatabaseServerParameterDecorator> builder = new ImmutableMap.Builder<>();
        for (DatabaseServerParameterDecorator databaseServerParameterDecorator : databaseServerParameterDecorators) {
            builder.put(databaseServerParameterDecorator.getCloudPlatform(), databaseServerParameterDecorator);
        }
        return builder.build();
    }

    @Bean
    public Map<DatabaseStackConfigKey, DatabaseStackConfig> databaseConfigs() throws IOException {
        ImmutableMap.Builder<DatabaseStackConfigKey, DatabaseStackConfig> builder = new ImmutableMap.Builder<>();

        for (CloudPlatform cloudPlatform : allPossibleExternalDbPlatforms) {
            Set<? extends DatabaseType> databaseTypes = databaseTypeMap.getOrDefault(cloudPlatform, Set.of());
            if (databaseTypes.isEmpty()) {
                readDatabaseStackConfigResource(cloudPlatform, null)
                        .ifPresent(dbc -> builder.put(new DatabaseStackConfigKey(cloudPlatform, null), dbc));
            } else {
                for (DatabaseType databaseType : databaseTypes) {
                    readDatabaseStackConfigResource(cloudPlatform, databaseType)
                            .ifPresent(dbc -> builder.put(new DatabaseStackConfigKey(cloudPlatform, databaseType), dbc));
                }
            }
        }
        return builder.build();
    }

    private String getResourcePath(CloudPlatform cloudPlatform, DatabaseType databaseType) {
        String cloudPlatformString = cloudPlatform.toString().toLowerCase(Locale.ROOT);
        String resourcePath;
        if (databaseType == null) {
            resourcePath = String.format("externaldatabase/%s/database-template.json", cloudPlatformString);
        } else {
            resourcePath = String.format("externaldatabase/%s/database-%s-template.json", cloudPlatformString, databaseType.shortName());
        }
        return resourcePath;
    }

    private Optional<DatabaseStackConfig> readDatabaseStackConfigResource(CloudPlatform cloudPlatform, DatabaseType databaseType)
            throws IOException {
        String resourcePath = getResourcePath(cloudPlatform, databaseType);
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspathQuietly(resourcePath);
        if (databaseTemplateJson == null) {
            LOGGER.debug("No readable external database template found for cloud platform {} and database type {}, skipping", cloudPlatform, databaseType);
            return Optional.empty();
        }
        return Optional.of(JsonUtil.readValue(databaseTemplateJson, DatabaseStackConfig.class));
    }
}
