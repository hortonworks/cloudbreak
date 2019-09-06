package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.validation.ValidDatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

public class DatabaseServerV4StackBase extends ProviderParametersBase {

    @ApiModelProperty(DatabaseServerModelDescriptions.INSTANCE_TYPE)
    private String instanceType;

    @ValidDatabaseVendor
    @ApiModelProperty(DatabaseServerModelDescriptions.DATABASE_VENDOR)
    private String databaseVendor;

    @ApiModelProperty(DatabaseServerModelDescriptions.CONNECTION_DRIVER)
    private String connectionDriver;

    @ApiModelProperty(DatabaseServerModelDescriptions.STORAGE_SIZE)
    private Long storageSize;

    @ApiModelProperty(DatabaseServerModelDescriptions.ROOT_USER_NAME)
    private String rootUserName;

    @ApiModelProperty(DatabaseServerModelDescriptions.ROOT_USER_PASSWORD)
    private String rootUserPassword;

    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    @ApiModelProperty(DatabaseServerModelDescriptions.PORT)
    private Integer port;

    @ApiModelProperty(DatabaseServerModelDescriptions.AWS_PARAMETERS)
    private AwsDatabaseServerV4Parameters aws;

    @Valid
    @ApiModelProperty(DatabaseServerModelDescriptions.AZURE_PARAMETERS)
    private AzureDatabaseServerV4Parameters azure;

    // @ApiModelProperty(DatabaseServerModelDescriptions.GCP_PARAMETERS)
    // private GcpDatabaseServerV4Parameters gcp;

    // @ApiModelProperty(DatabaseServerModelDescriptions.AZURE_PARAMETERS)
    // private AzureDatabaseServerV4Parameters azure;

    // @ApiModelProperty(DatabaseServerModelDescriptions.OPEN_STACK_PARAMETERS)
    // private OpenStackDatabaseServerV4Parameters openstack;

    // @ApiModelProperty(hidden = true)
    // private MockDatabaseServerV4Parameters mock;

    // @ApiModelProperty(hidden = true)
    // private YarnDatabaseServerV4Parameters yarn;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getDatabaseVendor() {
        return databaseVendor;
    }

    public void setDatabaseVendor(String databaseVendor) {
        this.databaseVendor = databaseVendor;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public Long getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(Long storageSize) {
        this.storageSize = storageSize;
    }

    public String getRootUserName() {
        return rootUserName;
    }

    public void setRootUserName(String rootUserName) {
        this.rootUserName = rootUserName;
    }

    public String getRootUserPassword() {
        return rootUserPassword;
    }

    public void setRootUserPassword(String rootUserPassword) {
        this.rootUserPassword = rootUserPassword;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public AwsDatabaseServerV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsDatabaseServerV4Parameters();
        }
        return aws;
    }

    public void setAws(AwsDatabaseServerV4Parameters aws) {
        this.aws = aws;
    }

    @Override
    public Mappable createGcp() {
        return null;
    }

    @Override
    public Mappable createAzure() {
        if (azure == null) {
            azure = new AzureDatabaseServerV4Parameters();
        }
        return azure;
    }

    public void setAzure(AzureDatabaseServerV4Parameters azure) {
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
        return null;
    }

    public AwsDatabaseServerV4Parameters getAws() {
        return aws;
    }

    public AzureDatabaseServerV4Parameters getAzure() {
        return azure;
    }
}
