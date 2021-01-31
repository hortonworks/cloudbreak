package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DATA_ON_ATTACHED_DISK_KEY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DEFAULT_DIRECTORY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DEFAULT_LOG_DIRECTORY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DIRECTORY_KEY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_LOG_DIRECTORY_KEY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDatabaseConfigProviderTest {
    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private EmbeddedDatabaseConfigProvider underTest;

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabled() {
        // GIVEN
        Stack stack = new Stack();
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);
        // WHEN
        Map<String, Object> actualResult = underTest.collectEmbeddedDatabaseConfigs(stack);
        // THEN
        assertTrue((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        assertEquals(VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK, actualResult.get(POSTGRES_DIRECTORY_KEY));
        assertEquals(VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
    }

    @Test
    public void collectEmbeddedDatabaseConfigsWhenDbOnAttachedDiskDisabledOrNoAttachedVolumes() {
        // GIVEN
        Stack stack = new Stack();
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(false);
        // WHEN
        Map<String, Object> actualResult = underTest.collectEmbeddedDatabaseConfigs(stack);
        // THEN
        assertFalse((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        assertEquals(POSTGRES_DEFAULT_DIRECTORY, actualResult.get(POSTGRES_DIRECTORY_KEY));
        assertEquals(POSTGRES_DEFAULT_LOG_DIRECTORY, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
    }
}
