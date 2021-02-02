package com.sequenceiq.cloudbreak.cloud.mock;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class MockUrlFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockUrlFactory.class);

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    @Inject
    private MockStackUtil mockStackUtil;

    @PostConstruct
    void log() {
        LOGGER.info("Mock-infrastructure host: {}", mockInfrastructureHost);
    }

    public Invocation.Builder get(String path) {
        return getBuilder(path, "https://%s:10090");
    }

    public Invocation.Builder get(AuthenticatedContext ac, String path) {
        return getBuilder(path, "https://%s:10090/" + mockStackUtil.getStackName(ac));
    }

    private Invocation.Builder getBuilder(String path, String s) {
        ConfigKey config = ConfigKey.builder()
                /* TODO: there is a JVM bug where the TrustAllCertStore does not work correctly in multi threaded environments
                    and can default back to the original X509TrustStoreImpl. openjdk:11.0.6 */
                .withSecure(false)
                .withDebug(false)
                .build();

        Client client = RestClientUtil.get(config);
        WebTarget nginxTarget = client.target(String.format(s, mockInfrastructureHost));
        return nginxTarget.path(path).request();
    }
}
