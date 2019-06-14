package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.service.credential.CredentialService.DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    private static final String PLATFORM = "AWS";

    private static final String TEST_CREDENTIAL_NAME = "testCredentialName";

    private static final Long TEST_CREDENTIAL_ID = 2L;

    private static final String TEST_WORKSPACE_NAME = "test@org.name";

    private static final Long WORKSPACE_ID = 1L;

    private static final Set<String> CLOUD_PLATFORMS = Set.of("YARN, AWS, AZURE, GCP, OPENSTACK");

    private static final String USER_ID = "some@user.id";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    private static final String AUTHORIZE_GRANT_FLOW_CODE = "testcode";

    private static final String AUTHORIZE_GRANT_FLOW_STATE = "someState";

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
    private PreferencesService preferencesService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StackService stackService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Workspace workspace;

    @Mock
    private User user;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Mock
    private SecretService secretService;

    @Mock
    private UserService userService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private final CredentialService underTest = new CredentialService();

    private Credential testCredential;

    @Before
    public void init() {
        testCredential = mock(Credential.class);
        when(testCredential.getName()).thenReturn(TEST_CREDENTIAL_NAME);
        when(preferencesService.enabledPlatforms()).thenReturn(CLOUD_PLATFORMS);
        when(testCredential.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(workspace.getName()).thenReturn(TEST_WORKSPACE_NAME);
        when(user.getUserId()).thenReturn(USER_ID);
        when(userService.getOrCreate(any())).thenReturn(user);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
    }

    @Test
    public void testGetWhenCredentialDoesNotExistsWithIdThenNotFoundExceptionShouldCome() {
        when(credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, WORKSPACE_ID, CLOUD_PLATFORMS)).thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(format("Credential with id: '%d' not found", TEST_CREDENTIAL_ID));

        underTest.get(TEST_CREDENTIAL_ID, workspace);
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, WORKSPACE_ID, CLOUD_PLATFORMS);
    }

    @Test
    public void testGetWhenCredentialExistsAndObtainableThenItWillBeReturned() {
        Credential expected = createCredential(TEST_CREDENTIAL_NAME);
        when(credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(expected));

        Credential result = underTest.get(TEST_CREDENTIAL_ID, workspace);

        assertEquals(expected, result);
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByIdAndWorkspaceFilterByPlatforms(TEST_CREDENTIAL_ID, WORKSPACE_ID, CLOUD_PLATFORMS);
    }

    @Test
    public void testInteractiveLogin() {
        String key = "deploymentAddress";
        String expected = "https://192.168.909.100";
        when(testCredential.getAttributes()).thenReturn(format("{\"%s\":\"%s\"}", key, expected));

        underTest.interactiveLogin(WORKSPACE_ID, testCredential);

        verify(credentialAdapter, times(1)).interactiveLogin(testCredential, WORKSPACE_ID, USER_ID);
    }

    @Test
    public void testUpdateByWorkspaceIdWhenCredentialCloudPlatformIsNotValidThenNoUnnecessaryCallsAreHappen() {
        String message = "Invalid credential";
        when(testCredential.cloudPlatform()).thenReturn("");

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(message);

        underTest.updateByWorkspaceId(WORKSPACE_ID, testCredential);

        verify(credentialRepository, times(0)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(preferencesService, times(0)).enabledPlatforms();
        verify(workspaceService, times(0)).get(anyLong(), any(User.class));
        verify(credentialAdapter, times(0)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialRepository, times(0)).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdWhenCredentialDoesNotExistsOrItIsInAnotherWorkspaceWhereUserHasNoRightThenNotFoundExceptionShouldCome() {
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(format("Credential with name: '%s' not found", TEST_CREDENTIAL_NAME));

        underTest.updateByWorkspaceId(WORKSPACE_ID, testCredential);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(workspaceService, times(0)).get(anyLong(), any(User.class));
        verify(credentialAdapter, times(0)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialRepository, times(0)).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdWhenOriginalCredentialHasCloudPlatformAndItDoesNotEqualsToTheDesiredOneThenBadRequestExceptionShouldCome() {
        Credential original = mock(Credential.class);
        when(original.cloudPlatform()).thenReturn("AWS");
        when(testCredential.cloudPlatform()).thenReturn("GCP");
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.of(original));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Modifying credential platform is forbidden");

        underTest.updateByWorkspaceId(WORKSPACE_ID, testCredential);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(workspaceService, times(0)).get(anyLong(), any(User.class));
        verify(credentialAdapter, times(0)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialRepository, times(0)).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdWhenEveryConditionMeetsThenCredentialSaveHappens() {
        Credential saved = mock(Credential.class);
        Credential original = mock(Credential.class);
        when(original.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(testCredential.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.of(original));
        when(credentialAdapter.verify(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(testCredential);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(credentialRepository.save(any())).thenReturn(saved);

        Credential result = underTest.updateByWorkspaceId(WORKSPACE_ID, testCredential);

        assertEquals(saved, result);
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anyCollection());
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(workspaceService, times(2)).get(anyLong(), any(User.class));
        verify(workspaceService, times(2)).get(WORKSPACE_ID, user);
        verify(credentialAdapter, times(1)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).verify(testCredential, WORKSPACE_ID, USER_ID);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_MODIFIED.getMessage());
    }

    @Test
    public void testCreateWhenCredentialCloudPlatformIsNotValidThenSaveShouldNotBePerformed() {
        String invalidCloudPlatformValue = "something invalid";
        String message = "Invalid credential";
        when(testCredential.cloudPlatform()).thenReturn(invalidCloudPlatformValue);
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(message);

        underTest.create(testCredential, WORKSPACE_ID, user);

        verify(credentialAdapter, times(0)).verify(any(Credential.class), anyLong(), anyString());
        verify(notificationSender, times(0)).send(any());
        verify(messagesService, times(0)).getMessage(anyString());
    }

    @Test
    public void testCreateWhenCredentialCloudPlatformIsValidAndCredentialValuesAreFineThenCredentialWillBeSaved() {
        Credential expected = createCredential(TEST_CREDENTIAL_NAME);
        when(testCredential.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(credentialAdapter.verify(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(testCredential);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(credentialRepository.save(any())).thenReturn(expected);

        Credential result = underTest.create(testCredential, WORKSPACE_ID, user);

        assertEquals(expected, result);

        verify(credentialAdapter, times(1)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).verify(testCredential, WORKSPACE_ID, USER_ID);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_CREATED.getMessage());
    }

    @Test
    public void testCreateWithRetryWhenCredentialCloudPlatformIsNotValidThenSaveShouldNotBePerformed() {
        String invalidCloudPlatformValue = "something invalid";
        String message = "Invalid credential";
        when(testCredential.cloudPlatform()).thenReturn(invalidCloudPlatformValue);
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(message);

        underTest.createWithRetry(testCredential, WORKSPACE_ID, user);

        verify(credentialAdapter, times(0)).verify(any(Credential.class), anyLong(), anyString());
        verify(notificationSender, times(0)).send(any());
        verify(messagesService, times(0)).getMessage(anyString());
    }

    @Test
    public void testCreateWithRetryWhenCredentialCloudPlatformIsValidAndCredentialValuesAreFineThenCredentialWillBeSaved() {
        Credential expected = createCredential(TEST_CREDENTIAL_NAME);
        when(testCredential.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(credentialAdapter.verify(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(testCredential);
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(credentialRepository.save(any())).thenReturn(expected);

        underTest.createWithRetry(testCredential, WORKSPACE_ID, user);

        verify(credentialAdapter, times(1)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).verify(testCredential, WORKSPACE_ID, USER_ID);
        verify(notificationSender, times(1)).send(any());
        verify(messagesService, times(1)).getMessage(ResourceEvent.CREDENTIAL_CREATED.getMessage());
    }

    @Test
    public void testDeleteByNameWhenOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stackName = "testStackName";
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(stackName);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(testCredential));
        when(stackService.findByCredential(testCredential)).thenReturn(Set.of(stack));

        try {
            underTest.delete(TEST_CREDENTIAL_NAME, workspace);
        } catch (BadRequestException ex) {
            String msg = format(format("There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                    + "The following cluster is using this credential: [%s]", TEST_CREDENTIAL_NAME, stackName));
            assertEquals(msg, ex.getMessage());
        }

        verify(stackService, times(1)).findByCredential(testCredential);
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
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(testCredential));
        when(stackService.findByCredential(testCredential)).thenReturn(Set.of(stack1, stack2));

        try {
            underTest.delete(TEST_CREDENTIAL_NAME, workspace);
        } catch (BadRequestException ex) {
            String msg = format("There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                    + "The following clusters are using this credential: [%s]", TEST_CREDENTIAL_NAME, format("%s, %s", stack1Name, stack2Name));
            assertEquals(msg, ex.getMessage());
        }

        verify(stackService, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteByNameWhenEnvironmentsUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        // GIVEN
        Credential credential = createCredential(TEST_CREDENTIAL_NAME);
        EnvironmentView env1 = new EnvironmentView();
        env1.setId(TestUtil.generateUniqueId());
        env1.setName("env1");
        EnvironmentView env2 = new EnvironmentView();
        env2.setId(TestUtil.generateUniqueId());
        env2.setName("env2");
        Set<EnvironmentView> envs = new HashSet<>();
        envs.add(env1);
        envs.add(env2);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(credential));
        when(stackService.findByCredential(credential)).thenReturn(Collections.emptySet());
        when(environmentViewService.findAllByCredentialId(credential.getId())).thenReturn(envs);
        // WHEN
        try {
            underTest.delete(TEST_CREDENTIAL_NAME, workspace);
        } catch (BadRequestException ex) {
            String msg = format("Credential '%s' cannot be deleted because the following environments are using it: [%s].",
                    TEST_CREDENTIAL_NAME, envs.stream().map(EnvironmentView::getName).collect(Collectors.joining(", ")));
            assertEquals(msg, ex.getMessage());
        }
        // THEN
        verify(stackService, times(1)).findByCredential(credential);
        verify(environmentViewService, times(1)).findAllByCredentialId(credential.getId());
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(credential);
        verify(credentialRepository, times(0)).save(credential);
    }

    @Test
    public void testDeleteByNameWhenCredentialIsDeletableThenItWillBeArchivedProperly() {
        Credential credential = createCredential(TEST_CREDENTIAL_NAME);
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(credential));
        when(stackService.findByCredential(credential)).thenReturn(Collections.emptySet());
        when(environmentViewService.findAllByCredentialId(credential.getId())).thenReturn(Collections.emptySet());

        underTest.delete(TEST_CREDENTIAL_NAME, workspace);

        verifyCredentialsAreArchived(credential);
    }

    @Test
    public void testMultipleDelete() {
        String credential1Name = TEST_CREDENTIAL_NAME + '1';
        String credential2Name = TEST_CREDENTIAL_NAME + '2';

        Credential credential1 = createCredential(credential1Name);
        Credential credential2 = createCredential(credential2Name);
        when(workspaceService.getByIdForCurrentUser(WORKSPACE_ID)).thenReturn(workspace);
        when(credentialRepository.findByNameInAndWorkspaceId(ImmutableSet.of(credential1Name, credential2Name),
                WORKSPACE_ID)).thenReturn(ImmutableSet.of(credential1, credential2));
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(credential1Name, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.of(credential1));
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(credential2Name, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.of(credential2));

        underTest.deleteMultipleByNameFromWorkspace(ImmutableSet.of(credential1Name, credential2Name), WORKSPACE_ID);

        verifyCredentialsAreArchived(credential1, credential2);
    }

    @Test
    public void testMultipleDeleteWithMissingCredential() {
        String credential1Name = TEST_CREDENTIAL_NAME + '1';
        String credential2Name = TEST_CREDENTIAL_NAME + '2';

        Credential credential1 = createCredential(credential1Name);
        when(credentialRepository.findByNameInAndWorkspaceId(ImmutableSet.of(credential1Name, credential2Name),
                WORKSPACE_ID)).thenReturn(ImmutableSet.of(credential1));

        try {
            underTest.deleteMultipleByNameFromWorkspace(ImmutableSet.of(credential1Name, credential2Name), WORKSPACE_ID);
            fail("Expected a NotFoundException");
        } catch (NotFoundException e) {
            assertTrue(e.getMessage().contains(credential2Name) && !e.getMessage().contains(credential1Name));
        }
    }

    @Test
    public void testGetPrerequisitesBothCredentialValidatorAndCredentialPrerequisiteServiceIsCalled() {
        CredentialPrerequisitesResponse expected = mock(CredentialPrerequisitesResponse.class);
        when(credentialPrerequisiteService.getPrerequisites(user, workspace, PLATFORM, "")).thenReturn(expected);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(workspace.getId(), PLATFORM, "");

        assertEquals("The result CredentialPrerequisites object is not the expected one!", expected, result);
        verify(credentialPrerequisiteService, times(1)).getPrerequisites(user, workspace, PLATFORM, "");
    }

    @Test
    public void testGetPrerequisitesBothCredentialValidatorAndCredentialPrerequisiteServiceIsCalledAndDeploymentAddressIsNotEmpty() {
        CredentialPrerequisitesResponse expected = mock(CredentialPrerequisitesResponse.class);
        String deploymentAddress = "https://MYDEPLOYMENT_ADDRESS";
        when(credentialPrerequisiteService.getPrerequisites(user, workspace, PLATFORM, deploymentAddress)).thenReturn(expected);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(workspace.getId(), PLATFORM, deploymentAddress);

        assertEquals("The result CredentialPrerequisites object is not the expected one!", expected, result);
        verify(credentialPrerequisiteService, times(1)).getPrerequisites(user, workspace, PLATFORM, deploymentAddress);
    }

    @Test
    public void testInitCodeGrantFlowWhenEverythingIsFineThenExpectedUrlShouldComeBack() {
        Credential created = mock(Credential.class);
        String key = "appLoginUrl";
        String expected = "someValue";
        when(created.getAttributes()).thenReturn(format("{\"%s\":\"%s\"}", key, expected));
        when(credentialRepository.save(created)).thenReturn(created);
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(workspace));
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);
        when(testCredential.cloudPlatform()).thenReturn(PLATFORM);
        when(testCredential.getAttributes()).thenReturn(format("{\"%s\":\"%s\"}", "deploymentAddress", "https://MY_DEPLOYMENT_ADDRESS"));
        when(credentialAdapter.initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(created);

        String result = underTest.initCodeGrantFlow(WORKSPACE_ID, testCredential, user);

        assertEquals(expected, result);
        verify(credentialAdapter, times(1)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID);
    }

    @Test
    public void testInitCodeGrantFlowWhenCredentialTheSpecifiedCredentialDoesNotContainDeploymentAddressAttributeThenBadRequestExceptionShouldBeThrown() {
        when(testCredential.cloudPlatform()).thenReturn(PLATFORM);
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND);

        underTest.initCodeGrantFlow(WORKSPACE_ID, testCredential);

        verify(credentialAdapter, times(0)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
    }

    @Test
    public void testInitCodeGrantFlowWhenEverythingIsFineButCredentialAttributesDoesNotContainsAppLoginUrlKeyThenNullReturns() {
        Credential created = mock(Credential.class);
        when(created.getAttributes()).thenReturn("{}");
        when(credentialRepository.save(created)).thenReturn(created);
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(workspace));
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);
        when(testCredential.cloudPlatform()).thenReturn(PLATFORM);
        when(testCredential.getAttributes()).thenReturn(format("{\"%s\":\"%s\"}", "deploymentAddress", "https://MY_DEPLOYMENT_ADDRESS"));
        when(credentialAdapter.initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(created);

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectMessage("Unable to obtain App login url!");

        underTest.initCodeGrantFlow(WORKSPACE_ID, testCredential);

        verify(credentialAdapter, times(1)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID);
    }

    @Test
    public void testInitCodeGrantFlowWhenCredentialIsNotAvailableFromRepositoryThenNotFoundExceptionComes() {
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("%s '%s' not found.", "Credential with name:", TEST_CREDENTIAL_NAME));

        underTest.initCodeGrantFlow(WORKSPACE_ID, TEST_CREDENTIAL_NAME);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anySet());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialAdapter, times(0)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
        verify(secretService, times(0)).delete(anyString());
    }

    @Test
    public void testInitCodeGrantFlowWhenCredentialAttributesContainsCodeGrantFlowKeyAndItIsFalseThenUnsupportedOperationExceptionComes() {
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(testCredential));
        when(testCredential.getAttributes()).thenReturn("{\"codeGrantFlow\":false}");

        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("This operation is only allowed on Authorization Code Grant flow based credentails.");

        underTest.initCodeGrantFlow(WORKSPACE_ID, TEST_CREDENTIAL_NAME);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anySet());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialAdapter, times(0)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
        verify(secretService, times(0)).delete(anyString());
    }

    @Test
    public void testInitCodeGrantFlowWhenCodeGrantFlowAttributeTrueThenUpdatedAttributesContainsAppLoginUrlKeyAndReturningItsValue() {
        String originalAttributes = "{\"codeGrantFlow\":true}";
        String key = "appLoginUrl";
        String expected = "someValue";
        Credential updatedCredential = mock(Credential.class);
        when(updatedCredential.getAttributes()).thenReturn(format("{\"%s\":\"%s\"}", key, expected));
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(testCredential));
        when(testCredential.getAttributes()).thenReturn(originalAttributes);
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(workspace));
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);
        when(credentialAdapter.initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(updatedCredential);
        doNothing().when(secretService).delete(originalAttributes);
        when(credentialRepository.save(updatedCredential)).thenReturn(updatedCredential);

        String result = underTest.initCodeGrantFlow(WORKSPACE_ID, TEST_CREDENTIAL_NAME);

        assertEquals(expected, result);
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anySet());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialAdapter, times(1)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID);
        verify(secretService, times(1)).delete(anyString());
        verify(secretService, times(1)).delete(originalAttributes);
    }

    @Test
    public void testInitCodeGrantFlowWhenCodeGrantFlowAttributeTrueButAfterUpdateDoesNotContainsAppLoginUrlThenStringNullReturns() {
        String originalAttributes = "{\"codeGrantFlow\":true}";
        Credential updatedCredential = mock(Credential.class);
        when(updatedCredential.getAttributes()).thenReturn("{}");
        when(credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS))
                .thenReturn(Optional.ofNullable(testCredential));
        when(testCredential.getAttributes()).thenReturn(originalAttributes);
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(workspace));
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);
        when(credentialAdapter.initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(updatedCredential);
        doNothing().when(secretService).delete(originalAttributes);
        when(credentialRepository.save(updatedCredential)).thenReturn(updatedCredential);

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectMessage("Unable to obtain App login url!");

        underTest.initCodeGrantFlow(WORKSPACE_ID, TEST_CREDENTIAL_NAME);

        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(anyString(), anyLong(), anySet());
        verify(credentialRepository, times(1)).findActiveByNameAndWorkspaceIdFilterByPlatforms(TEST_CREDENTIAL_NAME, WORKSPACE_ID, CLOUD_PLATFORMS);
        verify(preferencesService, times(1)).enabledPlatforms();
        verify(credentialAdapter, times(1)).initCodeGrantFlow(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).initCodeGrantFlow(testCredential, WORKSPACE_ID, USER_ID);
        verify(secretService, times(1)).delete(anyString());
        verify(secretService, times(1)).delete(originalAttributes);
    }

    @Test
    public void testAuthorizeCodeGrantFlowWhenThereIsNoCredentialsAtAllGivenFromWorkspaceIdAndCloudPlatformThenNotFoundExceptionComes() {
        when(credentialRepository.findActiveForWorkspaceFilterByPlatforms(WORKSPACE_ID, List.of(PLATFORM))).thenReturn(Collections.emptySet());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(format("%s '%s' not found.", "Code grant flow based credential for user with state:", AUTHORIZE_GRANT_FLOW_STATE));

        underTest.authorizeCodeGrantFlow(AUTHORIZE_GRANT_FLOW_CODE, AUTHORIZE_GRANT_FLOW_STATE, WORKSPACE_ID, PLATFORM);

        verify(credentialRepository, times(1)).findActiveForWorkspaceFilterByPlatforms(anyLong(), anySet());
        verify(credentialRepository, times(1)).findActiveForWorkspaceFilterByPlatforms(WORKSPACE_ID, Set.of(PLATFORM));
        verify(credentialAdapter, times(0)).verify(any(Credential.class), anyLong(), anyString());
        verify(secretService, times(0)).delete(anyString());
    }

    @Test
    public void testAuthorizeCodeGrantFlowWhenCredentialFouldThenAuthorizationCodeHasBeenSetToCredentialAttribute() {
        String originalAttributes = format("{\"codeGrantFlowState\":\"%s\"}", AUTHORIZE_GRANT_FLOW_STATE);
        String attributeSecret = "someExtremelyBrutallySecretValue";
        when(testCredential.getAttributesSecret()).thenReturn(attributeSecret);
        when(testCredential.getAttributes()).thenReturn(originalAttributes);
        Credential expectedCredential = mock(Credential.class);
        when(credentialRepository.findActiveForWorkspaceFilterByPlatforms(WORKSPACE_ID, List.of(PLATFORM))).thenReturn(Set.of(testCredential));
        when(credentialAdapter.verify(testCredential, WORKSPACE_ID, USER_ID)).thenReturn(expectedCredential);
        when(credentialRepository.save(expectedCredential)).thenReturn(expectedCredential);
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(workspace));
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);

        Credential result = underTest.authorizeCodeGrantFlow(AUTHORIZE_GRANT_FLOW_CODE, AUTHORIZE_GRANT_FLOW_STATE, WORKSPACE_ID, PLATFORM);

        assertEquals(expectedCredential, result);
        verify(credentialRepository, times(1)).findActiveForWorkspaceFilterByPlatforms(anyLong(), anyList());
        verify(credentialRepository, times(1)).findActiveForWorkspaceFilterByPlatforms(WORKSPACE_ID, List.of(PLATFORM));
        verify(credentialAdapter, times(1)).verify(any(Credential.class), anyLong(), anyString());
        verify(credentialAdapter, times(1)).verify(testCredential, WORKSPACE_ID, USER_ID);
        verify(secretService, times(1)).delete(anyString());
        verify(secretService, times(1)).delete(attributeSecret);
        verify(testCredential, times(1)).setAttributes(
                format("{\"codeGrantFlowState\":\"%s\",\"authorizationCode\":\"%s\"}", AUTHORIZE_GRANT_FLOW_STATE, AUTHORIZE_GRANT_FLOW_CODE));
    }

    private Credential createCredential(String name) {
        Credential credential = new Credential();
        credential.setName(name);
        credential.setArchived(false);
        credential.setTopology(new Topology());
        credential.setWorkspace(workspace);

        return credential;
    }

    private void verifyCredentialsAreArchived(Credential... credentials) {
        for (Credential credential : credentials) {
            verify(stackService, times(1)).findByCredential(credential);
            verify(userProfileHandler, times(1)).destroyProfileCredentialPreparation(credential);
            verify(credentialRepository, times(1)).save(credential);
        }
        int numOfCredentials = credentials.length;
        verify(notificationSender, times(numOfCredentials)).send(any());
        verify(messagesService, times(numOfCredentials)).getMessage(ResourceEvent.CREDENTIAL_DELETED.getMessage());
    }
}