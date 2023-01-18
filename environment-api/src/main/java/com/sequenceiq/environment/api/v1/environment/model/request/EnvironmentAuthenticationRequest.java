package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentAuthenticationV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentAuthenticationRequest implements Serializable {

    public static final String DEFAULT_USER_NAME = "cloudbreak";

    @Schema(description = EnvironmentModelDescription.PUBLIC_KEY)
    private String publicKey;

    @Size(max = 255, message = "Maximum length of the publicKeyId is 255 characters.")
    @Schema(description = EnvironmentModelDescription.PUBLIC_KEY_ID)
    private String publicKeyId;

    @Size(max = 32, min = 1, message = "The length of the user name has to be in range of 1 to 32")
    @Pattern(regexp = "(^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\\$)$)",
            message = "The user name can only contain lowercase alphanumeric characters, digits, underscores and hyphens; and has to start with "
                    + "an alphanumeric character. It may end with the dollar sign ($).")
    @Schema(description = EnvironmentModelDescription.LOGIN_USER_NAME)
    private String loginUserName = DEFAULT_USER_NAME;

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
        this.loginUserName = ObjectUtils.defaultIfNull(loginUserName, DEFAULT_USER_NAME);
    }

    @Override
    public String toString() {
        return "EnvironmentAuthenticationRequest{" +
                "publicKey='" + publicKey + '\'' +
                ", publicKeyId='" + publicKeyId + '\'' +
                ", loginUserName='" + loginUserName + '\'' +
                '}';
    }
}
