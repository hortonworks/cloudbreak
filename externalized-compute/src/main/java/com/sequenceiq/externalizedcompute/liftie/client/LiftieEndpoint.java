package com.sequenceiq.externalizedcompute.liftie.client;

import static com.google.common.base.Preconditions.checkNotNull;

public class LiftieEndpoint {

    private static final String API_PATH_PREFIX = "";

    private String protocol;

    private String host;

    private int port;

    public LiftieEndpoint(String protocol, String host, int port) {
        this.protocol = checkNotNull(protocol);
        this.host = checkNotNull(host);
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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
        return String.format("%s%s:%d%s", protocol, host, port, API_PATH_PREFIX);
    }
}