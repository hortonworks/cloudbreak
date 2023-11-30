package com.sequenceiq.externalizedcompute.liftie.client;

import static com.google.common.base.Preconditions.checkNotNull;

public class LiftieEndpoint {

    private static final String API_PATH_PREFIX = "";

    private String host;

    private int port;

    public LiftieEndpoint(String host, int port) {
        this.host = checkNotNull(host);
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = checkNotNull(host);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBasePath() {
        return String.format("%s%s:%d%s", "http://", host, port, API_PATH_PREFIX);
    }
}