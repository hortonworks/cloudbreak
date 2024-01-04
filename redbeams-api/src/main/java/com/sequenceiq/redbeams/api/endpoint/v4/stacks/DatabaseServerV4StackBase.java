package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.validation.ValidDatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp.GcpDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

public class DatabaseServerV4StackBase extends ProviderParametersBase {

    @Schema(description = DatabaseServerModelDescriptions.INSTANCE_TYPE)
    private String instanceType;

    @ValidDatabaseVendor
    @Schema(description = DatabaseServerModelDescriptions.DATABASE_VENDOR)
    private String databaseVendor;

    @Schema(description = DatabaseServerModelDescriptions.CONNECTION_DRIVER)
    private String connectionDriver;

    @Schema(description = DatabaseServerModelDescriptions.STORAGE_SIZE)
    private Long storageSize;

    @Schema(description = DatabaseServerModelDescriptions.ROOT_USER_NAME)
    private String rootUserName;

    @Schema(description = DatabaseServerModelDescriptions.ROOT_USER_PASSWORD)
    private String rootUserPassword;

    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    @Schema(description = DatabaseServerModelDescriptions.PORT)
    private Integer port;

    @Schema(description = DatabaseServerModelDescriptions.AWS_PARAMETERS)
    private AwsDatabaseServerV4Parameters aws;

    @Valid
    @Schema(description = DatabaseServerModelDescriptions.AZURE_PARAMETERS)
    private AzureDatabaseServerV4Parameters azure;

    @Valid
    @Schema(description = DatabaseServerModelDescriptions.GCP_PARAMETERS)
    private GcpDatabaseServerV4Parameters gcp;

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
        if (gcp == null) {
            gcp = new GcpDatabaseServerV4Parameters();
        }
        return gcp;
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
    public Mappable createYarn() {
        return null;
    }

    @Override
    public Mappable createMock() {
        if (aws == null) {
            aws = new AwsDatabaseServerV4Parameters();
        }
        return aws;
    }

    public AwsDatabaseServerV4Parameters getAws() {
        return aws;
    }

    public AzureDatabaseServerV4Parameters getAzure() {
        return azure;
    }

    public GcpDatabaseServerV4Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpDatabaseServerV4Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public String toString() {
        return "DatabaseServerV4StackBase{" +
                "instanceType='" + instanceType + '\'' +
                ", databaseVendor='" + databaseVendor + '\'' +
                ", connectionDriver='" + connectionDriver + '\'' +
                ", storageSize=" + storageSize +
                ", rootUserName='" + rootUserName + '\'' +
                ", port=" + port +
                ", aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                "} " + super.toString();
    }
}
