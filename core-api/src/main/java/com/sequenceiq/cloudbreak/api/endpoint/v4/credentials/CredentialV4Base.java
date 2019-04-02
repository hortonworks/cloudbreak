package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {CredentialV4Request.class, CredentialV4Response.class})
public abstract class CredentialV4Base implements JsonEntity {

    @Size(max = 100, min = 5, message = "The length of the credential's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name of the credential can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true, allowableValues = "length range[5, 100]")
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @Valid
    @ApiModelProperty(CredentialModelDescription.AWS_PARAMETERS)
    private AwsCredentialV4Parameters aws;

    @Valid
    @ApiModelProperty(CredentialModelDescription.GCP_PARAMETERS)
    private GcpCredentialV4Parameters gcp;

    @Valid
    @ApiModelProperty(CredentialModelDescription.OPENSTACK_PARAMETERS)
    private OpenstackCredentialV4Parameters openstack;

    @Valid
    @ApiModelProperty(CredentialModelDescription.CUMULUS_YARN_PARAMETERS)
    private CumulusYarnCredentialV4Parameters cumulus;

    @Valid
    @ApiModelProperty(CredentialModelDescription.YARN_PARAMETERS)
    private YarnCredentialV4Parameters yarn;

    @Valid
    @ApiModelProperty(hidden = true)
    private MockCredentialV4Parameters mock;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    public MockCredentialV4Parameters getMock() {
        return mock;
    }

    public void setMock(MockCredentialV4Parameters mock) {
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

    public AwsCredentialV4Parameters getAws() {
        return aws;
    }

    public void setAws(AwsCredentialV4Parameters aws) {
        this.aws = aws;
    }

    public GcpCredentialV4Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpCredentialV4Parameters gcp) {
        this.gcp = gcp;
    }

    public OpenstackCredentialV4Parameters getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenstackCredentialV4Parameters openstack) {
        this.openstack = openstack;
    }

    public CumulusYarnCredentialV4Parameters getCumulus() {
        return cumulus;
    }

    public void setCumulus(CumulusYarnCredentialV4Parameters cumulus) {
        this.cumulus = cumulus;
    }

    public YarnCredentialV4Parameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnCredentialV4Parameters yarn) {
        this.yarn = yarn;
    }

}
