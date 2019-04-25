package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class EnvironmentV4Request extends EnvironmentV4BaseRequest implements CredentialAwareEnvV4Request {

    @Size(max = 100, min = 5, message = "The length of the environments's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The environments's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentRequestModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(EnvironmentRequestModelDescription.CREDENTIAL)
    private CredentialV4Request credential;

    @ApiModelProperty(EnvironmentRequestModelDescription.REGIONS)
    private Set<String> regions = new HashSet<>();

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION)
    @NotNull
    private LocationV4Request location;

    @ApiModelProperty(EnvironmentRequestModelDescription.NETWORK)
    private EnvironmentNetworkV4Request network;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getCredentialName() {
        return credentialName;
    }

    @Override
    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    @Override
    public CredentialV4Request getCredential() {
        return credential;
    }

    @Override
    public void setCredential(CredentialV4Request credential) {
        this.credential = credential;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions == null ? new HashSet<>() : regions;
    }

    public LocationV4Request getLocation() {
        return location;
    }

    public void setLocation(LocationV4Request location) {
        this.location = location;
    }

    public EnvironmentNetworkV4Request getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkV4Request network) {
        this.network = network;
    }
}
