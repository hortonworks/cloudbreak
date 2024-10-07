package com.sequenceiq.datalake.service.validation.resize;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.database.AzureDatabaseAttributesService;

@ExtendWith(MockitoExtension.class)
class SdxResizeValidatorTest {

    @InjectMocks
    private SdxResizeValidator underTest;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Test
    void testValidateDatabaseTypeForResizeShouldThrowExceptionWhenTheDatabaseTypeIsSingleServer() {
        SdxDatabase sdxDatabase = new SdxDatabase();

        when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(AzureDatabaseType.SINGLE_SERVER);

        assertThrows(BadRequestException.class, () -> underTest.validateDatabaseTypeForResize(sdxDatabase, CloudPlatform.AZURE));
    }

    @Test
    void testValidateDatabaseTypeForResizeShouldNotThrowExceptionWhenTheDatabaseTypeIsFlexibleServer() {
        SdxDatabase sdxDatabase = new SdxDatabase();

        when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);

        underTest.validateDatabaseTypeForResize(sdxDatabase, CloudPlatform.AZURE);
    }

    @Test
    void testValidateDatabaseTypeForResizeShouldNotThrowExceptionWhenTheDatabaseTypeIsNull() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(null);

        underTest.validateDatabaseTypeForResize(sdxDatabase, CloudPlatform.AZURE);
    }

    @Test
    void testValidateDatabaseTypeForResizeShouldNotThrowExceptionWhenTheCloudProviderIsAws() {
        SdxDatabase sdxDatabase = new SdxDatabase();

        underTest.validateDatabaseTypeForResize(sdxDatabase, CloudPlatform.AWS);
        verifyNoInteractions(azureDatabaseAttributesService);
    }

}