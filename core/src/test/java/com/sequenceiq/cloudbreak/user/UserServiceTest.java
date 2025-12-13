package com.sequenceiq.cloudbreak.user;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.tenant.TenantService;
import com.sequenceiq.cloudbreak.service.user.CachedUserService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserRepository userRepository;

    @Spy
    private CachedUserService cachedUserService = new CachedUserService();

    @Spy
    private TransactionService transactionService = new TransactionService();

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private UserService underTest;

    @Test
    void testCreateUser() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).requiresNew(any(Supplier.class));
        when(userRepository.findByTenantNameAndUserId(anyString(), anyString())).thenReturn(Optional.empty());
        when(tenantService.findByName(anyString())).thenReturn(Optional.empty());
        when(tenantService.save(any())).thenReturn(createTenant());
        when(workspaceService.create(any())).thenReturn(createWorkspace());
        when(userRepository.save(any())).thenReturn(createUser());

        User user = underTest.getOrCreate(createCbUser());

        assertNotNull(user);
        verify(cachedUserService, times(1)).getUser(any(), any(), any());
        verify(workspaceService, times(1)).create(any());
        verify(tenantService, times(1)).save(any());
    }

    @Test
    void testCreateUserWithDuplicateException() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).requiresNew(any(Supplier.class));
        when(tenantService.findByName(anyString())).thenReturn(Optional.empty());
        when(tenantService.save(any())).thenReturn(createTenant());
        when(workspaceService.create(any())).thenReturn(createWorkspace());

        when(userRepository.findByTenantNameAndUserId(anyString(), anyString())).thenAnswer(new Answer() {
            private int count;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 0) {
                    return Optional.empty();
                }
                return Optional.of(createUser());
            }
        });

        when(userRepository.save(any())).thenAnswer(new Answer() {
            private int count;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 0) {
                    TransactionExecutionException exception = new TransactionExecutionException("", new RuntimeException("User already exists!"));
                    throw new TransactionRuntimeExecutionException(exception);
                }
                TransactionExecutionException exception = new TransactionExecutionException("", new RuntimeException("This is not expected to run"));
                throw new TransactionRuntimeExecutionException(exception);
            }
        });


        User user = underTest.getOrCreate(createCbUser());

        assertNotNull(user);
        verify(cachedUserService, times(1)).getUser(any(), any(), any());
        verify(workspaceService, times(1)).create(any());
        verify(tenantService, times(1)).save(any());
    }

    @Test
    void testPersistModifiedInternalUser() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).requiresNew(any(Supplier.class));
        when(userRepository.findByTenantNameAndUserId(anyString(), anyString())).thenReturn(Optional.empty());
        when(tenantService.findByName(anyString())).thenReturn(Optional.empty());
        when(tenantService.save(any())).thenReturn(createTenant());
        when(workspaceService.create(any())).thenReturn(createWorkspace());
        when(userRepository.save(any())).thenReturn(createUser());
        doNothing().when(restRequestThreadLocalService).setCloudbreakUser(any());

        CrnUser cbUser = createCrnUser();
        underTest.persistModifiedInternalUser(cbUser);

        assertNotNull(cbUser);
        verify(cachedUserService, times(1)).getUser(any(), any(), any());
        verify(workspaceService, times(1)).create(any());
        verify(tenantService, times(1)).save(any());
        verify(restRequestThreadLocalService, times(1)).setCloudbreakUser(eq(cbUser));
    }

    private User createUser() {
        User user = new User();
        user.setTenant(createTenant());
        user.setUserId("userId");
        user.setUserName("userName");
        user.setUserCrn("crn:cdp:iam:us-west-1:tenantName:user:userName");
        return user;
    }

    private Workspace createWorkspace() {
        return new Workspace();
    }

    private Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("tenantName");
        tenant.setWorkspaces(Sets.newHashSet(createWorkspace()));
        return tenant;
    }

    private CloudbreakUser createCbUser() {
        return new CloudbreakUser("userId", "crn:cdp:iam:us-west-1:tenantName:user:userName",
                "userName", "email@email.com", "tenantName");
    }

    private CrnUser createCrnUser() {
        return new CrnUser("userId", "crn:cdp:iam:us-west-1:tenantName:user:userName",
                "userName", "email@email.com", "tenantName", "role");
    }

}
