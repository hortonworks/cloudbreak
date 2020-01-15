package com.sequenceiq.environment.api.v1.environment.model.request;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentEditV1Request")
public class EnvironmentEditRequest {

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkRequest network;

    @ApiModelProperty(EnvironmentModelDescription.AUTHENTICATION)
    private @Valid EnvironmentAuthenticationRequest authentication;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY)
    private TelemetryRequest telemetry;

    @ApiModelProperty(EnvironmentModelDescription.SECURITY_ACCESS)
    private @Valid SecurityAccessRequest securityAccess;

    @ApiModelProperty(EnvironmentModelDescription.IDBROKER_MAPPING_SOURCE)
    private IdBrokerMappingSource idBrokerMappingSource;

    @ApiModelProperty(EnvironmentModelDescription.ADMIN_GROUP_NAME)
    private String adminGroupName;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.AWS_PARAMETERS)
    private AwsEnvironmentParameters aws;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EnvironmentNetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkRequest network) {
        this.network = network;
    }

    public EnvironmentAuthenticationRequest getAuthentication() {
        return authentication;
    }

    public void setAuthentication(EnvironmentAuthenticationRequest authentication) {
        this.authentication = authentication;
    }

    public TelemetryRequest getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryRequest telemetry) {
        this.telemetry = telemetry;
    }

    public SecurityAccessRequest getSecurityAccess() {
        return securityAccess;
    }

    public void setSecurityAccess(SecurityAccessRequest securityAccess) {
        this.securityAccess = securityAccess;
    }

    public IdBrokerMappingSource getIdBrokerMappingSource() {
        return idBrokerMappingSource;
    }

    public void setIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
        this.idBrokerMappingSource = idBrokerMappingSource;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public AwsEnvironmentParameters getAws() {
        return aws;
    }

    public void setAws(AwsEnvironmentParameters aws) {
        this.aws = aws;
    }
}
