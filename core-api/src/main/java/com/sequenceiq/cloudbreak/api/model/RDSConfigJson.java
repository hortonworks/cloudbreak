package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RDSConfig")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSConfigJson {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.NAME, required = true)
    private String name;
    @NotNull
    @Pattern(regexp = "^jdbc:postgresql://[-\\w\\.]*:?\\d*/?\\w*", message = "Connection URL is not valid")
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.CONNECTION_URL, required = true)
    private String connectionURL;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.DB_TYPE, required = true)
    private RDSDatabase databaseType;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.USERNAME, required = true)
    private String connectionUserName;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.PASSWORD, required = true)
    private String connectionPassword;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.HDPVERSION, required = true)
    private String hdpVersion;
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.VALIDATED)
    private boolean validated = true;
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.RDSTYPE)
    private RdsType type = RdsType.HIVE;
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.RDS_PROPERTIES)
    private Set<RdsConfigPropertyJson> properties = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getConnectionUserName() {
        return connectionUserName;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
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
