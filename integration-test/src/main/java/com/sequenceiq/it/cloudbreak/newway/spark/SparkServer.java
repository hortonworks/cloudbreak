package com.sequenceiq.it.cloudbreak.newway.spark;

import static java.lang.String.format;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.verification.Call;

import spark.Response;
import spark.Service;

public class SparkServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServer.class);

    private final Map<Call, Response> requestResponseMap = new HashMap<>();

    private final java.util.Stack<Call> callStack = new java.util.Stack<>();

    private final Service sparkService;

    private String endpoint;

    private Integer port;

    public SparkServer() {
        sparkService = Service.ignite();
    }

    public void reset(String endpoint, File keystoreFile, int port, boolean printRequestBody) {
        this.endpoint = endpoint;
        this.port = port;
        sparkService.port(port);
        sparkService.secure(keystoreFile.getPath(), "secret", null, null);
        sparkService.before((req, res) -> res.type("application/json"));
        sparkService.after(
                (request, response) -> {
                    if (printRequestBody) {
                        LOGGER.info(format("%s request from %s --> %s", request.requestMethod(), request.url(), request.body()));
                        LOGGER.info(format("response from [%s] ::: [%d] --> %s", request.url(), response.status(), response.body()));
                    }
                    requestResponseMap.put(Call.fromRequest(request), response);
                });
        sparkService.after(
                (request, response) -> callStack.push(Call.fromRequest(request))
        );
    }

    public Map<Call, Response> getRequestResponseMap() {
        return Collections.unmodifiableMap(requestResponseMap);
    }

    public Vector<Call> getCallStack() {
        return (Vector<Call>) callStack.clone();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Integer getPort() {
        return port;
    }

    public Service getSparkService() {
        return sparkService;
    }

    public void awaitInitialization() {
        sparkService.awaitInitialization();
    }

    public void awaitStop() {
        sparkService.awaitStop();
    }

    public void init() {
        callStack.clear();
        requestResponseMap.clear();
        sparkService.init();
    }

    public void stop() {
        sparkService.stop();
    }
}