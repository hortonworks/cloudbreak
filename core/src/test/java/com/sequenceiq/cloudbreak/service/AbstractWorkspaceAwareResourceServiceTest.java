package com.sequenceiq.cloudbreak.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@ExtendWith(MockitoExtension.class)
class AbstractWorkspaceAwareResourceServiceTest {

    private static final long WORKSPACE_ID = 0L;

    @Spy
    @InjectMocks
    private TestResourceService underTest;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceResourceRepository<TestWorkspaceAwareResource, Long> testRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Workspace workspace;

    @Mock
    private User user;

    @Mock
    private TestWorkspaceAwareResource resource;

    @BeforeEach
    void setup() throws Exception {
        // The mock service is the object under test. Mock methods of the abstract
        // class that are not under test here.
        lenient().when(underTest.repository()).thenReturn(testRepository);

        lenient().when(workspaceService.get(any(), any())).thenReturn(workspace);
        lenient().when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        lenient().when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(mock());
        lenient().when(userService.getOrCreate(any())).thenReturn(user);
        lenient().when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

        lenient().when(resource.getWorkspace()).thenReturn(workspace);
        lenient().when(resource.getName()).thenReturn("name");
        lenient().when(resource.getResourceName()).thenReturn("resourcename");
        lenient().when(workspace.getName()).thenReturn("workspacename");
    }

    @Test
    void createForLoggedInUserSuccess() {
        underTest.createForLoggedInUser(resource, WORKSPACE_ID);

        verify(underTest).create(resource, WORKSPACE_ID, user);
        verifyNoInteractions(transactionService);
    }

    @Test
    void createForLoggedInUserAlreadyExists() {
        DataIntegrityViolationException duplicate = new DataIntegrityViolationException("duplicate");
        when(testRepository.save(any())).thenThrow(duplicate);

        assertThatThrownBy(() -> underTest.createForLoggedInUser(resource, WORKSPACE_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("resourcename already exists with name 'name' in workspace workspacename");
    }

    @Test
    void createForLoggedInUserInTransactionSuccess() throws Exception {
        underTest.createForLoggedInUserInTransaction(resource, WORKSPACE_ID);

        verify(underTest).createForLoggedInUser(resource, WORKSPACE_ID);
        verify(transactionService).required(any(Supplier.class));
    }

    @Test
    void createForLoggedInUserInTransactionBadRequest() throws Exception {
        BadRequestException cause = new BadRequestException("badrequest");
        Exception executionException = new TransactionService.TransactionExecutionException("", cause);
        when(transactionService.required(any(Supplier.class))).thenThrow(executionException);

        assertThatThrownBy(() -> underTest.createForLoggedInUserInTransaction(resource, WORKSPACE_ID))
                .isEqualTo(cause);
    }

    @Test
    void createForLoggedInUserInTransactionAlreadyExists() throws Exception {
        Exception executionException = new TransactionService.TransactionExecutionException("", new DataIntegrityViolationException("duplicate"));
        when(transactionService.required(any(Supplier.class))).thenThrow(executionException);

        assertThatThrownBy(() -> underTest.createForLoggedInUserInTransaction(resource, WORKSPACE_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("resourcename already exists with name 'name' in workspace workspacename");
    }

    @Test
    void testDelete() {
        TestWorkspaceAwareResource r = new TestWorkspaceAwareResource(1L, workspace, "name1");

        underTest.delete(r);

        verify(underTest).prepareDeletion(r);
        verify(underTest.repository()).delete(r);
    }

    @Test
    void testMultiDelete() {
        TestWorkspaceAwareResource r1 = new TestWorkspaceAwareResource(1L, workspace, "name1");
        TestWorkspaceAwareResource r2 = new TestWorkspaceAwareResource(2L, workspace, "name2");

        underTest.delete(ImmutableSet.of(r1, r2));

        verify(underTest).prepareDeletion(r1);
        verify(underTest).prepareDeletion(r2);
        verify(underTest.repository()).delete(r1);
        verify(underTest.repository()).delete(r2);
    }

    @Test
    void testMultiDeleteNotFound() {
        TestWorkspaceAwareResource r1 = new TestWorkspaceAwareResource(1L, workspace, "name2");
        Set<String> names = ImmutableSet.of("badname1", "name2", "badname3");

        when(testRepository.findByNameInAndWorkspaceId(names, 1L))
                .thenReturn(ImmutableSet.of(r1));

        assertThatThrownBy(() -> underTest.deleteMultipleByNameFromWorkspace(names, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No resource(s) found with name(s) ''badname1', 'badname3''");

    }

    @Test
    void testNotFoundErrorMessage() {
        when(testRepository.findByNameAndWorkspaceId(anyString(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.deleteByNameFromWorkspace("badname1", 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No resource found with name 'badname1'");
    }

    private static class TestResourceService extends AbstractWorkspaceAwareResourceService<TestWorkspaceAwareResource> {

        @Inject
        private WorkspaceResourceRepository<TestWorkspaceAwareResource, Long> repository;

        @Override
        protected WorkspaceResourceRepository<TestWorkspaceAwareResource, Long> repository() {
            return repository;
        }

        @Override
        protected void prepareDeletion(TestWorkspaceAwareResource resource) {

        }

        @Override
        protected void prepareCreation(TestWorkspaceAwareResource resource) {

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

    }

}
