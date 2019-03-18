package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;

public class ResourceArchivatorTest {

    private static final Long TIMESTAMP = 1234L;

    @Mock
    private Clock clock;

    @InjectMocks
    private TestResourceArchivator underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testArchive() {
        Resource resource = new Resource();
        when(clock.getCurrentTimeMillis()).thenReturn(TIMESTAMP);

        underTest.archive(resource);

        assertTrue(resource.archived);
        assertTrue(resource.relationsUnset);
        assertEquals(TIMESTAMP, resource.deletionTimeStamp);
        verify(underTest.repository).save(resource);
    }

    private static class TestResourceArchivator extends ResourceArchivator<Resource, Long> {

        private final DisabledBaseRepository<Resource, Long> repository = mock(DisabledBaseRepository.class);

        @Override
        protected DisabledBaseRepository<Resource, Long> repository() {
            return repository;
        }
    }

    private static class Resource implements ArchivableResource {

        private boolean archived;

        private Long deletionTimeStamp = -1L;

        private boolean relationsUnset;

        @Override
        public void setDeletionTimestamp(Long timestampMillisecs) {
            deletionTimeStamp = timestampMillisecs;
        }

        @Override
        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        @Override
        public void unsetRelationsToEntitiesToBeDeleted() {
            relationsUnset = true;
        }

        public boolean isArchived() {
            return archived;
        }

        public Long getDeletionTimeStamp() {
            return deletionTimeStamp;
        }

        public boolean isRelationsUnset() {
            return relationsUnset;
        }
    }
}
