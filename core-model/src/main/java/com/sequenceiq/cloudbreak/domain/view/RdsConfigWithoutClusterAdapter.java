package com.sequenceiq.cloudbreak.domain.view;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

/**
 * Adapts a freshly created {@link RDSConfig} to the {@link RdsConfigWithoutCluster} projection so that callers can
 * return the just-created config without an extra database read.
 */
public class RdsConfigWithoutClusterAdapter implements RdsConfigWithoutCluster {

    private final RDSConfig rdsConfig;

    public RdsConfigWithoutClusterAdapter(RDSConfig rdsConfig) {
        this.rdsConfig = rdsConfig;
    }

    @Override
    public Long getId() {
        return rdsConfig.getId();
    }

    @Override
    public String getName() {
        return rdsConfig.getName();
    }

    @Override
    public String getDescription() {
        return rdsConfig.getDescription();
    }

    @Override
    public String getConnectionURL() {
        return rdsConfig.getConnectionURL();
    }

    @Override
    public RdsSslMode getSslMode() {
        return rdsConfig.getSslMode();
    }

    @Override
    public DatabaseVendor getDatabaseEngine() {
        return rdsConfig.getDatabaseEngine();
    }

    @Override
    public String getConnectionDriver() {
        return rdsConfig.getConnectionDriver();
    }

    @Override
    public Long getCreationDate() {
        return rdsConfig.getCreationDate();
    }

    @Override
    public String getStackVersion() {
        return rdsConfig.getStackVersion();
    }

    @Override
    public ResourceStatus getStatus() {
        return rdsConfig.getStatus();
    }

    @Override
    public String getType() {
        return rdsConfig.getType();
    }

    @Override
    public String getConnectorJarUrl() {
        return rdsConfig.getConnectorJarUrl();
    }

    @Override
    public boolean isArchived() {
        return rdsConfig.isArchived();
    }

    @Override
    public Long getDeletionTimestamp() {
        return rdsConfig.getDeletionTimestamp();
    }

    @Override
    public Secret getConnectionUserNameSecret() {
        return rdsConfig.getConnectionUserNameSecretObject();
    }

    @Override
    public Secret getConnectionPasswordSecret() {
        return rdsConfig.getConnectionPasswordSecretObject();
    }
}
