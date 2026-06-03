package com.sequenceiq.it.cloudbreak.mock;

import java.util.Map;
import java.util.function.Function;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.testcase.mock.response.MockResponse;

@Component
public class ExecuteQueryToMockInfrastructure extends AbstractRestCallExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteQueryToMockInfrastructure.class);

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    public <T> T executeMethod(HttpMethod method, String path, Map<String, String> parameters, Entity<?> body, Function<Response, T> proc, Function<WebTarget,
            WebTarget> deco) {
        WebTarget target = buildWebTarget(path, deco, parameters);
        return proc.apply(target.request().method(method.name(), body));
    }

    public void executeConfigure(Map<String, String> pathVariables, MockResponse body) {
        executeConfigure(pathVariables, w -> w, body);
    }

    public void executeConfigure(Map<String, String> pathVariables, Function<WebTarget, WebTarget> decorateWebTarget, MockResponse body) {
        String configuredPath = body.getPath();
        for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
            configuredPath = configuredPath.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        Invocation.Builder invocation = buildWebTarget("/configure", decorateWebTarget, Map.of()).request();
        body.setPath(configuredPath);
        try (Response ignore = invocation.post(Entity.json(body))) {
            LOGGER.debug("Successfully configured mock infrastructure for path {}", configuredPath);
        } catch (Exception e) {
            LOGGER.warn("Failed to configure mock infrastructure for path {}: {}", configuredPath, e.getMessage(), e);
        }
    }

    public void executeClearConfigure(Map<String, String> pathVariables, MockResponse body) {
        String configuredPath = body.getPath();
        for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
            configuredPath = configuredPath.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        Invocation.Builder invocation = buildWebTarget("/configure/clear", w -> w, Map.of()).request();
        body.setPath(configuredPath);
        try (Response ignore = invocation.post(Entity.json(body))) {
            LOGGER.debug("Successfully cleared mock infrastructure configuration for path {}", configuredPath);
        } catch (Exception e) {
            LOGGER.warn("Failed to clear mock infrastructure configuration for path {}: {}", configuredPath, e.getMessage(), e);
        }
    }

    @Override
    protected String getUrl() {
        return String.format("https://%s:%d", mockInfrastructureHost, 10090);
    }
}
