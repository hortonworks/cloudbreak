package com.sequenceiq.cloudbreak.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@RunWith(MockitoJUnitRunner.class)
public class AbstractWorkspaceAwareResourceServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock(answer = Answers.CALLS_REAL_METHODS)
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
        TestWorkspaceAwareResource r1 = new TestWorkspaceAwareResource(1L, workspace, "name2");
        Set<String> names = ImmutableSet.of("badname1", "name2", "badname3");

        when(testRepository.findByNameInAndWorkspaceId(names, 1L))
                .thenReturn(ImmutableSet.of(r1));

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("No resource(s) found with name(s) ''badname1', 'badname3''");

        underTest.deleteMultipleByNameFromWorkspace(names, 1L);
    }

    @Test
    public void testNotFoundErrorMessage() {
        when(testRepository.findByNameAndWorkspaceId(anyString(), any())).thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("No resource found with name 'badname1'");

        underTest.deleteByNameFromWorkspace("badname1", 1L);
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

    }

}
