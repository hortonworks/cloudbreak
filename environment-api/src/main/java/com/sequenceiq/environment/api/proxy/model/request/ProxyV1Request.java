package com.sequenceiq.environment.api.proxy.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;
import com.sequenceiq.environment.api.proxy.model.ProxyV1Base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ProxyV1Request extends ProxyV1Base {

    @ApiModelProperty(ProxyConfigDescription.USERNAME)
    private String userName;

    @ApiModelProperty(ProxyConfigDescription.PASSWORD)
    private String password;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
