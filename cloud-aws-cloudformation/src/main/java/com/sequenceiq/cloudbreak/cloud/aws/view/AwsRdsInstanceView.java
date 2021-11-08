package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

public class AwsRdsInstanceView {

    @VisibleForTesting
    static final String BACKUP_RETENTION_PERIOD = "backupRetentionPeriod";

    @VisibleForTesting
    static final String ENGINE_VERSION = "engineVersion";

    @VisibleForTesting
    static final String MULTI_AZ = "multiAZ";

    @VisibleForTesting
    static final String STORAGE_TYPE = "storageType";

    private final DatabaseServer databaseServer;

    public AwsRdsInstanceView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public Long getAllocatedStorage() {
        return databaseServer.getStorageSize();
    }

    public Integer getBackupRetentionPeriod() {
        return databaseServer.getParameter(BACKUP_RETENTION_PERIOD, Integer.class);
    }

    public String getDBInstanceClass() {
        return databaseServer.getFlavor();
    }

    public String getDBInstanceIdentifier() {
        return databaseServer.getServerId();
    }

    public String getEngine() {
        if (databaseServer.getEngine() == null) {
            return null;
        }
        switch (databaseServer.getEngine()) {
            case POSTGRESQL:
                return "postgres";
            default:
                throw new IllegalStateException("Unsupported RDS engine " + databaseServer.getEngine());
        }
    }

    public String getEngineVersion() {
        return databaseServer.getStringParameter(ENGINE_VERSION);
    }

    public String getMasterUsername() {
        return databaseServer.getRootUserName();
    }

    public String getMasterUserPassword() {
        return databaseServer.getRootPassword();
    }

    public String getMultiAZ() {
        return databaseServer.getStringParameter(MULTI_AZ);
    }

    public String getStorageType() {
        return databaseServer.getStringParameter(STORAGE_TYPE);
    }

    public List<String> getVPCSecurityGroups() {
        return databaseServer.getSecurity().getCloudSecurityIds();
    }

    public String getSslCertificateIdentifier() {
        return databaseServer.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER);
    }

    public boolean isSslCertificateIdentifierDefined() {
        return !Strings.isNullOrEmpty(getSslCertificateIdentifier());
    }

    public boolean isKmsCustom() {
        String encryptionKeyArn = databaseServer.getStringParameter("key");
        return !encryptionKeyArn.isEmpty() && encryptionKeyArn != null;
    }

    public String getEncryptionKeyArn() {
        return databaseServer.getStringParameter("key");
    }
}
