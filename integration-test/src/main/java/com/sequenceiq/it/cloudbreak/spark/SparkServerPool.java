package com.sequenceiq.it.cloudbreak.spark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class SparkServerPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServerPool.class);

    private final BlockingQueue<SparkServer> secureServers;

    private final String endpoint;

    public SparkServerPool(int initialSparkPoolSize, boolean printRequestBody, String endpoint) {
        this.endpoint = endpoint;
        secureServers = new LinkedBlockingQueue<>(initialSparkPoolSize);
        initializeSpark(secureServers);
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

    private SparkServer popServer(BlockingQueue<SparkServer> servers) {
        long start = System.currentTimeMillis();
//        LOGGER.info("Spark server popping. Queue size: {}", servers.size());
        SparkServer sparkServer;
        try {
            sparkServer = servers.poll(1, TimeUnit.SECONDS);
//            if (sparkServer == null) {
//                throw new TestFailException("Can't take spark server from pool in time");
//            }
        } catch (InterruptedException e) {
            LOGGER.error("Can't pop spark server", e);
            throw new TestFailException("Can't take spark server from pool", e);
        }
        LOGGER.info("POP chosen one: {}", sparkServer);
        LOGGER.info("POP state: {}", sparkServer.getEndpoint());
        logServers();
        sparkServer.init();
        sparkServer.awaitInitialization();
        LOGGER.info("Spark has been initalized in {}ms", System.currentTimeMillis() - start);
        return sparkServer;
    }

    private void initializeSpark(BlockingQueue<SparkServer> servers) {
        LOGGER.info("Initialize spark server. queue size: {}", servers.size());
        SparkServer server = new SparkServer(endpoint);
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
                throw new TestFailException("Can't add spark server", e);
            }
        }
    }

    @PreDestroy
    public void autoShutdown() {
        LOGGER.info("Invoking PreDestroy for Spark Pool bean");
        secureServers.forEach(SparkServer::stop);
    }
}
