package com.sequenceiq.cloudbreak.cloud.mock;

import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.RestClientUtil;

@Component
public class MockUrlFactory {

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    public Invocation.Builder get(String path) throws KeyManagementException {
        CertificateTrustManager.SavingX509TrustManager x509TrustManager = new CertificateTrustManager.SavingX509TrustManager();
        TrustManager[] trustManagers = {x509TrustManager};
        SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
        sslContext.init(null, trustManagers, new SecureRandom());
        Client client = RestClientUtil.createClient(sslContext, false);
        WebTarget nginxTarget = client.target(String.format("https://%s:10090", mockInfrastructureHost));
        return nginxTarget.path(path).request();
    }
}
