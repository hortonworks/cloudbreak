package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.service.sdx.DatabaseConfig;
import com.sequenceiq.datalake.service.sdx.DatabaseConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
public class PlatformConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformConfig.class);

    @Value("${datalake.supported.externaldb.platform:AWS}")
    private Set<CloudPlatform> dbServiceSupportedPlatforms;

    @Value("${datalake.experimental.externaldb.platform:AZURE,MOCK}")
    private Set<CloudPlatform> dbServiceExperimentalPlatforms;

    private Set<CloudPlatform> allPossibleExternalDbPlatforms;

    public boolean isExternalDatabaseSupportedFor(String cloudPlatform) {
        return dbServiceSupportedPlatforms.contains(CloudPlatform.valueOf(cloudPlatform));
    }

    public boolean isExternalDatabaseSupportedFor(CloudPlatform cloudPlatform) {
        return dbServiceSupportedPlatforms.contains(cloudPlatform);
    }

    public Set<CloudPlatform> getSupportedExternalDatabasePlatforms() {
        return dbServiceSupportedPlatforms;
    }

    public Set<CloudPlatform> getAllPossibleExternalDbPlatforms() {
        return allPossibleExternalDbPlatforms;
    }

    public boolean isExternalDatabaseSupportedOrExperimental(String cloudPlatform) {
        return allPossibleExternalDbPlatforms.contains(CloudPlatform.valueOf(cloudPlatform));
    }

    @PostConstruct
    public void createAllPossibleDatabasePlatform() {
        allPossibleExternalDbPlatforms = new HashSet<>();
        allPossibleExternalDbPlatforms.addAll(dbServiceSupportedPlatforms);
        allPossibleExternalDbPlatforms.addAll(dbServiceExperimentalPlatforms);
    }

    @Bean
    public Map<DatabaseConfigKey, com.sequenceiq.datalake.service.sdx.DatabaseConfig> databaseConfigs() throws IOException {
        ImmutableMap.Builder<DatabaseConfigKey, com.sequenceiq.datalake.service.sdx.DatabaseConfig> builder = new ImmutableMap.Builder<>();

        for (CloudPlatform cloudPlatform : allPossibleExternalDbPlatforms) {
            for (SdxClusterShape sdxClusterShape : SdxClusterShape.values()) {
                Optional<com.sequenceiq.datalake.service.sdx.DatabaseConfig> dbConfig = readDbConfig(cloudPlatform, sdxClusterShape);
                if (dbConfig.isPresent()) {
                    builder.put(new DatabaseConfigKey(cloudPlatform, sdxClusterShape), dbConfig.get());
                }
            }
        }
        return builder.build();
    }

    private Optional<com.sequenceiq.datalake.service.sdx.DatabaseConfig> readDbConfig(CloudPlatform cloudPlatform, SdxClusterShape sdxClusterShape)
            throws IOException {
        String resourcePath = String.format("sdx/%s/database-%s-template.json",
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
