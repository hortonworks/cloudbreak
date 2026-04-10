package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class DatalakeEmbeddedDbSslCertRotationContextProviderTest {

    @InjectMocks
    private DatalakeEmbeddedDbSslCertRotationContextProvider underTest;

    @Test
    void testIsApplicable() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getDatabaseAvailabilityType()).thenReturn(SdxDatabaseAvailabilityType.NONE);
        assertTrue(underTest.isApplicable(sdxCluster));
    }
}
