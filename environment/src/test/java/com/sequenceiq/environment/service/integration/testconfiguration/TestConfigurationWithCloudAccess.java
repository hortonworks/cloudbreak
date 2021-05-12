package com.sequenceiq.environment.service.integration.testconfiguration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

import reactor.core.Dispatcher;
import reactor.core.dispatch.SynchronousDispatcher;

@TestConfiguration
@EntityScan(basePackages = {"com.sequenceiq.flow.domain",
        "com.sequenceiq.environment",
        "com.sequenceiq.cloudbreak.ha.domain"})
@Profile("test")
public class TestConfigurationWithCloudAccess {
    @MockBean
    private SecretService secretService;

    @MockBean
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Bean
    public Dispatcher dispatcher(MDCCleanerThreadPoolExecutor threadPoolExecutor) {
        return new SynchronousDispatcher();
    }
}
