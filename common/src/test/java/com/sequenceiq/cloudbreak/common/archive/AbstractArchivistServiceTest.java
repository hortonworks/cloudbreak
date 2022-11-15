package com.sequenceiq.cloudbreak.common.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.common.service.Clock;

public class AbstractArchivistServiceTest {
    private static final long CURRENT_TIME_MILLIS = 123456L;

    @Mock
    private Clock clock;

    @InjectMocks
    private ArchivistServiceImplementation underTest;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDelete() {
        Resource resource = new Resource();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);

        underTest.delete(resource);

        assertEquals(CURRENT_TIME_MILLIS, resource.deletionTimestamp);
        assertTrue(resource.isArchived());
        assertTrue(resource.isUnsetRelationsDone());
        verify(underTest.repository()).save(resource);
    }

    private static class ArchivistServiceImplementation extends AbstractArchivistService<Resource> {

        private final JpaRepository<Resource, Long> repository = mock(JpaRepository.class);

        @Override
        public JpaRepository<Resource, Long> repository() {
            return repository;
        }
    }

    private static class Resource implements ArchivableResource {

        private long deletionTimestamp;

        private boolean archived;

        private boolean unsetRelationsDone;

        @Override
        public void setDeletionTimestamp(Long timestampMillisecs) {
            deletionTimestamp = timestampMillisecs;
        }

        @Override
        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        @Override
        public void unsetRelationsToEntitiesToBeDeleted() {
            unsetRelationsDone = true;
        }

        public long getDeletionTimestamp() {
            return deletionTimestamp;
        }

        boolean isArchived() {
            return archived;
        }

        boolean isUnsetRelationsDone() {
            return unsetRelationsDone;
        }
    }
}