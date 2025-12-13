package com.sequenceiq.cloudbreak.dto;

import static org.springframework.util.Assert.hasLength;

import java.io.Serializable;

public class ProxyAuthentication implements Serializable {
    private final String userName;

    private final String password;

    private ProxyAuthentication(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public static final class Builder {
        private String userName;

        private String password;

        private Builder() {
        }

        public ProxyAuthentication.Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public ProxyAuthentication.Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public ProxyAuthentication build() {
            hasLength(userName, "Username cannot be empty");
            hasLength(password, "Password cannot be empty");
            return new ProxyAuthentication(userName, password);
        }
    }
}
