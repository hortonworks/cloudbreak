package com.sequenceiq.environment.api.v1.proxy.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.ProxyBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
@JsonInclude(Include.NON_NULL)
public class ProxyViewResponse extends ProxyBase {

    @ApiModelProperty(ProxyConfigDescription.PROXY_CONFIG_ID)
    private String crn;

    @ApiModelProperty(ProxyConfigDescription.CREATOR)
    private String creator;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}
