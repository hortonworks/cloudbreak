package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Snapshot;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.GcpInstanceTemplate;
import com.sequenceiq.common.api.type.EncryptionType;

@Service
public class CustomGcpDiskEncryptionService {

    private static final String KMS_KEY_ENCRYPTION_METHOD = "KMS";

    @Inject
    private CustomGcpDiskEncryptionCreatorService customGcpDiskEncryptionCreatorService;

    public void addEncryptionKeyToDisk(InstanceTemplate template, Disk disk) {
        if (hasCustomEncryptionRequested(template)) {
            CustomerEncryptionKey customerEncryptionKey = customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(template);
            disk.setDiskEncryptionKey(customerEncryptionKey);
        }
    }

    public void addEncryptionKeyToDisk(InstanceTemplate template, AttachedDisk disk) {
        if (hasCustomEncryptionRequested(template)) {
            CustomerEncryptionKey customerEncryptionKey = customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(template);
            disk.setDiskEncryptionKey(customerEncryptionKey);
        }
    }

    /**
     * For customer-supplied encryption keys (CSEK: RAW/RSA) the source disk cannot be read by GCP without the key,
     * so the snapshot request must carry the source disk's encryption key. For KMS (CMEK) the encryption is
     * transparent to the caller, so this is a no-op.
     */
    public void addEncryptionKeyToSnapshot(InstanceTemplate template, Snapshot snapshot) {
        if (hasCustomerSuppliedEncryptionKey(template)) {
            CustomerEncryptionKey customerEncryptionKey = customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(template);
            snapshot.setSourceDiskEncryptionKey(customerEncryptionKey);
        }
    }

    /**
     * For customer-supplied encryption keys (CSEK: RAW/RSA) the source snapshot cannot be read by GCP without the key,
     * so the create-disk-from-snapshot request must carry the source snapshot's encryption key. For KMS (CMEK) the
     * encryption is transparent to the caller, so this is a no-op.
     */
    public void addSourceSnapshotEncryptionKeyToDisk(InstanceTemplate template, Disk disk) {
        if (hasCustomerSuppliedEncryptionKey(template)) {
            CustomerEncryptionKey customerEncryptionKey = customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(template);
            disk.setSourceSnapshotEncryptionKey(customerEncryptionKey);
        }
    }

    public boolean hasCustomEncryptionRequested(InstanceTemplate template) {
        return EncryptionType.CUSTOM.name()
                .equalsIgnoreCase(Optional.ofNullable(template.getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE))
                        .orElse(EncryptionType.DEFAULT.name()));
    }

    private boolean hasCustomerSuppliedEncryptionKey(InstanceTemplate template) {
        if (!hasCustomEncryptionRequested(template)) {
            return false;
        }
        String method = Optional.ofNullable(template.getStringParameter(GcpInstanceTemplate.KEY_ENCRYPTION_METHOD)).orElse("RSA");
        return !KMS_KEY_ENCRYPTION_METHOD.equalsIgnoreCase(method);
    }
}
