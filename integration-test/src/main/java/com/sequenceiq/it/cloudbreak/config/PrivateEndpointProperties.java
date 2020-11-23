package com.sequenceiq.it.cloudbreak.config;

import static com.sequenceiq.it.cloudbreak.PrivateEndpointTest.PRIVATE_ENDPOINT_DISABLED;
import static com.sequenceiq.it.cloudbreak.PrivateEndpointTest.PRIVATE_ENDPOINT_ENABLED;
import static com.sequenceiq.it.cloudbreak.PrivateEndpointTest.PRIVATE_ENDPOINT_USAGE;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.TestParameter;

@Component
public class PrivateEndpointProperties {

    @Value("${integrationtest.privateEndpointEnabled}")
    private boolean enabled;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() {
        testParameter.put(PRIVATE_ENDPOINT_USAGE, enabled ? PRIVATE_ENDPOINT_ENABLED : PRIVATE_ENDPOINT_DISABLED);
    }

}
