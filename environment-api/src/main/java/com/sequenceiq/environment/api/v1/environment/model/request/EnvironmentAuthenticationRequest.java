package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("EnvironmentAuthenticationV1Request")
public class EnvironmentAuthenticationRequest implements Serializable {

    public static final String DEFAULT_USER_NAME = "cloudbreak";

    @ApiModelProperty(EnvironmentModelDescription.PUBLIC_KEY)
    private String publicKey;

    @Size(max = 255, message = "Maximum length of the publicKeyId is 255 characters.")
    @ApiModelProperty(EnvironmentModelDescription.PUBLIC_KEY_ID)
    private String publicKeyId;

    @Size(max = 32, min = 1, message = "The length of the user name has to be in range of 1 to 32")
    @Pattern(regexp = "(^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\\$)$)",
            message = "The user name can only contain lowercase alphanumeric characters, digits, underscores and hyphens; and has to start with "
                    + "an alphanumeric character. It may end with the dollar sign ($).")
    @ApiModelProperty(EnvironmentModelDescription.LOGIN_USER_NAME)
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
