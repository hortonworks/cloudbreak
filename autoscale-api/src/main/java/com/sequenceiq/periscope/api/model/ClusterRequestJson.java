package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ClusterRequestJson")
public class ClusterRequestJson extends ClusterBaseJson {

    @ApiModelProperty(ClusterJsonsProperties.PASSWORD)
    private String pass;

    @ApiModelProperty(ClusterJsonsProperties.ENABLE_AUTOSCALING)
    private boolean enableAutoscaling;

    public ClusterRequestJson() {
    }

    public ClusterRequestJson(String host, String port, String user, String pass, Long stackId, boolean enableAutoscaling) {
        super(host, port, user, stackId);
        this.pass = pass;
        this.enableAutoscaling = enableAutoscaling;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean enableAutoscaling() {
        return enableAutoscaling;
    }

    public void setEnableAutoscaling(boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }
}
