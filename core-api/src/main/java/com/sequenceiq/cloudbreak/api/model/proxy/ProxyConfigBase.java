package com.sequenceiq.cloudbreak.api.model.proxy;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ProxyConfigBase implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ProxyConfigModelDescription.NAME, required = true)
    private String name;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = ProxyConfigModelDescription.SERVER_HOST, required = true)
    private String serverHost;

    @NotNull
    @ApiModelProperty(value = ProxyConfigModelDescription.SERVER_PORT, required = true)
    private Integer serverPort;

    @NotNull
    @Pattern(regexp = "^http(s)?$")
    @ApiModelProperty(value = ProxyConfigModelDescription.PROTOCOL, required = true)
    private String protocol;

    @ApiModelProperty(ProxyConfigModelDescription.USERNAME)
    private String userName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
