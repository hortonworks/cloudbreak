package com.sequenceiq.cloudbreak.cloud.mock;

import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
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
        try {
            CertificateTrustManager.SavingX509TrustManager x509TrustManager = new CertificateTrustManager.SavingX509TrustManager();
            TrustManager[] trustManagers = {x509TrustManager};
            SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
            sslContext.init(null, trustManagers, new SecureRandom());
            Client client = RestClientUtil.createClient(sslContext, false);
            WebTarget nginxTarget = client.target(String.format(s, mockInfrastructureHost));
            return nginxTarget.path(path).request();
        } catch (KeyManagementException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create SSL context");
        }
    }
}
