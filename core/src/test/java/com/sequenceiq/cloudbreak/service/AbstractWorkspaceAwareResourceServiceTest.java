package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;

@RunWith(MockitoJUnitRunner.class)
public class AbstractWorkspaceAwareResourceServiceTest {

    @Mock
    private AbstractWorkspaceAwareResourceService<TestWorkspaceAwareResource> underTest;

    @Mock
    private Workspace workspace;

    @Mock
    private WorkspaceResourceRepository<TestWorkspaceAwareResource, Long> testRepository;

    @Before
    public void setup() {
        // The mock service is the object under test. Mock methods of the abstract
        // class that are not under test here.
        when(underTest.repository()).thenReturn(testRepository);
        when(underTest.resource()).thenReturn(WorkspaceResource.ALL);

        // Have the mock call the real methods in the abstract class that are
        // under test.
        when(underTest.delete(any(TestWorkspaceAwareResource.class))).thenCallRealMethod();
        when(underTest.delete(any(Set.class))).thenCallRealMethod();
        when(underTest.deleteMultipleByNameFromWorkspace(any(Set.class), any(Long.class))).thenCallRealMethod();
        when(underTest.getByNamesForWorkspaceId(any(Set.class), any(Long.class))).thenCallRealMethod();
    }

    @Test
    public void testDelete() {
        TestWorkspaceAwareResource r = new TestWorkspaceAwareResource(1L, workspace, "name1");

        underTest.delete(r);

        verify(underTest).prepareDeletion(r);
        verify(underTest.repository()).delete(r);
    }

    @Test
    public void testMultiDelete() {
        TestWorkspaceAwareResource r1 = new TestWorkspaceAwareResource(1L, workspace, "name1");
        TestWorkspaceAwareResource r2 = new TestWorkspaceAwareResource(2L, workspace, "name2");

        underTest.delete(ImmutableSet.of(r1, r2));

        verify(underTest).prepareDeletion(r1);
        verify(underTest).prepareDeletion(r2);
        verify(underTest.repository()).delete(r1);
        verify(underTest.repository()).delete(r2);
    }

    @Test
    public void testMultiDeleteNotFound() {
        try {
            TestWorkspaceAwareResource r1 = new TestWorkspaceAwareResource(1L, workspace, "name2");
            Set<String> names = ImmutableSet.of("badname1", "name2", "badname3");

            when(testRepository.findByNameInAndWorkspaceId(names, 1L))
                    .thenReturn(ImmutableSet.of(r1));
            underTest.deleteMultipleByNameFromWorkspace(names, 1L);
            fail("Expected a NotFoundException");
        } catch (NotFoundException e) {
            assertTrue(e.getMessage().contains("badname1") && e.getMessage().contains("badname3")
                    && !e.getMessage().contains("name2"));
        }
    }

    private static class TestWorkspaceAwareResource implements WorkspaceAwareResource {

        private Long id;

        private Workspace workspace;

        private String name;

        private TestWorkspaceAwareResource(Long id, Workspace workspace, String name) {
            this.id = id;
            this.workspace = workspace;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public Workspace getWorkspace() {
            return workspace;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setWorkspace(Workspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public WorkspaceResource getResource() {
            return WorkspaceResource.ALL;
        }

    }

}
