package com.sequenceiq.it.cloudbreak.spark;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkServerPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServerPool.class);

    private static final long TIMEOUT = 15 * 60 * 1000;

    private final Map<SparkServer, Boolean> servers = new HashMap<>();

    public SparkServer pop() {
        synchronized (servers) {
            LOGGER.info("Spark server popped. Pool size: {}", servers.entrySet().stream().filter(Entry::getValue).count());
            if (servers.entrySet().stream().noneMatch(Entry::getValue)) {
                LOGGER.info("Spark server pool is empty - creating spark server. Pool size: {}", servers.entrySet().stream().filter(Entry::getValue).count());
                SparkServer server = new SparkServer();
                servers.put(server, Boolean.FALSE);
                return server;
            }
            Optional<Entry<SparkServer, Boolean>> found = servers.entrySet().stream().filter(Entry::getValue).findFirst();
            Entry<SparkServer, Boolean> entry = found.orElseThrow();
            LOGGER.info("POP chosen one: {}", entry.getKey());
            LOGGER.info("POP state: {}", entry.getKey().getEndpoint());
            logServers();
            entry.setValue(Boolean.FALSE);
            return entry.getKey();
        }
    }

    private void logServers() {
        servers.forEach((key, value) -> LOGGER.debug("servers - [{}]", key + "::" + value));
    }

    public void put(SparkServer sparkServer) {
        synchronized (servers) {
            LOGGER.info("Spark server put back. Pool size: {}", servers.entrySet().stream().filter(Entry::getValue).count());
            LOGGER.info("PUT state: {}", sparkServer.getEndpoint());
            logServers();
            servers.put(sparkServer, Boolean.TRUE);
            servers.notify();
        }
    }

    @PreDestroy
    public void autoShutdown() {
        LOGGER.info("Invoking PreDestroy for Spark Pool bean");
        servers.keySet().forEach(SparkServer::stop);
    }
}
