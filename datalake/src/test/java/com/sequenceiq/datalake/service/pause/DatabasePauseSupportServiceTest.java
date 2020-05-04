package com.sequenceiq.datalake.service.pause;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatabasePauseSupportServiceTest {

    private static final String DATABASE_CRN = "database crn";

    private static final String ENVIRONMENT_CRN = "env crn";

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private PlatformConfig platformConfig;

    @InjectMocks
    private DatabasePauseSupportService victim;

    @Test
    public void sdxClusterWithoutDatabaseIsNotSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(false);

        assertFalse(victim.isDatabasePauseSupported(sdxCluster));

        verifyZeroInteractions(environmentClientService);
    }

    @Test
    public void sdxClusterWithoutDatabaseCrnIsNotSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(null);

        assertFalse(victim.isDatabasePauseSupported(sdxCluster));

        verifyZeroInteractions(environmentClientService);
    }

    @Test
    public void cloudPlatformIsNotSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        when(sdxCluster.getEnvCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());
        when(platformConfig.isExternalDatabasePauseSupportedFor(CloudPlatform.AZURE)).thenReturn(false);
        assertFalse(victim.isDatabasePauseSupported(sdxCluster));
    }

    @Test
    public void cloudPlatformIsSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        when(sdxCluster.getEnvCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(platformConfig.isExternalDatabasePauseSupportedFor(CloudPlatform.AWS)).thenReturn(true);

        assertTrue(victim.isDatabasePauseSupported(sdxCluster));
    }
}