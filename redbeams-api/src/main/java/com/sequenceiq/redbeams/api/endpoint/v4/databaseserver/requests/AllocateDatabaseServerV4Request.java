package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static com.sequenceiq.cloudbreak.validation.ValidCrn.Effect.DENY;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDBStackV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDBStackV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp.GcpDBStackV4Parameters;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DBStack;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.ALLOCATE_DATABASE_SERVER_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllocateDatabaseServerV4Request extends ProviderParametersBase {

    public static final int RDS_NAME_MAX_LENGTH = 40;

    @Size(max = RDS_NAME_MAX_LENGTH, min = 5, message = "The length of the name must be between 5 to " + RDS_NAME_MAX_LENGTH + " inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and must start with an alphanumeric character")
    @ApiModelProperty(value = DBStack.STACK_NAME)
    private String name;

    @NotNull
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(value = DatabaseServer.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ValidCrn(resource = { CrnResourceDescriptor.ENVIRONMENT }, effect = DENY)
    @ApiModelProperty(value = DatabaseServer.CLUSTER_CRN, required = true)
    private String clusterCrn;

    @Valid
    @ApiModelProperty(DBStack.NETWORK)
    private NetworkV4StackRequest network;

    @NotNull
    @Valid
    @ApiModelProperty(value = DBStack.DATABASE_SERVER, required = true)
    private DatabaseServerV4StackRequest databaseServer;

    @ApiModelProperty(DBStack.AWS_PARAMETERS)
    private AwsDBStackV4Parameters aws;

    @ApiModelProperty(DBStack.AZURE_PARAMETERS)
    private AzureDBStackV4Parameters azure;

    @ApiModelProperty(DBStack.AZURE_PARAMETERS)
    private GcpDBStackV4Parameters gcp;

    @ApiModelProperty(DatabaseServer.SSL_CONFIG)
    private SslConfigV4Request sslConfig;

    @ApiModelProperty(DatabaseServer.TAGS)
    private Map<String, String> tags = new HashMap<>();

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

    public NetworkV4StackRequest getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4StackRequest network) {
        this.network = network;
    }

    public DatabaseServerV4StackRequest getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServerV4StackRequest databaseServer) {
        this.databaseServer = databaseServer;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    public SslConfigV4Request getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(SslConfigV4Request sslConfig) {
        this.sslConfig = sslConfig;
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
        if (gcp == null) {
            gcp = new GcpDBStackV4Parameters();
        }
        return gcp;
    }

    public GcpDBStackV4Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpDBStackV4Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public Mappable createAzure() {
        if (azure == null) {
            azure = new AzureDBStackV4Parameters();
        }
        return azure;
    }

    public void setAzure(AzureDBStackV4Parameters azure) {
        this.azure = azure;
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
        if (aws == null) {
            aws = new AwsDBStackV4Parameters();
        }
        return aws;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public AwsDBStackV4Parameters getAws() {
        return aws;
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerV4Request{" +
                "name='" + name + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", clusterCrn='" + clusterCrn + '\'' +
                ", network=" + network +
                ", databaseServer=" + databaseServer +
                ", aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                ", sslConfig=" + sslConfig +
                ", tags=" + tags +
                '}';
    }
}
