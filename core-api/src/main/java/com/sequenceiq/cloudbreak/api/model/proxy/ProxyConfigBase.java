package com.sequenceiq.cloudbreak.api.model.proxy;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ProxyConfigBase implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ProxyConfigModelDescription.NAME, required = true)
    @Size(max = 100, min = 4, message = "The length of the name has to be in range of 4 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String name;

    @Size(max = 1000, message = "The length of the description cannot be longer than 1000 character")
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = ProxyConfigModelDescription.SERVER_HOST, required = true)
    @Size(max = 255, min = 1, message = "The length of the server host has to be in range of 1 to 255")
    @Pattern(regexp = "(^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$)",
            message = "The server host can only contain lowercase alphanumeric characters and hyphens and dots")
    private String serverHost;

    @NotNull(message = "Server port is required")
    @ApiModelProperty(value = ProxyConfigModelDescription.SERVER_PORT, required = true)
    @Min(value = 1, message = "Port value must be greater than 0")
    @Max(value = 65535, message = "Port value cannot be greater than 65535")
    private Integer serverPort;

    @NotNull
    @Pattern(regexp = "^http(s)?$")
    @ApiModelProperty(value = ProxyConfigModelDescription.PROTOCOL, required = true)
    private String protocol;

    @ApiModelProperty(ProxyConfigModelDescription.USERNAME)
    private String userName;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

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

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments == null ? new HashSet<>() : environments;
    }
}
