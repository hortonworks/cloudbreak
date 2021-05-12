package com.sequenceiq.environment.service.integration.testconfiguration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

@TestConfiguration
@EntityScan(basePackages = {"com.sequenceiq.flow.domain",
        "com.sequenceiq.environment",
        "com.sequenceiq.cloudbreak.ha.domain",
        "com.sequenceiq.cloudbreak.structuredevent.domain"})
@Profile("test")
public class TestConfigurationWithoutCloudAccess {
    @MockBean
    private SecretService secretService;

    @MockBean
    private FreeIpaV1Endpoint freeIpaV1Endpoint;
}
