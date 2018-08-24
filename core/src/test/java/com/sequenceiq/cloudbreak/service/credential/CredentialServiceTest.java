package com.sequenceiq.cloudbreak.service.credential;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlarformService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    private static final String PLATFORM = "OPENSTACK";

    private static final String TEST_CREDENTIAL_NAME = "testCredentialName";

    private static final String TEST_ORGANIZATION_NAME = "test@org.name";

    private static final Long DEFAULT_ORG_ID = 1L;

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
    private CloudPlarformService cloudPlarformService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private Organization defaultOrg;

    @InjectMocks
    private final CredentialService credentialService = new CredentialService();

    private Credential credentialToModify;

    private Topology originalTopology;

    private String originalDescription;

    private Json originalAttributes;

    private Credential testCredential;

    @Before
    public void init() throws Exception {
        credentialToModify = new Credential();
        credentialToModify.setId(1L);
        credentialToModify.setCloudPlatform(PLATFORM);
        originalTopology = new Topology();
        credentialToModify.setTopology(originalTopology);
        originalDescription = "orig-desc";
        credentialToModify.setDescription(originalDescription);
        originalAttributes = new Json("test");
        credentialToModify.setAttributes(originalAttributes);
        when(credentialRepository.save(any(Credential.class))).then(invocation -> invocation.getArgument(0));
        testCredential = mock(Credential.class);
        when(testCredential.getName()).thenReturn(TEST_CREDENTIAL_NAME);
    }

    @Test
    public void testCanDeleteWhenCredentialIsNullThenNotFoundExceptionShouldCome() {
        thrown.expect(NotFoundException.class);

        credentialService.canDelete(null);

        verify(stackRepository, times(0)).findByCredential(any(Credential.class));
    }

    @Test
    public void testCanDeleteWhenStackRepositoryDoesNotFindAnyStackWithTheGivenCredentialThenTrueShouldCome() {
        when(stackRepository.findByCredential(testCredential)).thenReturn(Collections.emptySet());

        boolean result = credentialService.canDelete(testCredential);

        assertTrue(result);
        verify(stackRepository, times(1)).findByCredential(testCredential);
    }

    @Test
    public void testCanDeleteWhenOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stackName = "testStackName";
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(stackName);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                + "The following cluster is using this credential: [%s]", TEST_CREDENTIAL_NAME, stackName));

        credentialService.canDelete(testCredential);

        verify(stackRepository, times(1)).findByCredential(testCredential);
    }

    @Test
    public void testCanDeleteWhenMoreThaneOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stack1Name = "testStack1Name";
        String stack2Name = "testStack1Name";
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn(stack1Name);
        when(stack2.getName()).thenReturn(stack2Name);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack1, stack2));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                + "The following clusters are using this credential: [%s]", TEST_CREDENTIAL_NAME, String.format("%s, %s", stack1Name, stack2Name)));

        credentialService.canDelete(testCredential);

        verify(stackRepository, times(1)).findByCredential(testCredential);
    }

    @Test
    public void testDeleteWhenOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stackName = "testStackName";
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(stackName);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                + "The following cluster is using this credential: [%s]", TEST_CREDENTIAL_NAME, stackName));

        credentialService.delete(testCredential, defaultOrg);

        verify(stackRepository, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteWhenMoreThaneOneStackUsesTheGivenCredentialThenBadRequestExceptionShouldComeWithExpectedMessage() {
        String stack1Name = "testStack1Name";
        String stack2Name = "testStack1Name";
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn(stack1Name);
        when(stack2.getName()).thenReturn(stack2Name);
        when(stackRepository.findByCredential(testCredential)).thenReturn(Set.of(stack1, stack2));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                + "The following clusters are using this credential: [%s]", TEST_CREDENTIAL_NAME, String.format("%s, %s", stack1Name, stack2Name)));

        credentialService.delete(testCredential, defaultOrg);

        verify(stackRepository, times(1)).findByCredential(testCredential);
        verify(userProfileHandler, times(0)).destroyProfileCredentialPreparation(testCredential);
        verify(credentialRepository, times(0)).save(testCredential);
    }

    @Test
    public void testDeleteWhenCredentialIsDeletableThenItWillBeArchivedProperly() {
        Credential credential = new Credential();
        credential.setName(TEST_CREDENTIAL_NAME);
        credential.setArchived(false);
        credential.setTopology(new Topology());
        when(stackRepository.findByCredential(credential)).thenReturn(Collections.emptySet());

        Credential deleted = credentialService.delete(credential, defaultOrg);

        assertTrue(deleted.isArchived());
        assertNull(deleted.getTopology());
        assertNotEquals(TEST_CREDENTIAL_NAME, credential.getName());

        verify(stackRepository, times(1)).findByCredential(credential);
        verify(userProfileHandler, times(1)).destroyProfileCredentialPreparation(credential);
        verify(credentialRepository, times(1)).save(credential);
    }

}