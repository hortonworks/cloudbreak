package com.sequenceiq.datalake.service.pause;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseServerParameterSetter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class DatabasePauseSupportServiceTest {

    private static final String DATABASE_CRN = "database crn";

    private static final String ENVIRONMENT_CRN = "env crn";

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseServerParameterSetters;

    @InjectMocks
    private DatabasePauseSupportService victim;

    @Test
    public void sdxClusterWithoutDatabaseIsNotSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(false);

        assertFalse(victim.isDatabasePauseSupported(sdxCluster));

        verifyNoInteractions(environmentClientService);
    }

    @Test
    public void sdxClusterWithoutDatabaseCrnIsNotSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(null);

        assertFalse(victim.isDatabasePauseSupported(sdxCluster));

        verifyNoInteractions(environmentClientService);
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

    @Test
    public void cloudPlatformAndDbTypeIsSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        when(sdxCluster.getEnvCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());
        when(platformConfig.isExternalDatabasePauseSupportedFor(CloudPlatform.AZURE)).thenReturn(true);
        DatabaseServerParameterSetter databaseServerParameterSetter = mock(DatabaseServerParameterSetter.class);
        when(databaseServerParameterSetters.get(CloudPlatform.AZURE)).thenReturn(databaseServerParameterSetter);
        doReturn(Optional.of(AzureDatabaseType.FLEXIBLE_SERVER)).when(databaseServerParameterSetter).getDatabaseType(null);
        assertTrue(victim.isDatabasePauseSupported(sdxCluster));
    }

    @Test
    public void cloudPlatformAndDbTypeIsNotSupported() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        when(sdxCluster.getEnvCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());
        when(platformConfig.isExternalDatabasePauseSupportedFor(CloudPlatform.AZURE)).thenReturn(true);
        DatabaseServerParameterSetter databaseServerParameterSetter = mock(DatabaseServerParameterSetter.class);
        when(databaseServerParameterSetters.get(CloudPlatform.AZURE)).thenReturn(databaseServerParameterSetter);
        doReturn(Optional.of(AzureDatabaseType.SINGLE_SERVER)).when(databaseServerParameterSetter).getDatabaseType(null);
        assertFalse(victim.isDatabasePauseSupported(sdxCluster));
    }
}
