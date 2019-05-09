package com.sequenceiq.environment.api.proxy.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.SecretV4Response;
import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;
import com.sequenceiq.environment.api.proxy.model.ProxyV1Base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
@JsonInclude(Include.NON_NULL)
public class ProxyV1Response extends ProxyV1Base {

    @ApiModelProperty(ProxyConfigDescription.PROXY_CONFIG_ID)
    private Long id;

    @ApiModelProperty(ProxyConfigDescription.USERNAME)
    private SecretV4Response userName;

    @ApiModelProperty(ProxyConfigDescription.PASSWORD)
    private SecretV4Response password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretV4Response getUserName() {
        return userName;
    }

    public void setUserName(SecretV4Response userName) {
        this.userName = userName;
    }

    public SecretV4Response getPassword() {
        return password;
    }

    public void setPassword(SecretV4Response password) {
        this.password = password;
    }
}
