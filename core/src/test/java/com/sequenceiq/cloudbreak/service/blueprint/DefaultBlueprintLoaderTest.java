package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.service.LockNumber;
import com.sequenceiq.cloudbreak.common.service.LockService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class DefaultBlueprintLoaderTest {

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private LockService lockService;

    @InjectMocks
    private DefaultBlueprintLoader underTest;

    @Test
    void testOnApplicationReadyWhenMigrationDisabledShouldDoNothing() {
        ReflectionTestUtils.setField(underTest, "globalDefaultBlueprintMigrationEnabled", false);

        underTest.onApplicationReady();

        verifyNoInteractions(lockService);
        verifyNoInteractions(blueprintService);
    }

    @Test
    void testOnApplicationReadyWhenMigrationEnabledShouldLockAndRunMigrationSuccessfully() {
        ReflectionTestUtils.setField(underTest, "globalDefaultBlueprintMigrationEnabled", true);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        underTest.onApplicationReady();

        verify(lockService).lockAndRunIfLockWasSuccessful(runnableCaptor.capture(), eq(LockNumber.BLUEPRINT));

        // Execute captured runnable to test migrateDefaultBlueprints logic
        Runnable capturedRunnable = runnableCaptor.getValue();
        assertNotNull(capturedRunnable);
        capturedRunnable.run();

        verify(blueprintService).updateDefaultBlueprintCollection((Workspace) null);
    }

    @Test
    void testOnApplicationReadyWhenMigrationEnabledAndThrowsExceptionShouldCatchAndLogException() {
        ReflectionTestUtils.setField(underTest, "globalDefaultBlueprintMigrationEnabled", true);
        doThrow(new RuntimeException("Failed to update database schema")).when(blueprintService)
                .updateDefaultBlueprintCollection((Workspace) null);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        underTest.onApplicationReady();

        verify(lockService).lockAndRunIfLockWasSuccessful(runnableCaptor.capture(), eq(LockNumber.BLUEPRINT));

        Runnable capturedRunnable = runnableCaptor.getValue();
        assertNotNull(capturedRunnable);

        // Verify that the exception thrown inside migrateDefaultBlueprints is gracefully swallowed/logged
        assertDoesNotThrow(capturedRunnable::run);
        verify(blueprintService).updateDefaultBlueprintCollection((Workspace) null);
    }
}