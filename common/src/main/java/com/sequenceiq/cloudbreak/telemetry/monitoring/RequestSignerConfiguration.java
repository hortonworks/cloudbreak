package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class RequestSignerConfiguration {

    private boolean enabled;

    private Integer port;

    private String user;

    private boolean useToken;

    private Integer tokenValidityMin;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isUseToken() {
        return useToken;
    }

    public void setUseToken(boolean useToken) {
        this.useToken = useToken;
    }

    public Integer getTokenValidityMin() {
        return tokenValidityMin;
    }

    public void setTokenValidityMin(Integer tokenValidityMin) {
        this.tokenValidityMin = tokenValidityMin;
    }
}
