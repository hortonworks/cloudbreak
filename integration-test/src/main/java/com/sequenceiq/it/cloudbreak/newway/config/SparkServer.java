package com.sequenceiq.it.cloudbreak.newway.config;


import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.verification.Call;

import spark.Response;
import spark.Service;

@Prototype
public class SparkServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServer.class);

    private static final Set<Integer> ALLOCATED_PORTS = new ConcurrentSkipListSet<>();

    private final Map<Call, Response> requestResponseMap = new HashMap<>();

    private final java.util.Stack<Call> callStack = new java.util.Stack<>();

    private boolean initialized;

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Value("#{'${integrationtest.cloudbreak.server}' + '${server.contextPath:/cb}'}")
    private String cloudbreakServerRoot;

    private Integer port;

    @Value("${mock.server.request.response.print:false}")
    private boolean printRequestBody;

    private Service sparkService;

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

    public Map<Call, Response> getRequestResponseMap() {
        return requestResponseMap;
    }

    public Stack<Call> getCallStack() {
        return callStack;
    }

    private synchronized int generatePort(int min, int max) {
        int randomPort;
        do {
            LOGGER.info("Generate new port between {} and {}", min, max);
            randomPort = ThreadLocalRandom.current().nextInt(min, max + 1);
        } while (ALLOCATED_PORTS.contains(randomPort));
        ALLOCATED_PORTS.add(randomPort);
        return randomPort;
    }

    public void initSparkService(int min, int max) {
        port = generatePort(min, max);
        if (sparkService == null) {
            LOGGER.info("Try to ignite with endpoint: {}", getEndpoint());
            sparkService = Service.ignite();
        }
        sparkService.port(port);
        File keystoreFile = createTempFileFromClasspath("/keystore_server");
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

        callStack.clear();
        requestResponseMap.clear();
        initialized = true;

        LOGGER.info("SparkServer has been started on {}", getEndpoint());
    }

    public String getEndpoint() {
        return "https://" + mockServerAddress + ":" + port;
    }

    public Service getSparkService() {
        return sparkService;
    }

    public Integer getPort() {
        return port;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void restart() {
        stop();
        sparkService.init();
    }

    @PreDestroy
    public void autoShutdown() {
        LOGGER.info("Invoking PreDestroy for Spark bean");
        shutdown();
    }

    private synchronized void deallocatePort() {
        ALLOCATED_PORTS.remove(port);
        LOGGER.info("Port deallocated: {}", port);
    }

    public void shutdown() {
        stop();
    }

    public void customAwait(Service sparkservice) {
        Field field = ReflectionUtils.findField(Service.class, "stopLatch");
        ReflectionUtils.makeAccessible(field);
        CountDownLatch latch = (CountDownLatch) ReflectionUtils.getField(field, sparkservice);
        try {
            LOGGER.info("Waiting for Spark to shutdown for 30 seconds...");
            boolean result = latch.await(30, TimeUnit.SECONDS);
            if (result) {
                deallocatePort();
            } else {
                LOGGER.error("Was not able to release spark service");
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted by another thread");
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        if (sparkService != null) {
            sparkService.stop();
            customAwait(sparkService);
            LOGGER.info("spark server has stopped.");
        }
    }
}