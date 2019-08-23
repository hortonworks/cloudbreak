package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.logger.ThreadBasedRequestIdProvider;
import com.sequenceiq.datalake.service.sdx.DatabaseConfig;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;
import com.sequenceiq.redbeams.client.RedbeamsServiceClientBuilder;
import com.sequenceiq.redbeams.client.RedbeamsServiceCrnClient;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements AsyncConfigurer {

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

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Bean
    public CloudbreakServiceUserCrnClient cloudbreakClient() {
        return new CloudbreakUserCrnClientBuilder(cloudbreakUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public EnvironmentServiceCrnClient environmentServiceClient() {
        return new EnvironmentServiceClientBuilder(environmentServerUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public RedbeamsServiceCrnClient redbeamsServiceCrnClient() {
        return new RedbeamsServiceClientBuilder(redbeamsServerUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public Map<SdxClusterShape, DatabaseConfig> databaseConfigs() throws IOException {
        return ImmutableMap.<SdxClusterShape, DatabaseConfig>builder()
                .put(SdxClusterShape.CUSTOM, readDbConfig(SdxClusterShape.CUSTOM))
                .put(SdxClusterShape.LIGHT_DUTY, readDbConfig(SdxClusterShape.LIGHT_DUTY))
                .put(SdxClusterShape.MEDIUM_DUTY_HA, readDbConfig(SdxClusterShape.MEDIUM_DUTY_HA))
                .build();
    }

    private DatabaseConfig readDbConfig(SdxClusterShape sdxClusterShape) throws IOException {
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspath("sdx/aws/database-" + sdxClusterShape.toString()
                .toLowerCase()
                .replaceAll("_", "-") + "-template.json");
        return JsonUtil.readValue(databaseTemplateJson, DatabaseConfig.class);
    }
}
