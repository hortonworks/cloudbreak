package com.sequenceiq.cloudbreak.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.repository.security.UserRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.security.OwnerBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@SpringBootTest(classes = CredentialServiceSecurityComponentTest.TestConfig.class)
public class CredentialServiceSecurityComponentTest extends SecurityComponentTestBase {

    @Inject
    private CredentialService underTest;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @SpyBean
    private OwnerBasedPermissionEvaluator ownerBasedPermissionEvaluator;

    @Inject
    private HasPermissionAspectForMockitoTest hasPermissionAspectForMockitoTest;

    @Inject
    private HasPermissionServiceForMockitoTest hasPermissionServiceForMockitoTest;

    @Inject
    private StackService stackService;

    @Test
    public void testRetrievePrivateCredential() throws Exception {
        Set<Credential> credentials = new HashSet<>(Collections.singleton(getACredential()));
        when(credentialRepository.findForUser(anyString())).thenReturn(credentials);
        IdentityUser loggedInUser = getOwner(false);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrievePrivateCredentials(loggedInUser);
        }

        verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credentials), eq(PERMISSION_READ));
    }

    @Test
    public void testRetrieveAccountCredentialsForAdmin() throws Exception {
        Set<Credential> credentials = new HashSet<>(Arrays.asList(getACredential(), getACredential(USER_B_ID, false)));
        when(credentialRepository.findAllInAccountAndFilterByPlatforms(anyString(), any())).thenReturn(credentials);
        IdentityUser loggedInUser = getOwner(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrieveAccountCredentials(loggedInUser);
        }

        verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credentials), eq(PERMISSION_READ));
    }

    @Test
    public void testRetrieveAccountCredentialsForUser() throws Exception {
        Set<Credential> credentials = new HashSet<>(Arrays.asList(getACredential(), getACredential(USER_B_ID, true)));
        when(credentialRepository.findPublicInAccountForUserFilterByPlatforms(anyString(), anyString(), any())).thenReturn(credentials);
        IdentityUser loggedInUser = getOwner(false);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrieveAccountCredentials(loggedInUser);
        }

        verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credentials), eq(PERMISSION_READ));
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findById(anyLong())).thenReturn(Optional.of(credential));
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get(1L);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(Optional.of(credential)), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findById(anyLong())).thenReturn(Optional.empty());
        setupLoggedInUser(getAUser());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get(1L);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(Optional.empty()), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdAndAccountNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findByIdInAccount(anyLong(), anyString())).thenReturn(credential);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get(1L, "account");

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdAndAccountThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findByIdInAccount(anyLong(), anyString())).thenReturn(null);
        setupLoggedInUser(getAUser());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get(1L, "account");

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByNameAndAccountNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findOneByName(anyString(), anyString())).thenReturn(credential);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get("name", "account");

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByNameAndAccountThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findOneByName(anyString(), anyString())).thenReturn(null);
        setupLoggedInUser(getAUser());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get("name", "account");

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test
    public void testCreateForUser() throws Exception {
        Credential credential = getACredential();
        when(credentialAdapter.init(credential)).thenReturn(credential);
        IdentityUser loggedInUser = getAUser();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.create(loggedInUser, credential);
        }

        verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_WRITE));
    }

    @Test
    public void testCreateForUserNameAndAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialAdapter.init(credential)).thenReturn(credential);
        IdentityUser loggedInUser = getAUser();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.create(loggedInUser.getUserId(), loggedInUser.getAccount(), credential);
        }

        verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_WRITE));
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPublicCredentialNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findOneByName(anyString(), anyString())).thenReturn(credential);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPublicCredential("name", loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPublicCredentialThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findOneByName(anyString(), anyString())).thenReturn(null);
        IdentityUser loggedInUser = getAUser();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPublicCredential("name", loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPrivateCredentialNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findByNameInUser(anyString(), anyString())).thenReturn(credential);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPrivateCredential("name", loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPrivateCredentialThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findByNameInUser(anyString(), anyString())).thenReturn(null);
        IdentityUser loggedInUser = getAUser();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPrivateCredential("name", loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testModifyWithPrivateCredentialNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findByNameInUser(anyString(), anyString())).thenReturn(credential);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.modify(loggedInUser, credential);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testModifyWithPublicCredentialNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential(true);
        when(credentialRepository.findOneByName(anyString(), anyString())).thenReturn(credential);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.modify(loggedInUser, credential);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteByIdNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findByIdInAccount(anyLong(), anyString())).thenReturn(credential);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.delete(1L, loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            verify(stackService, never()).countByCredential(credential);
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteByIdThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findByIdInAccount(anyLong(), anyString())).thenReturn(null);
        IdentityUser loggedInUser = getAUser();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.delete(1L, loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteByNameNotAccessibleForUserOfDifferentAccount() throws Exception {
        Credential credential = getACredential();
        when(credentialRepository.findByNameInAccount(anyString(), anyString(), anyString())).thenReturn(credential);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.delete("credentialName", loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(credential), eq(PERMISSION_READ));
            verify(stackService, never()).countByCredential(credential);
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteByNameThrowsWhenNotFound() throws Exception {
        when(credentialRepository.findByNameInAccount(anyString(), anyString(), anyString())).thenReturn(null);
        IdentityUser loggedInUser = getAUser();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.delete("credentialName", loggedInUser);

        } catch (AccessDeniedException e) {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
            throw e;
        }
    }

    private Credential getACredential() {
        return getACredential(USER_A_ID, false);
    }

    private Credential getACredential(boolean publicInAccount) {
        return getACredential(USER_A_ID, publicInAccount);
    }

    private Credential getACredential(String owner, boolean publicInAccount) {
        Credential credential = new Credential();
        credential.setName("credentialName");
        credential.setAccount(ACCOUNT_A);
        credential.setOwner(owner);
        credential.setPublicInAccount(publicInAccount);
        return credential;
    }

    @Configuration
    @ComponentScan(basePackages =
            {"com.sequenceiq.cloudbreak"},
            useDefaultFilters = false,
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                            CredentialService.class,
                            UserProfileHandler.class,
                            UserProfileService.class,
                    })
            })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public static class TestConfig extends SecurityComponentTestBase.SecurityComponentTestBaseConfig {
        @MockBean
        private UserProfileRepository userProfileRepository;

        @MockBean
        private StackService stackService;

        @MockBean
        private StackRepository stackRepository;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private ServiceProviderCredentialAdapter serviceProviderCredentialAdapter;

        @MockBean
        private AccountPreferencesService accountPreferencesService;

        @MockBean
        private NotificationSender notificationSender;

        @MockBean
        private CloudbreakMessagesService messagesService;

        @MockBean
        private OrganizationService organizationService;

        @MockBean
        private UserService userService;

        @MockBean
        private AuthenticatedUserService authenticatedUserService;

        @MockBean
        private ImageCatalogService imageCatalogService;

        @Bean
        public CredentialRepository credentialRepository() {
            return mock(CredentialRepository.class);
        }
    }

}
