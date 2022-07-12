package com.sequenceiq.cloudbreak.domain.view;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.util.NullUtil;

public interface RdsConfigWithoutCluster {
    Long getId();

    String getName();

    String getDescription();

    String getConnectionURL();

    RdsSslMode getSslMode();

    DatabaseVendor getDatabaseEngine();

    String getConnectionDriver();

    Long getCreationDate();

    String getStackVersion();

    ResourceStatus getStatus();

    String getType();

    String getConnectorJarUrl();

    boolean isArchived();

    Long getDeletionTimestamp();

    Secret getConnectionUserNameSecret();

    Secret getConnectionPasswordSecret();

    default String getConnectionUserName() {
        return NullUtil.getIfNotNull(getConnectionUserNameSecret(), Secret::getRaw);
    }

    default String getConnectionPassword() {
        return NullUtil.getIfNotNull(getConnectionPasswordSecret(), Secret::getRaw);
    }

    default String getConnectionUserNamePath() {
        return NullUtil.getIfNotNull(getConnectionUserNameSecret(), Secret::getSecret);
    }

    default String getConnectionPasswordPath() {
        return NullUtil.getIfNotNull(getConnectionPasswordSecret(), Secret::getSecret);
    }
}
