package com.sequenceiq.environment.api.v1.proxy.model;

import java.io.Serializable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.validation.ValidNoProxyList;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ProxyBase implements Serializable {

    @NotNull
    @ApiModelProperty(value = ProxyConfigDescription.NAME, required = true)
    @Size(max = 100, min = 4, message = "The length of the name has to be in range of 4 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String name;

    @Size(max = 1000, message = "The length of the description cannot be longer than 1000 character")
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = ProxyConfigDescription.SERVER_HOST, required = true)
    @Size(max = 255, min = 1, message = "The length of the server host has to be in range of 1 to 255")
    @Pattern(regexp = "(^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$)",
            message = "The server host can only contain lowercase alphanumeric characters and hyphens and dots")
    private String host;

    @NotNull(message = "Server port is required")
    @ApiModelProperty(value = ProxyConfigDescription.SERVER_PORT, required = true)
    @Min(value = 1, message = "Port value must be greater than 0")
    @Max(value = 65535, message = "Port value cannot be greater than 65535")
    private Integer port;

    @NotNull
    @Pattern(regexp = "^http(s)?$")
    @ApiModelProperty(value = ProxyConfigDescription.PROTOCOL, required = true)
    private String protocol;

    @ValidNoProxyList
    @ApiModelProperty(value = ProxyConfigDescription.NO_PROXY_HOSTS)
    private String noProxyHosts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNoProxyHosts() {
        return noProxyHosts;
    }

    public void setNoProxyHosts(String noProxyHosts) {
        this.noProxyHosts = noProxyHosts;
    }

    @Override
    public String toString() {
        return "ProxyBase{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", noProxyHosts='" + noProxyHosts + '\'' +
                '}';
    }
}
