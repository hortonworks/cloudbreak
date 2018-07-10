package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Set;

public class CloudEncryptionKeys {

    private final Set<CloudEncryptionKey> cloudEncryptionKeys;

    public CloudEncryptionKeys(Set<CloudEncryptionKey> cloudEncryptionKeys) {
        this.cloudEncryptionKeys = cloudEncryptionKeys;
    }

    public Collection<CloudEncryptionKey> getCloudEncryptionKeys() {
        return cloudEncryptionKeys;
    }

    @Override
    public String toString() {
        return "CloudEncryptionKeys{"
                + "cloudEncryptionKeys=" + cloudEncryptionKeys
                + '}';
    }
}
