package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Disk;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.EncryptionType;

@ExtendWith(MockitoExtension.class)
public class CustomGcpDiskEncryptionServiceTest {

    @InjectMocks
    private CustomGcpDiskEncryptionService underTest;

    @Mock
    private CustomGcpDiskEncryptionCreatorService customGcpDiskEncryptionCreatorService;

    @Test
    public void testAddEncryptionKeyToDiskWhenHasCustomEncryptionRequestedShouldCreateNewEncryption()  {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        Disk disk = disk();
        when(customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(any(InstanceTemplate.class)))
                .thenReturn(customerEncryptionKey);

        underTest.addEncryptionKeyToDisk(instanceTemplate(EncryptionType.CUSTOM), disk);

        assertTrue(disk.getDiskEncryptionKey().equals(customerEncryptionKey));
        verify(customGcpDiskEncryptionCreatorService, times(1)).createCustomerEncryptionKey(any(InstanceTemplate.class));
    }

    @Test
    public void testAddEncryptionKeyToDiskWhenHasNoCustomEncryptionRequestedShouldNoyCreateNewEncryption()  {
        Disk disk = disk();

        underTest.addEncryptionKeyToDisk(instanceTemplate(EncryptionType.DEFAULT), disk);

        verify(customGcpDiskEncryptionCreatorService, times(0)).createCustomerEncryptionKey(any(InstanceTemplate.class));
    }

    @Test
    public void testAddEncryptionKeyToAttachedDiskWhenHasCustomEncryptionRequestedShouldCreateNewEncryption()  {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        AttachedDisk disk = attachedDisk();
        when(customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(any(InstanceTemplate.class)))
                .thenReturn(customerEncryptionKey);

        underTest.addEncryptionKeyToDisk(instanceTemplate(EncryptionType.CUSTOM), disk);

        assertTrue(disk.getDiskEncryptionKey().equals(customerEncryptionKey));
        verify(customGcpDiskEncryptionCreatorService, times(1)).createCustomerEncryptionKey(any(InstanceTemplate.class));
    }

    @Test
    public void testAddEncryptionKeyToAttachedDiskWhenHasNoCustomEncryptionRequestedShouldNoyCreateNewEncryption()  {
        AttachedDisk disk = attachedDisk();

        underTest.addEncryptionKeyToDisk(instanceTemplate(EncryptionType.DEFAULT), disk);

        verify(customGcpDiskEncryptionCreatorService, times(0)).createCustomerEncryptionKey(any(InstanceTemplate.class));
    }

    private InstanceTemplate instanceTemplate(EncryptionType type) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, type.name());
        parameters.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "testurl");

        InstanceTemplate instanceTemplate = new InstanceTemplate("large", "master", 1L,
                new ArrayList<>(), InstanceStatus.CREATE_REQUESTED, parameters, 1L, "image", TemporaryStorage.ATTACHED_VOLUMES, 0L);

        return instanceTemplate;
    }

    public Disk disk() {
        return new Disk();
    }

    public AttachedDisk attachedDisk() {
        return new AttachedDisk();
    }
}