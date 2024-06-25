package com.sequenceiq.it.cloudbreak.mock;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;

import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public abstract class AbstractRestCallExecutor {

    public void call(String path, Function<WebTarget, WebTarget> decorateWebTarget) {
        execute(path, decorateWebTarget, r -> null);
    }

    public <T> T execute(String path, Function<Response, T> handleResponse) {
        return execute(path, w -> w, handleResponse);
    }

    public <T> T execute(String path, Function<WebTarget, WebTarget> decorateWebTarget, Function<Response, T> handleResponse) {
        WebTarget target = buildWebTarget(path, decorateWebTarget, Map.of());
        try (Response response = target.request().get()) {
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                return handleResponse.apply(response);
            }
            throw new TestFailException(response.readEntity(String.class));
        }
    }

    public void executeMethod(Method method, String path, Map<String, String> parameters, Entity<?> body, Consumer<Response> proc, Function<WebTarget,
            WebTarget> deco) {
        WebTarget target = buildWebTarget(path, deco, parameters);
        proc.accept(target.request().method(method.getMethodName().toUpperCase(Locale.ROOT), body));
    }

    protected WebTarget buildWebTarget(String path, Function<WebTarget, WebTarget> decorateWebTarget, Map<String, String> parameters) {
        CertificateTrustManager.SavingX509TrustManager x509TrustManager = new CertificateTrustManager.SavingX509TrustManager();
        TrustManager[] trustManagers = {x509TrustManager};
        SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
        try {
            sslContext.init(null, trustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new TestFailException("Cannot init SSL Context: " + e.getMessage(), e);
        }
        Client client = RestClientUtil.createClient(sslContext, true);
        WebTarget target = client.target(getUrl());
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            target = target.queryParam(param.getKey(), param.getValue());
        }
        target = decorateWebTarget.apply(target.path(path));
        return target;
    }

    protected abstract String getUrl();
}
