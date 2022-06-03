package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.UPGRADE_DATABASE_SERVER_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpgradeDatabaseServerV4Request {

    @NotEmpty
    @ApiModelProperty(value = ModelDescriptions.UpgradeModelDescriptions.MAJOR_VERSION, required = true)
    private String majorVersion;

    public String getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(String majorVersion) {
        this.majorVersion = majorVersion;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerV4Request{" +
                "majorVersion='" + majorVersion + '\'' +
                '}';
    }
}
