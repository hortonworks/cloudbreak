package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Port {

    @Id
    @GeneratedValue
    private Long id;
    private String localPort;
    private String name;
    private String port;
    private String protocol;
    @ManyToOne
    private AzureTemplate azureTemplate;

    public Port() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocalPort() {
        return localPort;
    }

    public void setLocalPort(String localPort) {
        this.localPort = localPort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @JsonIgnore
    public AzureTemplate getAzureTemplate() {
        return azureTemplate;
    }

    @JsonIgnore
    public void setAzureTemplate(AzureTemplate azureTemplate) {
        this.azureTemplate = azureTemplate;
    }
}
