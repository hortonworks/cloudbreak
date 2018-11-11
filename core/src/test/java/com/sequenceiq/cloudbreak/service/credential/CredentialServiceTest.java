package com.sequenceiq.cloudbreak.service.credential;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentViewRepository;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    private static final String PLATFORM = "OPENSTACK";

    private static final String TEST_CREDENTIAL_NAME = "testCredentialName";

    private static final Long TEST_CREDENTIAL_ID = 2L;

    private static final String TEST_WORKSPACE_NAME = "test@org.name";

    private static final Long ORG_ID = 1L;

    private static final Set<String> CLOUD_PLATFORMS = Set.of("YARN, AWS, AZURE, GCP, OPENSTACK");

    private static final String USER_ID = "some@user.id";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Mock
    private UserProfileHandler userProfileHandler;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private AccountPreferencesService accountPreferencesService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Workspace workspace;

    @Mock
    private User user;

    @Mock
    private CredentialValidator credentialValidator;

    @Mock
    private EnvironmentViewRepository environmentViewRepository;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private final CredentialService credentialService = new CredentialService();

    private Credential testCredential;

    @Before
    public void init() {
        testCredential = mock(Credential.class);
        when(testCredential.getName()).thenReturn(TEST_CREDENTIAL_NAME);
        when(accountPreferencesService.enabledPlatforms()).thenReturn(CLOUD_PLATFORMS);
        when(testCredential.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(ORG_ID);
        when(workspace.getName()).thenReturn(TEST_WORKSPACE_NAME);
        when(user.getUserId()).thenReturn(USER_ID);
    }

    @Test
    public void testGetWhenCredentialDoesNotExistsWithIdThenNotFoundExceptionShouldCome() {
        when(credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, ORG_ID, CLOUD_PLATFORMS)).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("Credential with id: '%d' not found", TEST_CREDENTIAL_ID));

        credentialService.get(TEST_CREDENTIAL_ID, workspace);
        verify(accountPreferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, ORG_ID, CLOUD_PLATFORMS);
    }

    @Test
    public void testGetWhenCredentialExistsAndObtainableThenItWillBeReturned() {
        Credential expected = new Credential();
        when(credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, ORG_ID, CLOUD_PLATFORMS)).thenReturn(expected);

        Credential result = credentialService.get(TEST_CREDENTIAL_ID, workspace);

        assertEquals(expected, result);
        verify(accountPreferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, ORG_ID, CLOUD_PLATFORMS);
    }

    @Test
    public void testInteractiveLogin() {
        credentialService.interactiveLogin(ORG_ID, testCredential, workspace, user);

        verify(credentialAdapter, times(1)).interactiveLogin(testCredential, ORG_ID, USER_ID);
    }

    @Test
    public void testUpdateByWorkspaceIdWhenCredentialCloudPlatformIsNotValidThenNoUnnecessaryCallsAreHappen() {
        String message = "Invalid credential";
        when(testCredential.cloudPlatform()).thenReturn("");
        doThrow(new BadRequestException(message)).when(credentialValidator).validateCredentialCloudPlatform("");

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(message);

        credentialService.updateByWorkspaceId(ORG_ID, testCredential, user);

        verify(credentialRepository, times(0)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(accountPreferencesService, times(0)).enabledPlatforms();
        verify(workspaceService, times(0)).get(anyLong(), any(User.class));
        verify(credentialAdapter, times(0)).init(any(Credential.class), anyLong(), anyString());
        verify(credentialRepository, times(0)).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdWhenCredentialDoesNotExistsOrItIsInAnotherWorkspaceWhereUserHasNoRightThenNotFoundExceptionShouldCome() {
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("Credential with name: '%s' not found", TEST_CREDENTIAL_NAME));

        credentialService.updateByWorkspaceId(ORG_ID, testCredential, user);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS);
        verify(accountPreferencesService, times(1)).enabledPlatforms();
        verify(workspaceService, times(0)).get(anyLong(), any(User.class));
        verify(credentialAdapter, times(0)).init(any(Credential.class), anyLong(), anyString());
        verify(credentialRepository, times(0)).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdWhenOriginalCredentialHasCloudPlatformAndItDoesNotEqualsToTheDesiredOneThenBadRequestExceptionShouldCome() {
        Credential original = mock(Credential.class);
        when(original.cloudPlatform()).thenReturn("AWS");
        when(testCredential.cloudPlatform()).thenReturn("GCP");
        doNothing().when(credentialValidator).validateCredentialCloudPlatform(anyString());
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(original);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Modifying credential platform is forbidden");

        credentialService.updateByWorkspaceId(ORG_ID, testCredential, user);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS);
        verify(accountPreferencesService, times(1)).enabledPlatforms();
        verify(workspaceService, times(0)).get(anyLong(), any(User.class));
        verify(credentialAdapter, times(0)).init(any(Credential.class), anyLong(), anyString());
        verify(credentialRepository, times(0)).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdWhenEveryConditionMeetsThenCredentialSaveHappens() {
        Credential saved = mock(Credential.class);
        Credential original = mock(Credential.class);
        when(original.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(testCredential.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        doNothing().when(credentialValidator).validateCredentialCloudPlatform(anyString());
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(original);
        when(credentialAdapter.init(testCredential, ORG_ID, USER_ID)).thenReturn(testCredential);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(credentialRepository.save(any())).thenReturn(saved);

        Credential result = credentialService.updateByWorkspaceId(ORG_ID, testCredential, user);

        assertEquals(saved, result);
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS);
        verify(accountPreferencesService, times(1)).enabledPlatforms();
        verify(workspaceService, times(2)).get(anyLong(), any(User.class));
        verify(workspaceService, times(2)).get(ORG_ID, user);
        verify(credentialAdapter, times(1)).init(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).init(testCredential, ORG_ID, USER_ID);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_MODIFIED.getMessage());
    }

    @Test
    public void testCreateWhenCredentialCloudPlatformIsNotValidThenSaveShouldNotBePerformed() {
        String invalidCloudPlatformValue = "something invalid";
        String message = "Invalid credential";
        when(testCredential.cloudPlatform()).thenReturn(invalidCloudPlatformValue);
        when(workspaceService.get(ORG_ID, user)).thenReturn(workspace);
        doThrow(new BadRequestException(message)).when(credentialValidator).validateCredentialCloudPlatform(invalidCloudPlatformValue);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(message);

        credentialService.create(testCredential, ORG_ID, user);

        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(anyString());
        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(invalidCloudPlatformValue);
        verify(credentialAdapter, times(0)).init(any(Credential.class), anyLong(), anyString());
        verify(notificationSender, times(0)).send(any());
        verify(messagesService, times(0)).getMessage(anyString());
    }

    @Test
    public void testCreateWhenCredentialCloudPlatformIsValidAndCredentialValuesAreFineThenCredentialWillBeSaved() {
        Credential expected = new Credential();
        when(testCredential.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(credentialAdapter.init(testCredential, ORG_ID, USER_ID)).thenReturn(testCredential);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(credentialRepository.save(any())).thenReturn(expected);

        Credential result = credentialService.create(testCredential, ORG_ID, user);

        assertEquals(expected, result);

        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(anyString());
        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(TEST_CLOUD_PLATFORM);
        verify(credentialAdapter, times(1)).init(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).init(testCredential, ORG_ID, USER_ID);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_CREATED.getMessage());
    }

    @Test
    public void testCreateWithRetryWhenCredentialCloudPlatformIsNotValidThenSaveShouldNotBePerformed() {
        String invalidCloudPlatformValue = "something invalid";
        String message = "Invalid credential";
        when(testCredential.cloudPlatform()).thenReturn(invalidCloudPlatformValue);
        when(workspaceService.get(ORG_ID, user)).thenReturn(workspace);
        doThrow(new BadRequestException(message)).when(credentialValidator).validateCredentialCloudPlatform(invalidCloudPlatformValue);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(message);

        credentialService.createWithRetry(testCredential, ORG_ID, user);

        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(anyString());
        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(invalidCloudPlatformValue);
        verify(credentialAdapter, times(0)).init(any(Credential.class), anyLong(), anyString());
        verify(notificationSender, times(0)).send(any());
        verify(messagesService, times(0)).getMessage(anyString());
    }

    @Test
    public void testCreateWithRetryWhenCredentialCloudPlatformIsValidAndCredentialValuesAreFineThenCredentialWillBeSaved() {
        Credential expected = new Credential();
        when(testCredential.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(credentialAdapter.init(testCredential, ORG_ID, USER_ID)).thenReturn(testCredential);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(credentialRepository.save(any())).thenReturn(expected);

        credentialService.createWithRetry(testCredential, ORG_ID, user);

        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(anyString());
        verify(credentialValidator, times(1)).validateCredentialCloudPlatform(TEST_CLOUD_PLATFORM);
        verify(credentialAdapter, times(1)).init(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).init(testCredential, ORG_ID, USER_ID);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_CREATED.getMessage());
    }

    @Test
    public void testDeleteByNameWhenOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stackName = "testStackName";
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(stackName);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(testCredential);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack));

        try {
            credentialService.delete(TEST_CREDENTIAL_NAME, workspace);
        } catch (BadRequestException ex) {
            String msg = String.format(String.format("There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                    + "The following cluster is using this credential: [%s]", TEST_CREDENTIAL_NAME, stackName));
            assertEquals(msg, ex.getMessage());
        }

        verify(stackRepository, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteByNameWhenMoreThaneOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stack1Name = "testStack1Name";
        String stack2Name = "testStack1Name";
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn(stack1Name);
        when(stack2.getName()).thenReturn(stack2Name);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(testCredential);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack1, stack2));

        try {
            credentialService.delete(TEST_CREDENTIAL_NAME, workspace);
        } catch (BadRequestException ex) {
            String msg = String.format("There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                    + "The following clusters are using this credential: [%s]", TEST_CREDENTIAL_NAME, String.format("%s, %s", stack1Name, stack2Name));
            assertEquals(msg, ex.getMessage());
        }

        verify(stackRepository, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteByNameWhenEnvironmentsUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        // GIVEN
        Credential credential = new Credential();
        credential.setName(TEST_CREDENTIAL_NAME);
        credential.setArchived(false);
        credential.setTopology(new Topology());
        credential.setWorkspace(workspace);
        EnvironmentView env1 = new EnvironmentView();
        env1.setId(TestUtil.generateUniqueId());
        env1.setName("env1");
        EnvironmentView env2 = new EnvironmentView();
        env2.setId(TestUtil.generateUniqueId());
        env2.setName("env2");
        Set<EnvironmentView> envs = new HashSet<>();
        envs.add(env1);
        envs.add(env2);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(credential);
        when(stackRepository.findByCredential(credential)).thenReturn(Collections.emptySet());
        when(environmentViewRepository.findAllByCredentialId(credential.getId())).thenReturn(envs);
        // WHEN
        try {
            credentialService.delete(TEST_CREDENTIAL_NAME, workspace);
        } catch (BadRequestException ex) {
            String msg = String.format("Credential '%s' cannot be deleted because the following environments are using it: [%s].",
                TEST_CREDENTIAL_NAME, envs.stream().map(EnvironmentView::getName).collect(Collectors.joining(", ")));
            assertEquals(msg, ex.getMessage());
        }
        // THEN
        verify(stackRepository, times(1)).findByCredential(credential);
        verify(environmentViewRepository, times(1)).findAllByCredentialId(credential.getId());
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(credential);
        verify(credentialRepository, times(0)).save(credential);
    }

    @Test
    public void testDeleteByNameWhenCredentialIsDeletableThenItWillBeArchivedProperly() {
        Credential credential = new Credential();
        credential.setName(TEST_CREDENTIAL_NAME);
        credential.setArchived(false);
        credential.setTopology(new Topology());
        credential.setWorkspace(workspace);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, ORG_ID, CLOUD_PLATFORMS)).thenReturn(credential);
        when(stackRepository.findByCredential(credential)).thenReturn(Collections.emptySet());
        when(environmentViewRepository.findAllByCredentialId(credential.getId())).thenReturn(Collections.emptySet());

        credentialService.delete(TEST_CREDENTIAL_NAME, workspace);

        verify(stackRepository, times(1)).findByCredential(credential);
        verify(userProfileHandler, times(1)).destroyProfileCredentialPreparation(credential);
        verify(credentialRepository, times(1)).save(credential);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_DELETED.getMessage());
    }

}