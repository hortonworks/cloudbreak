package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.workload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.WorkloadAnalyticsV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkloadAnalyticsV4Request extends WorkloadAnalyticsV4Base {
    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_WA_ACCESS_KEY)
    private String accessKey;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_WA_PRIVATE_KEY)
    private String privateKey;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
