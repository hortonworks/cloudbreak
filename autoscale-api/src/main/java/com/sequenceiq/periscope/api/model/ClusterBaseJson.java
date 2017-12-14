package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.annotations.ApiModelProperty;

public class ClusterBaseJson implements Json {

    @ApiModelProperty(ClusterJsonsProperties.HOST)
    private String host;

    @ApiModelProperty(ClusterJsonsProperties.PORT)
    private String port;

    @ApiModelProperty(ClusterJsonsProperties.USERNAME)
    private String user;

    @ApiModelProperty(ClusterJsonsProperties.STACK_ID)
    @NotNull
    private Long stackId;

    public ClusterBaseJson() {
    }

    public ClusterBaseJson(String host, String port, String user, Long stackId) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.stackId = stackId;
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

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

}
