package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
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
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfig;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfigKey;
import com.sequenceiq.datalake.service.sdx.database.DatabaseServerParameterSetter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
public class PlatformConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformConfig.class);

    @Value("${datalake.supported.externaldb.platform:AWS,AZURE,GCP}")
    private Set<CloudPlatform> dbServiceSupportedPlatforms;

    @Value("${datalake.experimental.externaldb.platform:MOCK}")
    private Set<CloudPlatform> dbServiceExperimentalPlatforms;

    @Value("${datalake.supported.externaldb.pause.platform:AWS,GCP,MOCK}")
    private Set<CloudPlatform> dbServicePauseSupportedPlatforms;

    @Value("${datalake.supported.externaldb.sslenforcement.platform:AWS,AZURE,MOCK}")
    private Set<CloudPlatform> dbServiceSslEnforcementSupportedPlatforms;

    @Value("${datalake.supported.raz.platform:AWS,AZURE,GCP}")
    private List<CloudPlatform> razSupportedPlatforms;

    @Value("${datalake.supported.multiaz.platform:AWS,AZURE}")
    private Set<CloudPlatform> multiAzSupportedPlatforms;

    private final Map<CloudPlatform, Set<? extends DatabaseType>> databaseTypeMap = Map.of(CloudPlatform.AZURE, EnumSet.allOf(AzureDatabaseType.class));

    @Inject
    private Set<DatabaseServerParameterSetter> databaseServerParameterSetters;

    private Set<CloudPlatform> allPossibleExternalDbPlatforms;

    public boolean isExternalDatabaseSupportedFor(CloudPlatform cloudPlatform) {
        return dbServiceSupportedPlatforms.contains(cloudPlatform);
    }

    public boolean isExternalDatabasePauseSupportedFor(CloudPlatform cloudPlatform) {
        return dbServicePauseSupportedPlatforms.contains(cloudPlatform);
    }

    public boolean isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform cloudPlatform) {
        return dbServiceSslEnforcementSupportedPlatforms.contains(cloudPlatform);
    }

    public List<CloudPlatform> getRazSupportedPlatforms() {
        return razSupportedPlatforms;
    }

    public Set<CloudPlatform> getMultiAzSupportedPlatforms() {
        return multiAzSupportedPlatforms;
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
    public void initPlatforms() {
        allPossibleExternalDbPlatforms = EnumSet.copyOf(dbServiceSupportedPlatforms);
        allPossibleExternalDbPlatforms.addAll(dbServiceExperimentalPlatforms);

        if (CollectionUtils.isEmpty(multiAzSupportedPlatforms)) {
            multiAzSupportedPlatforms = EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE);
        }
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
                Set<? extends DatabaseType> databaseTypes = databaseTypeMap.getOrDefault(cloudPlatform, Set.of());
                if (databaseTypes.isEmpty()) {
                    readDbConfig(cloudPlatform, sdxClusterShape, null)
                            .ifPresent(dbc -> builder.put(new DatabaseConfigKey(cloudPlatform, sdxClusterShape, null), dbc));
                } else {
                    for (DatabaseType databaseType : databaseTypes) {
                        readDbConfig(cloudPlatform, sdxClusterShape, databaseType)
                                .ifPresent(dbc -> builder.put(new DatabaseConfigKey(cloudPlatform, sdxClusterShape, databaseType), dbc));
                    }
                }
            }
        }
        return builder.build();
    }

    private String getResourcePath(CloudPlatform cloudPlatform, SdxClusterShape sdxClusterShape, DatabaseType databaseType) {
        String clusterShapeString = sdxClusterShape.toString().toLowerCase(Locale.US).replaceAll("_", "-");
        String cloudPlatformString = cloudPlatform.toString().toLowerCase(Locale.US);
        String resourcePath;
        if (databaseType == null) {
            resourcePath = String.format("rds/%s/database-%s-template.json", cloudPlatformString, clusterShapeString);
        } else {
            resourcePath = String.format("rds/%s/database-%s-%s-template.json", cloudPlatformString, clusterShapeString, databaseType.shortName());
        }
        return resourcePath;
    }

    private Optional<DatabaseConfig> readDbConfig(CloudPlatform cloudPlatform, SdxClusterShape sdxClusterShape, DatabaseType databaseType)
            throws IOException {
        String resourcePath = getResourcePath(cloudPlatform, sdxClusterShape, databaseType);
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspathQuietly(resourcePath);
        if (databaseTemplateJson == null) {
            LOGGER.debug("No readable database config found for cloud platform {}, cluster shape {}, databasetype {}: skipping",
                    cloudPlatform, sdxClusterShape, databaseType);
            return Optional.empty();
        }
        return Optional.of(JsonUtil.readValue(databaseTemplateJson, DatabaseConfig.class));
    }
}
