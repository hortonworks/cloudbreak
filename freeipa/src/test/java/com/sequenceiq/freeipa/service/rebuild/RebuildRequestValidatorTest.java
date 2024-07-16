package com.sequenceiq.freeipa.service.rebuild;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class RebuildRequestValidatorTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private RebuildRequestValidator underTest;

    private RebuildV2Request request;

    private Stack stack;

    @BeforeEach
    public void setUp() {
        request = new RebuildV2Request();
        request.setResourceCrn("test-crn");
        request.setInstanceToRestoreFqdn("test-instance");
        request.setFullBackupStorageLocation("s3://valid-path");
        request.setDataBackupStorageLocation("s3://valid-path");

        stack = new Stack();
        stack.setResourceCrn("test-crn");
        stack.setCloudPlatform("AWS");
        stack.setId(1L);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("test-instance");

        lenient().when(instanceMetaDataService.findAllInstancesForStack(stack.getId())).thenReturn(Set.of(instanceMetaData));
    }

    @Test
    public void testValidateSuccess() {
        assertDoesNotThrow(() -> underTest.validate(request, stack));
    }

    @Test
    public void testValidateInvalidCrn() {
        request.setResourceCrn("invalid-crn");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, stack));
        assertEquals("Invalid resource crn: invalid-crn", exception.getMessage());
    }

    @Test
    public void testValidateInstanceDoesNotExist() {
        when(instanceMetaDataService.findAllInstancesForStack(stack.getId())).thenReturn(Collections.emptySet());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, stack));
        assertEquals("Instance does not exist: test-instance", exception.getMessage());
    }

    @Test
    public void testValidateInvalidPath() {
        request.setFullBackupStorageLocation("invalid-path");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, stack));
        assertEquals("Invalid storage location path: [invalid-path]. It must start with one of the protocols: [s3, s3a, s3n]", exception.getMessage());
    }

    @Test
    public void testValidateUnsupportedCloudPlatform() {
        stack.setCloudPlatform("OPENSTACK");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, stack));
        assertEquals("Cloudplatform not supported for rebuild: OPENSTACK", exception.getMessage());
    }

    @Test
    public void testValidateAzureInvalidPath() {
        stack.setCloudPlatform("AZURE");
        request.setFullBackupStorageLocation("invalid-path");
        request.setDataBackupStorageLocation("invalid-path");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, stack));
        assertEquals("Invalid storage location path: [invalid-path]. It must start with one of the protocols: [abfs, abfss]", exception.getMessage());
    }

    @Test
    public void testValidateGcpInvalidPath() {
        stack.setCloudPlatform("GCP");
        request.setFullBackupStorageLocation("invalid-path");
        request.setDataBackupStorageLocation("invalid-path");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(request, stack));
        assertEquals("Invalid storage location path: [invalid-path]. It must start with one of the protocols: [gcs, gs]", exception.getMessage());
    }

    @Test
    public void testValidateAzureValidPath() {
        stack.setCloudPlatform("AZURE");
        request.setFullBackupStorageLocation("abfs://valid-path");
        request.setDataBackupStorageLocation("abfs://valid-path");

        assertDoesNotThrow(() -> underTest.validate(request, stack));
    }

    @Test
    public void testValidateGcpValidPath() {
        stack.setCloudPlatform("GCP");
        request.setFullBackupStorageLocation("gs://valid-path");
        request.setDataBackupStorageLocation("gs://valid-path");

        assertDoesNotThrow(() -> underTest.validate(request, stack));
    }
}
