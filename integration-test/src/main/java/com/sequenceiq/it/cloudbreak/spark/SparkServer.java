package com.sequenceiq.it.cloudbreak.spark;

import static java.lang.String.format;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestException;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;
import com.sequenceiq.it.verification.Call;

import spark.Response;
import spark.Service;

public class SparkServer {

    public static final String VALIDATIONCALL = "/validationcall";

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServer.class);

    private final Map<Call, Response> requestResponseMap = new HashMap<>();

    private final java.util.Stack<Call> callStack = new java.util.Stack<>();

    private final Service sparkService;

    private final String endpoint;

    private final boolean printRequestBody;

    private boolean secure;

    private int port;

    public SparkServer(int port, File keystoreFile, String endpoint, boolean printRequestBody, boolean secure) {
        this.port = port;
        this.endpoint = endpoint;
        this.printRequestBody = printRequestBody;
        sparkService = Service.ignite();
        sparkService.port(port);
        sparkService.threadPool(30, 30, 600000);
        this.secure = secure;
        if (secure) {
            sparkService.secure(keystoreFile.getPath(), "secret", null, null);
        }
    }

    public void reset() {
        stop();
        awaitStop();
        init();
        awaitInitialization();
    }

    public void init() {
        callStack.clear();
        requestResponseMap.clear();
        sparkService.init();
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
        sparkService.get(VALIDATIONCALL, (request, response) -> "OK");
    }

    public Map<Call, Response> getRequestResponseMap() {
        return Collections.unmodifiableMap(requestResponseMap);
    }

    public String getEndpoint() {
        return (secure ? "https://"  : "http://") + endpoint + ":" + port;
    }

    public Service getSparkService() {
        return sparkService;
    }

    public void awaitInitialization() {
        LOGGER.info("Spark service initialization in progress on port: {}.", port);
        sparkService.awaitInitialization();
        waitEndpointToBeReady(VALIDATIONCALL, null);
        LOGGER.info("Spark service initialization finished on port {}.", port);
    }

    public void waitEndpointToBeReady(String path, String expectedResponseBody) {
        LOGGER.info("Wait endpoint to be ready: {}", path);
        HttpHelper client = HttpHelper.getInstance();
        String validationEndpoint = getEndpoint() + path;
        Pair<javax.ws.rs.core.Response.StatusType, String> resultContent = null;
        Exception resultException = null;
        int count = 0;
        do {
            try {
                resultContent = client.getContent(validationEndpoint);
                if (expectedResponseBody != null) {
                    if (!expectedResponseBody.equals(resultContent.getValue())) {
                        LOGGER.info("Expected body is different. Expected: {}, Result: {}", expectedResponseBody, resultContent.getValue());
                        throw new TestException("Expected body is different");
                    }
                }
            } catch (Exception exception) {
                resultException = exception;
                LOGGER.info("Waiting for spark server validation call, {}", validationEndpoint, exception);
                if (count++ > 30) {
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new TestException("Waiting for spark server failed", e);
                }
            }
        }
        while (resultContent == null);

        if (resultContent == null) {
            throw new TestException("Waiting for spark server has failed", resultException);
        } else if (!resultContent.getKey().getReasonPhrase().equals("OK")) {
            throw new TestException("Waiting for spark server has failed, http reason: " + resultContent.getKey().getReasonPhrase());
        }
    }

    public void awaitStop() {
        sparkService.awaitStop();
    }

    public void stop() {
        sparkService.stop();
    }

    public int getPort() {
        return port;
    }

    public boolean isSecure() {
        return secure;
    }

    @Override
    public String toString() {
        return super.toString() + " " + endpoint;
    }
}