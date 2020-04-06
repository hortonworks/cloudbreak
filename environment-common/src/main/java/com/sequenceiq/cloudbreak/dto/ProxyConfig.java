package com.sequenceiq.cloudbreak.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class ProxyConfig implements Serializable {

    private final String crn;

    private final String name;

    private final String serverHost;

    private final Integer serverPort;

    private final String protocol;

    private final ProxyAuthentication proxyAuthentication;

    private ProxyConfig(String crn, String name, String serverHost, Integer serverPort, String protocol, ProxyAuthentication proxyAuthentication) {
        this.crn = crn;
        this.name = name;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.protocol = protocol;
        this.proxyAuthentication = proxyAuthentication;
    }

    public static ProxyConfigBuilder builder() {
        return new ProxyConfigBuilder();
    }

    public String getCrn() {
        return crn;
    }

    public String getName() {
        return name;
    }

    public String getServerHost() {
        return serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public Optional<ProxyAuthentication> getProxyAuthentication() {
        return Optional.ofNullable(proxyAuthentication);
    }

    public String getFullProxyUrl() {
        if (getProxyAuthentication().isPresent()) {
            ProxyAuthentication auth = getProxyAuthentication().get();
            return String.format("%s://%s:%s@%s:%d", protocol, auth.getUserName(), auth.getPassword(), serverHost, serverPort);
        } else {
            return String.format("%s://%s:%d", protocol, serverHost, serverPort);
        }
    }

    @Override
    public String toString() {
        return "ProxyConfig{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", serverHost='" + serverHost + '\'' +
                ", serverPort=" + serverPort +
                ", protocol='" + protocol + '\'' +
                ", proxyAuthentication=" + proxyAuthentication +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProxyConfig that = (ProxyConfig) o;
        return Objects.equals(crn, that.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn);
    }

    public static final class ProxyConfigBuilder {

        private String crn;

        private String name;

        private String serverHost;

        private Integer serverPort;

        private String protocol;

        private ProxyAuthentication proxyAuthentication;

        private ProxyConfigBuilder() {
        }

        public ProxyConfigBuilder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public ProxyConfigBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ProxyConfigBuilder withServerHost(String serverHost) {
            this.serverHost = serverHost;
            return this;
        }

        public ProxyConfigBuilder withServerPort(Integer serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public ProxyConfigBuilder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public ProxyConfigBuilder withProxyAuthentication(ProxyAuthentication proxyAuthentication) {
            this.proxyAuthentication = proxyAuthentication;
            return this;
        }

        public ProxyConfig build() {
            return new ProxyConfig(crn, name, serverHost, serverPort, protocol, proxyAuthentication);
        }
    }
}
