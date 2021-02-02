package com.sequenceiq.it.cloudbreak.mock;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.mock.response.MockResponse;

@Component
public class ExecuteQueryToMockInfrastructure {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteQueryToMockInfrastructure.class);

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    @PostConstruct
    void log() {
        Log.log(LOGGER, "Mock-infrastructure host: %s", mockInfrastructureHost);
    }

    public void call(String path, Function<WebTarget, WebTarget> decorateWebTarget) {
        execute(path, decorateWebTarget, r -> null);
    }

    public <T> T execute(String path, Function<Response, T> handleResponse) {
        return execute(path, w -> w, handleResponse);
    }

    public <T> T execute(String path, Function<WebTarget, WebTarget> decorateWebTarget, Function<Response, T> handleResponse) {
        pollUntilPkixGone();
        WebTarget target = buildWebTarget(path, decorateWebTarget);
        try (Response response = target.request().get()) {
            return handleResponse.apply(response);
        } catch (Exception e) {
            Log.log(LOGGER, "Cannot execute query on path: {}", path);
            throw e;
        }
    }

    private void pollUntilPkixGone() {
        boolean success = false;
        long attempt = 0;
        long maxAttempt = 30;
        long wait = 5;
        while (!success && attempt < maxAttempt) {
            WebTarget webTarget = buildWebTarget("/tests/new", w -> w);
            try (Response ignore = webTarget.request().get()) {
                success = true;
            } catch (ProcessingException e) {
                attempt++;
                try {
                    TimeUnit.SECONDS.sleep(wait);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                LOGGER.info(e.getMessage());
                Log.log(LOGGER, "Try the next attempt %s/%s,", attempt, maxAttempt);
            }
        }
    }

    public void executeConfigure(Map<String, String> pathVariables, MockResponse body) {
        executeConfigure(pathVariables, w -> w, body);
    }

    public void executeConfigure(Map<String, String> pathVariables, Function<WebTarget, WebTarget> decorateWebTarget, MockResponse body) {
        String configuredPath = body.getPath();
        for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
            configuredPath = configuredPath.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        Invocation.Builder invocation = buildWebTarget("/configure", decorateWebTarget).request();
        body.setPath(configuredPath);
        try (Response ignore = invocation.post(Entity.json(body))) {

        }
    }

    private synchronized WebTarget buildWebTarget(String path, Function<WebTarget, WebTarget> decorateWebTarget) {
//        CertificateTrustManager.SavingX509TrustManager x509TrustManager = new CertificateTrustManager.SavingX509TrustManager();
//        TrustManager[] trustManagers = {x5
//        09TrustManager};
//        SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
//        try {
//            sslContext.init(null, trustManagers, new SecureRandom());
//        } catch (KeyManagementException e) {
//            throw new TestFailException("Cannot init SSL Context: " + e.getMessage(), e);
//        }
//        Client client = RestClientUtil.createClient(sslContext, true);
        Client client = RestClientUtil.get(ConfigKey.builder().withSecure(true).build());
        WebTarget target = client.target(getUrl());
        target = decorateWebTarget.apply(target.path(path));
        return target;
    }

    public String getUrl() {
        return String.format("https://%s:%d", mockInfrastructureHost, 10090);
    }
}
