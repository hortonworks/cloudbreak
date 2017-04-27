package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsBuildResult")
public class RdsBuildResult implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.HIVE_DB, required = true)
    private String hiveDbName;

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.RANGER_DB, required = true)
    private String rangerDbName;

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.AMBARI_DB, required = true)
    private String ambariDbName;

    public RdsBuildResult() {

    }

    public String getHiveDbName() {
        return hiveDbName;
    }

    public void setHiveDbName(String hiveDbName) {
        this.hiveDbName = hiveDbName;
    }

    public String getRangerDbName() {
        return rangerDbName;
    }

    public void setRangerDbName(String rangerDbName) {
        this.rangerDbName = rangerDbName;
    }

    public String getAmbariDbName() {
        return ambariDbName;
    }

    public void setAmbariDbName(String ambariDbName) {
        this.ambariDbName = ambariDbName;
    }
}
