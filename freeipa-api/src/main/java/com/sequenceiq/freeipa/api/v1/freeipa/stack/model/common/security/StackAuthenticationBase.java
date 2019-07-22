package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.StackAuthenticationModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class StackAuthenticationBase {
    @ApiModelProperty(StackAuthenticationModelDescription.PUBLIC_KEY)
    private String publicKey;

    @ApiModelProperty(StackAuthenticationModelDescription.PUBLIC_KEY_ID)
    private String publicKeyId;

    @ApiModelProperty(StackAuthenticationModelDescription.LOGIN_USERNAME)
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

    @Override
    public String toString() {
        return "StackAuthenticationBase{"
                + "publicKey='" + publicKey + '\''
                + ", publicKeyId='" + publicKeyId + '\''
                + ", loginUserName='" + loginUserName + '\''
                + '}';
    }
}
