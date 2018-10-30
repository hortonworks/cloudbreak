package com.sequenceiq.cloudbreak.api.model.proxy;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ProxyConfigRequest")
public class ProxyConfigRequest extends ProxyConfigBase {

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
