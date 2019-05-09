package com.sequenceiq.environment.api.credential.model;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.environment.api.credential.doc.CredentialModelDescription;
import com.sequenceiq.environment.api.credential.model.parameters.aws.AwsCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.cumulus.CumulusYarnCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.gcp.GcpCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.mock.MockCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.openstack.OpenstackCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.yarn.YarnCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Response;
import com.sequenceiq.environment.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {CredentialV1Request.class, CredentialV1Response.class})
public abstract class CredentialV1Base implements Serializable {

    @Size(max = 100, min = 5, message = "The length of the credential's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name of the credential can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true, allowableValues = "length range[5, 100]")
    private String name;

    @NotNull
    @ApiModelProperty(value = CredentialModelDescription.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @Valid
    @ApiModelProperty(CredentialModelDescription.AWS_PARAMETERS)
    private AwsCredentialV1Parameters aws;

    @Valid
    @ApiModelProperty(CredentialModelDescription.GCP_PARAMETERS)
    private GcpCredentialV1Parameters gcp;

    @Valid
    @ApiModelProperty(CredentialModelDescription.OPENSTACK_PARAMETERS)
    private OpenstackCredentialV1Parameters openstack;

    @Valid
    @ApiModelProperty(CredentialModelDescription.CUMULUS_YARN_PARAMETERS)
    private CumulusYarnCredentialV1Parameters cumulus;

    @Valid
    @ApiModelProperty(CredentialModelDescription.YARN_PARAMETERS)
    private YarnCredentialV1Parameters yarn;

    @Valid
    @ApiModelProperty(hidden = true)
    private MockCredentialV1Parameters mock;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    public MockCredentialV1Parameters getMock() {
        return mock;
    }

    public void setMock(MockCredentialV1Parameters mock) {
        this.mock = mock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public AwsCredentialV1Parameters getAws() {
        return aws;
    }

    public void setAws(AwsCredentialV1Parameters aws) {
        this.aws = aws;
    }

    public GcpCredentialV1Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpCredentialV1Parameters gcp) {
        this.gcp = gcp;
    }

    public OpenstackCredentialV1Parameters getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenstackCredentialV1Parameters openstack) {
        this.openstack = openstack;
    }

    public CumulusYarnCredentialV1Parameters getCumulus() {
        return cumulus;
    }

    public void setCumulus(CumulusYarnCredentialV1Parameters cumulus) {
        this.cumulus = cumulus;
    }

    public YarnCredentialV1Parameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnCredentialV1Parameters yarn) {
        this.yarn = yarn;
    }

}
