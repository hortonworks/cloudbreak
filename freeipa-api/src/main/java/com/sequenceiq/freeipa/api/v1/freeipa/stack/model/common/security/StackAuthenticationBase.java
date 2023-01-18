package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security;

import javax.validation.constraints.Size;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.StackAuthenticationModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class StackAuthenticationBase {

    @Size(max = 2048, message = "The length of the publicKey has to be smaller than 2048")
    @Schema(description = StackAuthenticationModelDescription.PUBLIC_KEY)
    private String publicKey;

    @Size(max = 255, message = "The length of the publicKeyId has to be smaller than 255")
    @Schema(description = StackAuthenticationModelDescription.PUBLIC_KEY_ID)
    private String publicKeyId;

    @Size(max = 32, message = "The length of the loginUserName has to be smaller than 32")
    @Schema(description = StackAuthenticationModelDescription.LOGIN_USERNAME)
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
