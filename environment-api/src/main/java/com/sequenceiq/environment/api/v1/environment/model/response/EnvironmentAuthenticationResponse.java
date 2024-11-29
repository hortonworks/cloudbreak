package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentAuthenticationV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentAuthenticationResponse implements Serializable {

    @Schema(description = EnvironmentModelDescription.PUBLIC_KEY, requiredMode = Schema.RequiredMode.REQUIRED)
    private String publicKey;

    @Schema(description = EnvironmentModelDescription.PUBLIC_KEY_ID)
    private String publicKeyId;

    @Schema(description = EnvironmentModelDescription.LOGIN_USER_NAME)
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "EnvironmentAuthenticationResponse{" +
                "publicKey='" + publicKey + '\'' +
                ", publicKeyId='" + publicKeyId + '\'' +
                ", loginUserName='" + loginUserName + '\'' +
                '}';
    }

    public static class Builder {

        private String publicKey;

        private String publicKeyId;

        private String loginUserName;

        private Builder() {
        }

        public Builder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder withPublicKeyId(String publicKeyId) {
            this.publicKeyId = publicKeyId;
            return this;
        }

        public Builder withLoginUserName(String loginUserName) {
            this.loginUserName = loginUserName;
            return this;
        }

        public EnvironmentAuthenticationResponse build() {
            EnvironmentAuthenticationResponse response = new EnvironmentAuthenticationResponse();
            response.setLoginUserName(loginUserName);
            response.setPublicKey(publicKey);
            response.setPublicKeyId(publicKeyId);
            return response;
        }
    }
}
