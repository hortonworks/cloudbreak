package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class RDSConfigJson implements JsonEntity {

    @NotNull
    @Pattern(regexp = "^jdbc:postgresql://[-\\w\\.]*:\\d{1,5}/?\\w*", message = "Connection URL is not valid")
    @ApiModelProperty(value = RDSConfig.CONNECTION_URL, required = true)
    private String connectionURL;

    @NotNull
    @ApiModelProperty(value = RDSConfig.DB_TYPE, required = true)
    private RDSDatabase databaseType;

    @NotNull
    @ApiModelProperty(value = RDSConfig.HDPVERSION, required = true)
    private String hdpVersion;

    @ApiModelProperty(RDSConfig.VALIDATED)
    private boolean validated = true;

    @ApiModelProperty(RDSConfig.RDSTYPE)
    private RdsType type = RdsType.HIVE;

    @ApiModelProperty(RDSConfig.RDS_PROPERTIES)
    private Set<RdsConfigPropertyJson> properties = new HashSet<>();

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public RDSDatabase getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(RDSDatabase databaseType) {
        this.databaseType = databaseType;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public RdsType getType() {
        return type;
    }

    public void setType(RdsType type) {
        this.type = type;
    }

    public Set<RdsConfigPropertyJson> getProperties() {
        return properties;
    }

    public void setProperties(Set<RdsConfigPropertyJson> properties) {
        this.properties = properties;
    }
}
