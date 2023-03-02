package com.sequenceiq.it.cloudbreak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProxyConfigProperties {

    @Value("${integrationtest.proxy.protocol:http}")
    private String proxyProtocol;

    @Value("${integrationtest.proxy.host:1.2.3.4}")
    private String proxyHost;

    @Value("${integrationtest.proxy.port:9}")
    private Integer proxyPort;

    @Value("${integrationtest.proxy.noproxyhosts:noproxy.com}")
    private String proxyNoProxyHosts;

    @Value("${integrationtest.proxy.user:mock}")
    private String proxyUser;

    @Value("${integrationtest.proxy.password:akarmi}")
    private String proxyPassword;

    @Value("${integrationtest.proxy.user2:mock2}")
    private String proxyUser2;

    @Value("${integrationtest.proxy.password2:akarmi2}")
    private String proxyPassword2;

    public String getProxyProtocol() {
        return proxyProtocol;
    }

    public void setProxyProtocol(String proxyProtocol) {
        this.proxyProtocol = proxyProtocol;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyNoProxyHosts() {
        return proxyNoProxyHosts;
    }

    public void setProxyNoProxyHosts(String proxyNoProxyHosts) {
        this.proxyNoProxyHosts = proxyNoProxyHosts;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getProxyUser2() {
        return proxyUser2;
    }

    public void setProxyUser2(String proxyUser2) {
        this.proxyUser2 = proxyUser2;
    }

    public String getProxyPassword2() {
        return proxyPassword2;
    }

    public void setProxyPassword2(String proxyPassword2) {
        this.proxyPassword2 = proxyPassword2;
    }
}
