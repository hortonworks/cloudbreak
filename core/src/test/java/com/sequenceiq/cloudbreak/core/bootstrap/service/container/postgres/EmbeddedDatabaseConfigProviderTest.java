package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDatabaseConfigProviderTest {

    private static final String POSTGRES_DIRECTORY_KEY = "postgres_directory";

    private static final String POSTGRES_LOG_DIRECTORY_KEY = "postgres_log_directory";

    private static final String POSTGRES_DATA_ON_ATTACHED_DISK_KEY = "postgres_data_on_attached_disk";

    private static final String POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK = "pgsql";

    private static final String POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK = "pgsql/log";

    private static final String POSTGRES_DEFAULT_DIRECTORY = "/var/lib/pgsql";

    private static final String POSTGRES_DEFAULT_LOG_DIRECTORY = "/var/log";

    private static final String POSTGRES_VERSION = "postgres_version";

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private EmbeddedDatabaseConfigProvider underTest;

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabled() {
        // GIVEN
        StackDto stack = mock(StackDto.class);
        when(stack.getStack()).thenReturn(new Stack());
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);
        // WHEN
        Map<String, Object> actualResult = underTest.collectEmbeddedDatabaseConfigs(stack);
        // THEN
        assertTrue((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        assertEquals(VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK, actualResult.get(POSTGRES_DIRECTORY_KEY));
        assertEquals(VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
        assertFalse(actualResult.containsKey(POSTGRES_VERSION));
    }

    @Test
    public void collectEmbeddedDatabaseConfigsWhenDbOnAttachedDiskDisabledOrNoAttachedVolumes() {
        // GIVEN
        StackDto stack = mock(StackDto.class);
        when(stack.getStack()).thenReturn(new Stack());
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(false);
        // WHEN
        Map<String, Object> actualResult = underTest.collectEmbeddedDatabaseConfigs(stack);
        // THEN
        assertFalse((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        assertEquals(POSTGRES_DEFAULT_DIRECTORY, actualResult.get(POSTGRES_DIRECTORY_KEY));
        assertEquals(POSTGRES_DEFAULT_LOG_DIRECTORY, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
        assertFalse(actualResult.containsKey(POSTGRES_VERSION));
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWithVersion() {
        // GIVEN
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion("testVersion");
        when(stackDto.getStack()).thenReturn(stack);
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);
        // WHEN
        Map<String, Object> actualResult = underTest.collectEmbeddedDatabaseConfigs(stackDto);
        // THEN
        assertTrue((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        assertEquals(VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK, actualResult.get(POSTGRES_DIRECTORY_KEY));
        assertEquals(VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
        assertEquals(stack.getExternalDatabaseEngineVersion(), actualResult.get(POSTGRES_VERSION));
    }

    @Test
    public void collectEmbeddedDatabaseConfigsWhenDbOnAttachedDiskDisabledOrNoAttachedVolumesWithVersion() {
        // GIVEN
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion("testVersion");
        when(stackDto.getStack()).thenReturn(stack);
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(false);
        // WHEN
        Map<String, Object> actualResult = underTest.collectEmbeddedDatabaseConfigs(stackDto);
        // THEN
        assertFalse((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        assertEquals(POSTGRES_DEFAULT_DIRECTORY, actualResult.get(POSTGRES_DIRECTORY_KEY));
        assertEquals(POSTGRES_DEFAULT_LOG_DIRECTORY, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
        assertEquals(stack.getExternalDatabaseEngineVersion(), actualResult.get(POSTGRES_VERSION));
    }
}
