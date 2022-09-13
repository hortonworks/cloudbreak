package com.sequenceiq.cloudbreak.dto.datalake;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;

public class DatalakeDto {

    private final String name;

    private final HttpClientConfig httpClientConfig;

    private final Integer gatewayPort;

    private final String user;

    private final String password;

    private final Status status;

    private DatalakeDto(DatalakeDtoBuilder builder) {
        gatewayPort = builder.gatewayPort;
        user = builder.user;
        password = builder.password;
        httpClientConfig = builder.httpClientConfig;
        status = builder.status;
        name = builder.name;
    }

    public String getName() {
        return name;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "DatalakeDto{" +
                "name='" + name + '\'' +
                ", httpClientConfig=" + httpClientConfig +
                ", gatewayPort=" + gatewayPort +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public static final class DatalakeDtoBuilder {

        private String name;

        private HttpClientConfig httpClientConfig;

        private Integer gatewayPort;

        private String user;

        private String password;

        private Status status;

        private DatalakeDtoBuilder() {
        }

        public DatalakeDto.DatalakeDtoBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public DatalakeDto.DatalakeDtoBuilder withHttpClientConfig(HttpClientConfig httpClientConfig) {
            this.httpClientConfig = httpClientConfig;
            return this;
        }

        public DatalakeDto.DatalakeDtoBuilder withGatewayPort(Integer gatewayPort) {
            this.gatewayPort = gatewayPort;
            return this;
        }

        public DatalakeDto.DatalakeDtoBuilder withUser(String user) {
            this.user = user;
            return this;
        }

        public DatalakeDto.DatalakeDtoBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public DatalakeDto.DatalakeDtoBuilder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public static DatalakeDto.DatalakeDtoBuilder aDatalakeDto() {
            return new DatalakeDto.DatalakeDtoBuilder();
        }

        public DatalakeDto build() {
            return new DatalakeDto(this);
        }
    }

}
