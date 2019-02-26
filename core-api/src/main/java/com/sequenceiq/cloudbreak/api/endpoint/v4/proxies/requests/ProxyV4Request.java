package com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigModelDescription.DESCRIPTION)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ProxyV4Request extends ProxyV4Base {

    @ApiModelProperty(ProxyConfigModelDescription.USERNAME)
    private String userName;

    @ApiModelProperty(ProxyConfigModelDescription.PASSWORD)
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
