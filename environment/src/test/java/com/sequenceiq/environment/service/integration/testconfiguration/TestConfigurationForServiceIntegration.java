package com.sequenceiq.environment.service.integration.testconfiguration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;
import com.sequenceiq.environment.environment.service.LoadBalancerPollerConfig;
import com.sequenceiq.environment.service.integration.DummySecretService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

@TestConfiguration
@EntityScan(basePackages = {"com.sequenceiq.flow.domain",
        "com.sequenceiq.cloudbreak.rotation",
        "com.sequenceiq.environment",
        "com.sequenceiq.cloudbreak.ha.domain",
        "com.sequenceiq.cloudbreak.structuredevent.domain",
        "com.sequenceiq.notification.domain",
        "com.sequenceiq.cloudbreak.database"})
@Profile("test")
public class TestConfigurationForServiceIntegration {

    @MockBean
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @MockBean
    private EventBus eventBus;

    @MockBean
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @MockBean
    private LoadBalancerPollerConfig loadBalancerPollerConfig;

    @MockBean
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @Bean
    public SecretService secretService() {
        return new DummySecretService();
    }
}
