package com.sequenceiq.it.cloudbreak.spark;

import static java.lang.String.format;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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

    private String endpoint;

    private Integer port;

    private boolean shutdown;

    public SparkServer() {
        sparkService = Service.ignite();
        shutdown = true;
        endpoint = "init";
        port = -1;
    }

    public void reset(String endpoint, File keystoreFile, int port, boolean printRequestBody) {
        if (!shutdown) {
            //throw new TestException("Mock is active! Could not reset.");
            LOGGER.info("Mock is active! Could not reset.");
        }
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
        sparkService.get(VALIDATIONCALL, (request, response) -> "OK");
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

    public void awaitInitialization() throws InterruptedException {
        LOGGER.info("Spark service initialization in progress.");

        sparkService.awaitInitialization();

        HttpHelper client = HttpHelper.getInstance();
        String validationEndpoint = getEndpoint() + VALIDATIONCALL;
        Pair<javax.ws.rs.core.Response.StatusType, String> resultContent = null;
        Exception resultException = null;
        int count = 0;
        do {
            try {
                resultContent = client.getContent(validationEndpoint);
            } catch (Exception exception) {
                resultException = exception;
                LOGGER.info("Waiting for spark server validation call, {}", validationEndpoint, exception);
                if (count++ > 30) {
                    break;
                }
                Thread.sleep(2000);
            }
        }
        while (resultContent == null);

        if (resultContent == null) {
            throw new TestException("Waiting for spark server failed", resultException);
        } else if (!resultContent.getKey().getReasonPhrase().equals("OK")) {
            throw new TestException("Waiting for spark server failed, http reason: " + resultContent.getKey().getReasonPhrase());
        }
        LOGGER.info("Spark service initialization finished.");
    }

    public void awaitStop() {
        sparkService.awaitStop();
    }

    public void init() {
        callStack.clear();
        requestResponseMap.clear();
        sparkService.init();
        shutdown = false;
    }

    public void stop() {
        shutdown = true;
        endpoint = this + "spark has been stopped ";
        sparkService.stop();
    }

    @Override
    public String toString() {
        return super.toString() + " " + endpoint;
    }
}