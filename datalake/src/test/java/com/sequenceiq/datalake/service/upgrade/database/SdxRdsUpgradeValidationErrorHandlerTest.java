package com.sequenceiq.datalake.service.upgrade.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
class SdxRdsUpgradeValidationErrorHandlerTest {
    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxRdsUpgradeValidationErrorHandler underTest;

    @Test
    void testHandleUpgradeValidationErrorWhenNoAutoMigration() {
        SdxCluster sdxCluster = new SdxCluster();
        Exception exception = new Exception("exception");

        underTest.handleUpgradeValidationError(sdxCluster, exception);

        Mockito.verify(sdxService, Mockito.never()).save(sdxCluster);
    }

    @Test
    void testHandleUpgradeValidationErrorWhenAutoMigrationHappened() {
        SdxCluster sdxCluster = new SdxCluster();
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxCluster.setSdxDatabase(sdxDatabase);
        Exception exception = new Exception(AzureDatabaseType.AZURE_AUTOMIGRATION_ERROR_PREFIX);
        Mockito.when(sdxService.save(sdxCluster)).thenReturn(sdxCluster);

        SdxCluster actualResult = underTest.handleUpgradeValidationError(sdxCluster, exception);

        assertEquals(actualResult.getSdxDatabase().getAttributesMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY), AzureDatabaseType.FLEXIBLE_SERVER.name());
        Mockito.verify(sdxService, Mockito.times(1)).save(sdxCluster);
    }
}
