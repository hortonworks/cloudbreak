package com.sequenceiq.cloudbreak.user;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.tenant.TenantService;
import com.sequenceiq.cloudbreak.service.user.CachedUserService;
import com.sequenceiq.cloudbreak.service.user.UserPreferencesService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.UserPreferences;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.UserRepository;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesService userPreferencesService;

    @Spy
    private CachedUserService cachedUserService = new CachedUserService();

    @Spy
    private TransactionService transactionService = new TransactionService();

    @InjectMocks
    private UserService underTest;

    @Test
    public void testCreateUser() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).requiresNew(any());
        when(userRepository.findByTenantNameAndUserId(anyString(), anyString())).thenReturn(Optional.empty());
        when(tenantService.findByName(anyString())).thenReturn(Optional.empty());
        when(tenantService.save(any())).thenReturn(createTenant());
        when(workspaceService.create(any())).thenReturn(createWorkspace());
        when(userPreferencesService.save(any())).thenReturn(createUserPref());
        when(userRepository.save(any())).thenReturn(createUser());

        User user = underTest.getOrCreate(createCbUser());

        assertNotNull(user);
        verify(cachedUserService, times(1)).getUser(any(), any(), any());
        verify(workspaceService, times(1)).create(any());
        verify(tenantService, times(1)).save(any());
    }

    @Test
    public void testCreateUserWithDuplicateException() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).requiresNew(any());
        when(tenantService.findByName(anyString())).thenReturn(Optional.empty());
        when(tenantService.save(any())).thenReturn(createTenant());
        when(workspaceService.create(any())).thenReturn(createWorkspace());
        when(userPreferencesService.save(any())).thenReturn(createUserPref());

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

    private User createUser() {
        User user = new User();
        user.setTenant(createTenant());
        user.setUserId("userId");
        user.setUserName("userName");
        user.setUserCrn("crn:cdp:iam:us-west-1:tenantName:user:userName");
        user.setUserPreferences(createUserPref());
        return user;
    }

    private UserPreferences createUserPref() {
        return new UserPreferences();
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

}
