package com.sequenceiq.cloudbreak.api.model.stack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackAuthenticationBase implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.StackAuthenticationBase.PUBLIC_KEY)
    private String publicKey;

    @ApiModelProperty(ModelDescriptions.StackAuthenticationBase.PUBLIC_KEY_ID)
    private String publicKeyId;

    @ApiModelProperty(ModelDescriptions.StackAuthenticationBase.LOGIN_USERNAME)
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
