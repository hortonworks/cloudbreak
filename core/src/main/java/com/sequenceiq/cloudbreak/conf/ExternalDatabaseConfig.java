package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.platform.ExternalDatabasePlatformConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
public class ExternalDatabaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDatabaseConfig.class);

    @Inject
    private ExternalDatabasePlatformConfig externalDatabasePlatformConfig;

    @Inject
    private Set<DatabaseServerParameterDecorator> databaseServerParameterDecorators;

    @Bean
    public Map<CloudPlatform, DatabaseServerParameterDecorator> databaseParameterSetters() {
        ImmutableMap.Builder<CloudPlatform, DatabaseServerParameterDecorator> builder = new ImmutableMap.Builder<>();
        for (DatabaseServerParameterDecorator databaseServerParameterDecorator : databaseServerParameterDecorators) {
            builder.put(databaseServerParameterDecorator.getCloudPlatform(), databaseServerParameterDecorator);
        }
        return builder.build();
    }

    @Bean
    public Map<CloudPlatform, DatabaseStackConfig> databaseConfigs() throws IOException {
        ImmutableMap.Builder<CloudPlatform, DatabaseStackConfig> builder = new ImmutableMap.Builder<>();

        for (CloudPlatform cloudPlatform : externalDatabasePlatformConfig.getSupportedExternalDatabasePlatforms()) {
            Optional<DatabaseStackConfig> dbConfig = readDatabaseStackConfigResource(cloudPlatform);
            if (dbConfig.isPresent()) {
                builder.put(cloudPlatform, dbConfig.get());
            }
        }
        return builder.build();
    }

    private Optional<DatabaseStackConfig> readDatabaseStackConfigResource(CloudPlatform cloudPlatform)
            throws IOException {
        String resourcePath = String.format("externaldatabase/%s/database-template.json", cloudPlatform.toString().toLowerCase(Locale.US));
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspathQuietly(resourcePath);
        if (databaseTemplateJson == null) {
            LOGGER.debug("No readable external database template found for cloud platform {}, skipping", cloudPlatform);
            return Optional.empty();
        }
        return Optional.of(JsonUtil.readValue(databaseTemplateJson, DatabaseStackConfig.class));
    }
}
