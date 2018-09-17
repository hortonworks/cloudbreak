package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.Mock.createTempFileFromClasspath;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.sequenceiq.it.cloudbreak.newway.mock.MockModel;
import com.sequenceiq.it.verification.Call;

import spark.Response;
import spark.Service;

public class SparkMockEntity extends Entity {
    private int port;

    private String hostname;

    private MockModel model;

    private String credentialName;

    private final Map<Call, Response> requestResponseMap = new HashMap<>();

    private final java.util.Stack<Call> callStack = new java.util.Stack<Call>();

    private Service sparkService;

    public SparkMockEntity() {

    }

    public SparkMockEntity(String id, String hostname, int port) {
        super(id);
        this.port = port;
        this.hostname = hostname;
    }

    public Map<Call, Response> getRequestResponseMap() {
        return requestResponseMap;
    }

    public Stack<Call> getCallStack() {
        return callStack;
    }

    public void initSparkService() {
        if (sparkService == null) {
            sparkService = Service.ignite();
        }
        sparkService.port(getPort());
        File keystoreFile = createTempFileFromClasspath("/keystore_server");
        sparkService.secure(keystoreFile.getPath(), "secret", null, null);
        sparkService.before((req, res) -> res.type("application/json"));
        sparkService.after(
                (request, response) -> requestResponseMap.put(Call.fromRequest(request), response));
        sparkService.after(
                (request, response) -> callStack.push(Call.fromRequest(request))
        );

        callStack.clear();
        requestResponseMap.clear();
    }

    public void stop() {
        if (sparkService != null) {
            sparkService.stop();
        }
    }

    public void restart() {
        stop();
        sparkService = Service.ignite();
        initSparkService();
    }

    public String getHostname() {
        return hostname;
    }

    public MockModel getModel() {
        return model;
    }

    public void setAndStart(MockModel model) {
        this.model = model;
        restart();
        model.startModel(getSparkService(), getHostname());
    }

    public int getPort() {
        return port;
    }

    protected Service getSparkService() {
        return sparkService;
    }

    protected String getCredentialName() {
        return credentialName;
    }

    public String getEndpoint() {
        return "https://" + getHostname() + ":" + getPort();
    }

    public void setCredentialName(String name) {
        credentialName = name;
    }
}
