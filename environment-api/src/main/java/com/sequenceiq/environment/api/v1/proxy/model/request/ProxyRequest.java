package com.sequenceiq.environment.api.v1.proxy.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.ProxyBase;
import com.sequenceiq.environment.api.v1.proxy.validation.ValidProxyConfigAuthRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@ValidProxyConfigAuthRequest
public class ProxyRequest extends ProxyBase {
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

    @Override
    public String toString() {
        return super.toString() + ", " + "ProxyRequest{" +
                "userName='" + userName + '\'' +
                '}';
    }
}
