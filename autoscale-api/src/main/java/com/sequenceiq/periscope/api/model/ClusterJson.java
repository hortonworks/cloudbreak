package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ClusterSummary")
public class ClusterJson implements Json {

    @ApiModelProperty(ClusterJsonProperties.ID)
    private long id;
    @ApiModelProperty(ClusterJsonProperties.HOST)
    private String host;
    @ApiModelProperty(ClusterJsonProperties.PORT)
    private String port;
    @ApiModelProperty(ClusterJsonProperties.STATE)
    private String state;
    @ApiModelProperty(ClusterJsonProperties.STACKID)
    private Long stackId;

    public ClusterJson() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
