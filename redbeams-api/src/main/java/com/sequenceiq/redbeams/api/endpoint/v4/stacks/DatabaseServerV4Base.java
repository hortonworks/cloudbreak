package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.validation.ValidDatabaseVendor;
// import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class DatabaseServerV4Base extends ProviderParametersBase {

    // FIXME define DatabaseServerModelDescription

    @ApiModelProperty("Instance type for database server")
    private String instanceType;

    @ValidDatabaseVendor
    @ApiModelProperty("Vendor for database server")
    private String databaseVendor;

    @ApiModelProperty("Storage size for database server")
    private Long storageSize;

    @ApiModelProperty("Root user name for database server")
    private String rootUserName;

    @ApiModelProperty("Root user password for database server")
    private String rootUserPassword;

    // @ApiModelProperty(DatabaseServerModelDescription.AWS_PARAMETERS)
    @ApiModelProperty("AWS parameters")
    private AwsDatabaseServerV4Parameters aws;

    // @ApiModelProperty(DatabaseServerModelDescription.GCP_PARAMETERS)
    // private GcpDatabaseServerV4Parameters gcp;

    // @ApiModelProperty(DatabaseServerModelDescription.AZURE_PARAMETERS)
    // private AzureDatabaseServerV4Parameters azure;

    // @ApiModelProperty(DatabaseServerModelDescription.OPEN_STACK_PARAMETERS)
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

    public AwsDatabaseServerV4Parameters getAws() {
        return aws;
    }

}
