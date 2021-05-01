package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Disk;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.common.api.type.EncryptionType;

@Service
public class CustomGcpDiskEncryptionService {

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

    public boolean hasCustomEncryptionRequested(InstanceTemplate template) {
        return EncryptionType.CUSTOM.name()
                .equalsIgnoreCase(Optional.ofNullable(template.getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE))
                        .orElse(EncryptionType.DEFAULT.name()));
    }
}
