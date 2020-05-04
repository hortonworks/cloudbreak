package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfig;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfigKey;
import com.sequenceiq.datalake.service.sdx.database.DatabaseServerParameterSetter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
public class PlatformConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformConfig.class);

    @Value("${datalake.supported.externaldb.platform:AWS,AZURE}")
    private Set<CloudPlatform> dbServiceSupportedPlatforms;

    @Value("${datalake.experimental.externaldb.platform:MOCK}")
    private Set<CloudPlatform> dbServiceExperimentalPlatforms;

    @Value("${datalake.supported.externaldb.pause.platform:AWS}")
    private Set<CloudPlatform> dbServicePauseSupportedPlatforms;

    @Inject
    private Set<DatabaseServerParameterSetter> databaseServerParameterSetters;

    private Set<CloudPlatform> allPossibleExternalDbPlatforms;

    public boolean isExternalDatabaseSupportedFor(CloudPlatform cloudPlatform) {
        return dbServiceSupportedPlatforms.contains(cloudPlatform);
    }

    public boolean isExternalDatabasePauseSupportedFor(CloudPlatform cloudPlatform) {
        return dbServicePauseSupportedPlatforms.contains(cloudPlatform);
    }

    public Set<CloudPlatform> getSupportedExternalDatabasePlatforms() {
        return dbServiceSupportedPlatforms;
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
    public Map<CloudPlatform, DatabaseServerParameterSetter> databaseParameterSetters() throws IOException {
        ImmutableMap.Builder<CloudPlatform, DatabaseServerParameterSetter> builder = new ImmutableMap.Builder<>();

        for (DatabaseServerParameterSetter databaseServerParameterSetter : databaseServerParameterSetters) {
                    builder.put(databaseServerParameterSetter.getCloudPlatform(), databaseServerParameterSetter);
        }
        return builder.build();
    }

    @Bean
    public Map<DatabaseConfigKey, DatabaseConfig> databaseConfigs() throws IOException {
        ImmutableMap.Builder<DatabaseConfigKey, DatabaseConfig> builder = new ImmutableMap.Builder<>();

        for (CloudPlatform cloudPlatform : allPossibleExternalDbPlatforms) {
            for (SdxClusterShape sdxClusterShape : SdxClusterShape.values()) {
                Optional<DatabaseConfig> dbConfig = readDbConfig(cloudPlatform, sdxClusterShape);
                if (dbConfig.isPresent()) {
                    builder.put(new DatabaseConfigKey(cloudPlatform, sdxClusterShape), dbConfig.get());
                }
            }
        }
        return builder.build();
    }

    private Optional<DatabaseConfig> readDbConfig(CloudPlatform cloudPlatform, SdxClusterShape sdxClusterShape)
            throws IOException {
        String resourcePath = String.format("rds/%s/database-%s-template.json",
                cloudPlatform.toString().toLowerCase(Locale.US),
                sdxClusterShape.toString().toLowerCase(Locale.US).replaceAll("_", "-"));
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspathQuietly(resourcePath);
        if (databaseTemplateJson == null) {
            LOGGER.debug("No readable database config found for cloud platform {}, cluster shape {}: skipping",
                    cloudPlatform, sdxClusterShape);
            return Optional.empty();
        }
        return Optional.of(JsonUtil.readValue(databaseTemplateJson, DatabaseConfig.class));
    }
}
