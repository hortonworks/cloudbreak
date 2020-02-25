package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackAuthenticationBase;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackAuthenticationV4Base implements JsonEntity {

    @Size(max = 2048, message = "The length of the publicKey has to be smaller than 2048")
    @ApiModelProperty(StackAuthenticationBase.PUBLIC_KEY)
    private String publicKey;

    @Size(max = 255, message = "The length of the publicKeyId has to be smaller than 255")
    @ApiModelProperty(StackAuthenticationBase.PUBLIC_KEY_ID)
    private String publicKeyId;

    @Size(max = 32, message = "The length of the loginUserName has to be smaller than 32")
    @ApiModelProperty(StackAuthenticationBase.LOGIN_USERNAME)
    private String loginUserName;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public void setLoginUserName(String loginUserName) {
        this.loginUserName = loginUserName;
    }
}
