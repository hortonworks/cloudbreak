package com.sequenceiq.cloudbreak.api.model.rds;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;
import com.sequenceiq.cloudbreak.validation.ValidRDSConfigJson;

import io.swagger.annotations.ApiModelProperty;

@ValidRDSConfigJson
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class RDSConfigJson implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = RDSConfig.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = RDSConfig.CONNECTION_URL, required = true)
    private String connectionURL;

    @NotNull
    @ApiModelProperty(value = RDSConfig.RDSTYPE, required = true)
    private String type;

    @ApiModelProperty(RDSConfig.CONNECTOR_JAR_URL)
    private String connectorJarUrl;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnectorJarUrl() {
        return connectorJarUrl;
    }

    public void setConnectorJarUrl(String connectorJarUrl) {
        this.connectorJarUrl = connectorJarUrl;
    }

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments;
    }
}
