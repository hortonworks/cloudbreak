package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@ExtendWith(MockitoExtension.class)
class AbstractArchivistServiceTest {

    private static final long CURRENT_TIME_MILLIS = 123456L;

    @Mock
    private Clock clock;

    @InjectMocks
    private ArchivistServiceImplementation underTest;

    @Test
    void testDelete() {
        Resource resource = new Resource();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);

        underTest.delete(resource);

        assertEquals(CURRENT_TIME_MILLIS, resource.deletionTimestamp);
        assertTrue(resource.isArchived());
        verify(underTest.repository()).save(resource);
    }

    private static class ArchivistServiceImplementation extends AbstractArchivistService<Resource> {

        private WorkspaceResourceRepository<Resource, Long> repository = mock(WorkspaceResourceRepository.class);

        @Override
        protected WorkspaceResourceRepository<Resource, Long> repository() {
            return repository;
        }

        @Override
        protected void prepareDeletion(Resource resource) {

        }

        @Override
        protected void prepareCreation(Resource resource) {

        }
    }

    private static class Resource implements ArchivableResource, WorkspaceAwareResource {

        private long deletionTimestamp;

        private boolean archived;

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

        }

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public Workspace getWorkspace() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setWorkspace(Workspace workspace) {

        }

        public long getDeletionTimestamp() {
            return deletionTimestamp;
        }

        public boolean isArchived() {
            return archived;
        }
    }
}
