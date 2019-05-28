package com.sequenceiq.cloudbreak.dto;

import java.io.Serializable;
import java.util.Objects;

public class ProxyConfig implements Serializable {

    private final String crn;

    private final String name;

    private final String serverHost;

    private final Integer serverPort;

    private final String protocol;

    private final String userName;

    private final String password;

    private final String accountId;

    private final String userCrn;

    //CHECKSTYLE:OFF
    private ProxyConfig(String crn, String name, String serverHost, Integer serverPort, String protocol, String userName, String password, String accountId,
            String userCrn) {
        this.crn = crn;
        this.name = name;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.protocol = protocol;
        this.userName = userName;
        this.password = password;
        this.accountId = accountId;
        this.userCrn = userCrn;
    }
    //CHECKSTYLE:ON

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

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUserCrn() {
        return userCrn;
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

        private String userName;

        private String password;

        private String accountId;

        private String userCrn;

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

        public ProxyConfigBuilder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public ProxyConfigBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public ProxyConfigBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public ProxyConfigBuilder withUserCrn(String userCrn) {
            this.userCrn = userCrn;
            return this;
        }

        public ProxyConfig build() {
            return new ProxyConfig(crn, name, serverHost, serverPort, protocol, userName, password, accountId, userCrn);
        }
    }
}
