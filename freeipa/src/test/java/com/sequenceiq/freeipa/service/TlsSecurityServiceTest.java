package com.sequenceiq.freeipa.service;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
public class TlsSecurityServiceTest {

    @InjectMocks
    private TlsSecurityService underTest;

    @Test
    void testIllegalArgumentExceptionForGetSaltVersion() {
        InstanceMetaData instanceMetadata = new InstanceMetaData();
        instanceMetadata.setImage(Json.silent(null));
        assertNull(underTest.getInstanceSaltVersion(instanceMetadata));
    }
}
