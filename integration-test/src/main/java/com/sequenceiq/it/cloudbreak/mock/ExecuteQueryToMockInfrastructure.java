package com.sequenceiq.it.cloudbreak.mock;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class ExecuteQueryToMockInfrastructure {

    @Value("${mock.infrastructure.host:localhost}")
    private String infrastructureMockHost;

    public void call(String path, Function<WebTarget, WebTarget> decorateWebTarget) {
        execute(path, decorateWebTarget, r -> null);
    }

    public <T> T execute(String path, Function<Response, T> handleResponse) {
        return execute(path, w -> null, handleResponse);
    }

    public <T> T execute(String path, Function<WebTarget, WebTarget> decorateWebTarget, Function<Response, T> handleResponse) {
        WebTarget target = buildWebTarget(path, decorateWebTarget);
        try (Response response = target.request().get()) {
            return handleResponse.apply(response);
        }
    }

    public void executeConfigure(String path, Map<String, String> pathVariables, Object body) {
        executeConfigure(path, pathVariables, w -> w, body);
    }

    public void executeConfigure(String path, Map<String, String> pathVariables, Function<WebTarget, WebTarget> decorateWebTarget, Object body) {
        WebTarget webTarget = buildWebTarget(path + "/configure", decorateWebTarget);
        for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
            webTarget = webTarget.resolveTemplate(entry.getKey(), entry.getValue());
        }
        Invocation.Builder invocation = webTarget.request();
        try (Response ignore = invocation.post(Entity.json(body))) {

        }
    }

    private WebTarget buildWebTarget(String path, Function<WebTarget, WebTarget> decorateWebTarget) {
        CertificateTrustManager.SavingX509TrustManager x509TrustManager = new CertificateTrustManager.SavingX509TrustManager();
        TrustManager[] trustManagers = {x509TrustManager};
        SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
        try {
            sslContext.init(null, trustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new TestFailException("Cannot init SSL Context: " + e.getMessage(), e);
        }
        Client client = RestClientUtil.createClient(sslContext, true);
        WebTarget target = client.target(String.format("https://%s:%d", infrastructureMockHost, 10090));
        target = decorateWebTarget.apply(target.path(path));
        return target;
    }
}
