package com.sequenceiq.it.cloudbreak.newway.config;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.verification.Call;

import spark.Response;
import spark.Service;

@Prototype
public class SparkServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServer.class);

    private boolean initialized;

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Value("#{'${integrationtest.cloudbreak.server}' + '${server.contextPath:/cb}'}")
    private String cloudbreakServerRoot;

    @Value("${mock.server.port:#{T(java.util.concurrent.ThreadLocalRandom).current().nextInt(9750, 9900 + 1)}}")
    private int port;

    private Service sparkService;

    private final Map<Call, Response> requestResponseMap = new HashMap<>();

    private final java.util.Stack<Call> callStack = new java.util.Stack<>();

    public Map<Call, Response> getRequestResponseMap() {
        return requestResponseMap;
    }

    public Stack<Call> getCallStack() {
        return callStack;
    }

    public void initSparkService() {
        initSparkService(port);
    }

    public void initSparkService(int sparkPort) {
        port = sparkPort;
        if (sparkService == null) {
            sparkService = Service.ignite();
        }
        sparkService.port(port);
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
        initialized = true;

        LOGGER.info("SparkServer has been started on {}", getEndpoint());
    }

    public String getEndpoint() {
        return "https://" + mockServerAddress + ":" + port;
    }

    protected static File createTempFileFromClasspath(String file) {
        try {
            InputStream sshPemInputStream = new ClassPathResource(file).getInputStream();
            File tempKeystoreFile = File.createTempFile(file, ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + file, e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException(file + " not found", e);
        }
    }

    public Service getSparkService() {
        return sparkService;
    }

    public int getPort() {
        return port;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void restart() {
        stop();
        sparkService.init();
    }

    public void stop() {
        if (sparkService != null) {
            sparkService.stop();
            LOGGER.info("spark server has stopped.");
        }
    }
}
