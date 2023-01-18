package com.sequenceiq.environment.api.v1.credential.model.parameters.aws;

import static com.sequenceiq.environment.api.doc.credential.CredentialModelDescription.AWS_ROLE_ARN;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RoleBasedV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RoleBasedParameters implements Serializable {

    static final String ROLE_ARN_LENGTH_VALIDATION_ERROR_MSG = "RoleArn length should be between 20 and 2048 characters";

    @NotNull
    @Schema(description = AWS_ROLE_ARN, required = true, example = "arn:aws:iam::981628461338:role/example-role")
    @Size(max = 2048, min = 20, message = ROLE_ARN_LENGTH_VALIDATION_ERROR_MSG)
    private String roleArn;

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    @Override
    public String toString() {
        return "RoleBasedParameters{" +
                "roleArn='" + roleArn + '\'' +
                '}';
    }
}
