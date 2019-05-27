package com.sequenceiq.environment.api.v1.proxy.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.ProxyBase;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
@JsonInclude(Include.NON_NULL)
public class ProxyResponse extends ProxyBase {

    @ApiModelProperty(ProxyConfigDescription.PROXY_CONFIG_ID)
    private String crn;

    @ApiModelProperty(ProxyConfigDescription.USERNAME)
    private SecretResponse userName;

    @ApiModelProperty(ProxyConfigDescription.PASSWORD)
    private SecretResponse password;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public SecretResponse getUserName() {
        return userName;
    }

    public void setUserName(SecretResponse userName) {
        this.userName = userName;
    }

    public SecretResponse getPassword() {
        return password;
    }

    public void setPassword(SecretResponse password) {
        this.password = password;
    }
}
