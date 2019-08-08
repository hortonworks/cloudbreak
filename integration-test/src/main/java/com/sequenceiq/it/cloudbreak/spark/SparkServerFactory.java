package com.sequenceiq.it.cloudbreak.spark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SparkServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServerFactory.class);

    private static final AtomicInteger NEXT_PORT = new AtomicInteger(19400);

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Value("#{'${integrationtest.cloudbreak.server}' + '${server.contextPath:/cb}'}")
    private String cloudbreakServerRoot;

    @Value("${mock.server.request.response.print:false}")
    private boolean printRequestBody;

    @Inject
    private SparkServerPool sparkServerPool;

    public SparkServer construct() {
        long start = System.currentTimeMillis();
        int port = NEXT_PORT.incrementAndGet();
        String endpoint = "https://" + mockServerAddress + ':' + port;

        LOGGER.info("Try to setup with endpoint: {}", endpoint);
        SparkServer sparkServer = sparkServerPool.pop();
        sparkServer.reset(endpoint, createKeystoreTempFile(), port, printRequestBody);
        sparkServer.init();
        sparkServer.awaitInitialization();
        LOGGER.info("Spark has been initalized in {}ms", System.currentTimeMillis() - start);
        LOGGER.info("SparkServer has been started on {}", endpoint);

        return sparkServer;
    }

    public void release(@Nonnull SparkServer sparkServer) {
        new Thread(() -> {
            sparkServer.stop();
            sparkServer.awaitStop();
            LOGGER.info("spark server has cleared.");
            sparkServerPool.put(sparkServer);
        }).start();
    }

    private static File createKeystoreTempFile() {
        try {
            InputStream sshPemInputStream = new ClassPathResource("/keystore_server").getInputStream();
            File tempKeystoreFile = File.createTempFile("/keystore_server", ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + "/keystore_server", e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException("/keystore_server" + " not found", e);
        }
    }
}
