package com.sequenceiq.cloudbreak.api.model.rds;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class RDSConfigJson implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = RDSConfig.NAME, required = true)
    @Size(max = 50, min = 4, message = "The length of the name has to be in range of 4 to 50")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String name;

    @NotNull
    @ApiModelProperty(value = RDSConfig.CONNECTION_URL, required = true)
    @Pattern(regexp = "^jdbc:(postgresql|mysql|oracle):(thin:@|//)[-\\w\\.]*:\\d{1,5}/?:?\\w*",
            message = "Connection URL is not valid we only supports Oracle, Postgres or Mysql as database")
    private String connectionURL;

    @NotNull
    @ApiModelProperty(value = RDSConfig.RDSTYPE, required = true)
    @Size(max = 12, min = 3, message = "The length of the type has to be in range of 3 to 12")
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*[a-zA-Z0-9]$)",
            message = "The type can only contain alphanumeric characters and hyphens and has start with an alphanumeric character. "
                    + "The length of the name has to be in range of 3 to 12")
    private String type;

    @ApiModelProperty(RDSConfig.CONNECTOR_JAR_URL)
    @Size(max = 150, message = "The length of the connectorJarUrl has to be in range of 0 to 150")
    @Pattern(regexp = "^(?![\\s\\S])|http[s]://[\\w-/?=+&:,#.]*", message = "The URL must be proper and valid!")
    private String connectorJarUrl;

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
}
