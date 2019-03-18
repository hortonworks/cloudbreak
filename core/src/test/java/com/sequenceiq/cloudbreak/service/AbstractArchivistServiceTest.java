package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;

@RunWith(MockitoJUnitRunner.class)
public class AbstractArchivistServiceTest {

    private static final long CURRENT_TIME_MILLIS = 123456L;

    @Mock
    private Clock clock;

    @InjectMocks
    private ArchivistServiceImplementation underTest;

    @Before
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

        @Override
        public WorkspaceResource resource() {
            return WorkspaceResource.ALL;
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

        @Override
        public WorkspaceResource getResource() {
            return null;
        }

        public long getDeletionTimestamp() {
            return deletionTimestamp;
        }

        public boolean isArchived() {
            return archived;
        }
    }
}
