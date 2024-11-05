package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import static com.sequenceiq.common.model.AzureDatabaseType.AZURE_DATABASE_TYPE_KEY;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class RdsUpgradeValidationErrorHandlerTest {
    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private RdsUpgradeValidationErrorHandler underTest;

    @Test
    void testHandleUpgradeValidationErrorNoMigration() {
        underTest.handleUpgradeValidationError(1L, "reason");
        verify(stackDtoService, never()).getDatabaseByStackId(1L);
        verify(databaseService, never()).save(ArgumentMatchers.any());
    }

    @Test
    void testHandleUpgradeValidationErrorMigration() {
        Database database = new Database();
        Mockito.when(stackDtoService.getDatabaseByStackId(1L)).thenReturn(Optional.of(database));

        underTest.handleUpgradeValidationError(1L, AzureDatabaseType.AZURE_AUTOMIGRATION_ERROR_PREFIX);

        ArgumentCaptor<Database> databaseArgumentCaptor = ArgumentCaptor.forClass(Database.class);
        verify(databaseService).save(databaseArgumentCaptor.capture());
        assertEquals(databaseArgumentCaptor.getValue().getAttributesMap().get(AZURE_DATABASE_TYPE_KEY), FLEXIBLE_SERVER.name());
    }
}
