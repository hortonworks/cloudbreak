package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.internal.CloudbreakApiClientParams;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.logger.ThreadBasedRequestIdProvider;
import com.sequenceiq.datalake.service.sdx.DatabaseConfig;
import com.sequenceiq.datalake.service.sdx.DatabaseConfigKey;
import com.sequenceiq.environment.client.internal.EnvironmentApiClientParams;
import com.sequenceiq.redbeams.client.internal.RedbeamsApiClientParams;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Inject
    @Named("cloudbreakUrl")
    private String cloudbreakUrl;

    @Inject
    @Named("environmentServerUrl")
    private String environmentServerUrl;

    @Inject
    @Named("redbeamsServerUrl")
    private String redbeamsServerUrl;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Bean
    public CloudbreakApiClientParams cloudbreakApiClientParams() {
        return new CloudbreakApiClientParams(restDebug, certificateValidation, ignorePreValidation, cloudbreakUrl);
    }

    @Bean
    public EnvironmentApiClientParams environmentApiClientParams() {
        return new EnvironmentApiClientParams(restDebug, certificateValidation, ignorePreValidation, environmentServerUrl);
    }

    @Bean
    public RedbeamsApiClientParams redbeamsApiClientParams() {
        return new RedbeamsApiClientParams(restDebug, certificateValidation, ignorePreValidation, redbeamsServerUrl);
    }

    @Bean
    public Map<DatabaseConfigKey, DatabaseConfig> databaseConfigs() throws IOException {
        ImmutableMap.Builder<DatabaseConfigKey, DatabaseConfig> builder = new ImmutableMap.Builder<>();
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            for (SdxClusterShape sdxClusterShape : SdxClusterShape.values()) {
                Optional<DatabaseConfig> dbConfig = readDbConfig(cloudPlatform, sdxClusterShape);
                if (dbConfig.isPresent()) {
                    builder.put(new DatabaseConfigKey(cloudPlatform, sdxClusterShape), dbConfig.get());
                }
            }
        }
        return builder.build();
    }

    private Optional<DatabaseConfig> readDbConfig(CloudPlatform cloudPlatform, SdxClusterShape sdxClusterShape) throws IOException {
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
