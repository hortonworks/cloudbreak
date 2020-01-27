package com.sequenceiq.it.cloudbreak.spark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class SparkServerPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServerPool.class);

    private static final AtomicInteger NEXT_PORT = new AtomicInteger(9400);

    private final BlockingQueue<SparkServer> secureServers;

    private final BlockingQueue<SparkServer> insecureServers;

    private final String endpoint;

    private final boolean printRequestBody;

    public SparkServerPool(int initialSparkPoolSize, boolean printRequestBody, String endpoint) {
        this.printRequestBody = printRequestBody;
        this.endpoint = endpoint;
        secureServers = new LinkedBlockingQueue<>(initialSparkPoolSize);
        insecureServers = new LinkedBlockingQueue<>(initialSparkPoolSize);
        for (int i = 0; i < initialSparkPoolSize; i++) {
            initializeSpark(secureServers, true);
            initializeSpark(insecureServers, false);
        }
    }

    private File getKeyStore() {
        LOGGER.info("Preparing SparkServer keystore file");
        try {
            InputStream sshPemInputStream = new ClassPathResource("/keystore_server").getInputStream();
            File tempKeystoreFile = File.createTempFile("/keystore_server", ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + "/keystore_server", e);
                throw e;
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException("/keystore_server" + " not found", e);
        }
    }

    public SparkServer popSecure() {
        return popServer(secureServers);
    }

    public void putSecure(SparkServer sparkServer) {
        putBack(sparkServer, secureServers);
        LOGGER.info("Secure spark server put back. Pool size: {}", secureServers.size());
    }

    public SparkServer popInsecure() {
        return popServer(insecureServers);
    }

    public void putInsecure(SparkServer sparkServer) {
        putBack(sparkServer, insecureServers);
        LOGGER.info("Insecure spark server put back. Pool size: {}", secureServers.size());
    }

    private SparkServer popServer(BlockingQueue<SparkServer> servers) {
        long start = System.currentTimeMillis();
        LOGGER.info("Spark server popping. Queue size: {}", servers.size());
        SparkServer sparkServer;
        try {
            sparkServer = servers.poll(10, TimeUnit.MINUTES);
            if (sparkServer == null) {
                throw new TestFailException("Can't take spark server from pool in time");
            }
        } catch (InterruptedException e) {
            LOGGER.error("Can't pop spark server", e);
            throw new TestFailException("Can't take spark server from pool");
        }
        LOGGER.info("POP chosen one: {}", sparkServer);
        LOGGER.info("POP state: {}", sparkServer.getEndpoint());
        logServers();
        sparkServer.init();
        sparkServer.awaitInitialization();
        LOGGER.info("Spark has been initalized in {}ms", System.currentTimeMillis() - start);
        return sparkServer;
    }

    private void initializeSpark(BlockingQueue<SparkServer> servers, boolean secure) {
        LOGGER.info("Initialize spark server. queue size: {}", servers.size());
        SparkServer server = new SparkServer(NEXT_PORT.incrementAndGet(), getKeyStore(), endpoint, printRequestBody, secure);
        servers.add(server);
    }

    private void logServers() {
        secureServers.forEach(key -> LOGGER.debug("secure servers - [{}]", key));
    }

    private void putBack(SparkServer sparkServer, BlockingQueue<SparkServer> servers) {
        if (!servers.contains(sparkServer)) {
            LOGGER.info("Spark server put back. Pool size: {}", servers.size());
            LOGGER.info("PUT state: {}", sparkServer.getEndpoint());
            logServers();
            long start = System.currentTimeMillis();
            sparkServer.stop();
            sparkServer.awaitStop();
            LOGGER.info("spark server has been cleared in {}ms.", System.currentTimeMillis() - start);
            try {
                servers.add(sparkServer);
            } catch (Exception e) {
                LOGGER.error("Can't add spark server", e);
                throw new TestFailException("Can't add spark server");
            }
        }
    }

    @PreDestroy
    public void autoShutdown() {
        LOGGER.info("Invoking PreDestroy for Spark Pool bean");
        secureServers.forEach(SparkServer::stop);
    }
}
