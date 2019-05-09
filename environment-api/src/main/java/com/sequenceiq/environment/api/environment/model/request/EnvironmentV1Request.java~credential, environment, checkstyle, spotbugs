package com.sequenceiq.environment.api.environment.model.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class EnvironmentV1Request extends EnvironmentV1BaseRequest implements CredentialAwareEnvV1Request {

    @Size(max = 100, min = 5, message = "The length of the environments's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The environments's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_REQUEST)
    private CredentialV1Request credential;

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private Set<String> regions = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    @NotNull
    private LocationV1Request location;

    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkV1Request network;

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
    public CredentialV1Request getCredential() {
        return credential;
    }

    @Override
    public void setCredential(CredentialV1Request credential) {
        this.credential = credential;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions == null ? new HashSet<>() : regions;
    }

    public LocationV1Request getLocation() {
        return location;
    }

    public void setLocation(LocationV1Request location) {
        this.location = location;
    }

    public EnvironmentNetworkV1Request getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkV1Request network) {
        this.network = network;
    }
}
