package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.AwsDBStackV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4Request;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllocateDatabaseServerV4Request extends ProviderParametersBase {

    @Size(max = 40, min = 5, message = "The length of the name must be between 5 to 40 inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and must start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.DBStack.STACK_NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @Valid
    @ApiModelProperty(ModelDescriptions.DBStack.NETWORK)
    private NetworkV4Request network;

    @Valid
    @ApiModelProperty(ModelDescriptions.DBStack.DATABASE_SERVER)
    private DatabaseServerV4Request databaseServer;

    @ApiModelProperty(ModelDescriptions.DBStack.AWS_PARAMETERS)
    private AwsDBStackV4Parameters aws;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public NetworkV4Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4Request network) {
        this.network = network;
    }

    public DatabaseServerV4Request getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServerV4Request databaseServer) {
        this.databaseServer = databaseServer;
    }

    @Override
    public AwsDBStackV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsDBStackV4Parameters();
        }
        return aws;
    }

    public void setAws(AwsDBStackV4Parameters aws) {
        this.aws = aws;
    }

    @Override
    public Mappable createGcp() {
        return null;
    }

    @Override
    public Mappable createAzure() {
        return null;
    }

    @Override
    public Mappable createOpenstack() {
        return null;
    }

    @Override
    public Mappable createYarn() {
        return null;
    }

    @Override
    public Mappable createMock() {
        return null;
    }

    public AwsDBStackV4Parameters getAws() {
        return aws;
    }

}
