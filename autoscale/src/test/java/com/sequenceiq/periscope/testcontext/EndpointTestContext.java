package com.sequenceiq.periscope.testcontext;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@TestConfiguration
@EntityScan(basePackages = {"com.sequenceiq.periscope"})
@Profile("test")
public class EndpointTestContext {
    @MockBean
    private SecretService secretService;
}
