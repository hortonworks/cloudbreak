package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.v3.oas.annotations.media.Schema;

public class ClusterBaseJson implements Json {

    @Schema(description = ClusterJsonsProperties.HOST)
    private String host;

    @Schema(description = ClusterJsonsProperties.PORT)
    private String port;

    @Schema(description = ClusterJsonsProperties.USERNAME)
    private String user;

    @Schema(description = ClusterJsonsProperties.STACK_NAME)
    private String stackName;

    @NotNull
    @Schema(description = ClusterJsonsProperties.STACK_CRN)
    private String stackCrn;

    public ClusterBaseJson() {
    }

    public ClusterBaseJson(String stackCrn, String stackName) {
        this.stackCrn = stackCrn;
        this.stackName = stackName;
    }

    public ClusterBaseJson(String host, String port, String user, String stackCrn) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.stackCrn = stackCrn;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }
}
