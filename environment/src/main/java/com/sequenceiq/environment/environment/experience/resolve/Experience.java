package com.sequenceiq.environment.environment.experience.resolve;

public class Experience {

    private String pathPrefix;

    private String pathInfix;

    private String port;

    public Experience(String pathPrefix, String pathInfix, String port) {
        this.pathPrefix = pathPrefix;
        this.pathInfix = pathInfix;
        this.port = port;
    }

    public Experience() {
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getPathInfix() {
        return pathInfix;
    }

    public void setPathInfix(String pathInfix) {
        this.pathInfix = pathInfix;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

}
