package com.sequenceiq.it.cloudbreak.spark;

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

    private final String endpoint;

    public SparkServer(String endpoint) {
        this.endpoint = endpoint;
    }

    public void reset() {
        stop();
        awaitStop();
        init();
        awaitInitialization();
    }

    public void init() {
    }

    public String getEndpoint() {
        return "https://%s:10090";
    }

    public Service getSparkService() {
        throw new UnsupportedOperationException("Please don't use this. WE handle all request in a separated service");
    }

    public void awaitInitialization() {
//        LOGGER.info("Spark service initialization in progress on port: {}.", port);
//        sparkService.awaitInitialization();
//        waitEndpointToBeReady(VALIDATIONCALL, null);
//        LOGGER.info("Spark service initialization finished on port {}.", port);
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
//        sparkService.awaitStop();
    }

    public void stop() {
//        sparkService.stop();
    }

    public int getPort() {
        return 10090;
    }

    public boolean isSecure() {
//        return secure;
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + " " + endpoint;
    }
}