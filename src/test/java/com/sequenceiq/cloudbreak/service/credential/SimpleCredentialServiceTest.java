package com.sequenceiq.cloudbreak.service.credential;

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.converter.AwsCredentialConverter;
import com.sequenceiq.cloudbreak.converter.AzureCredentialConverter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.AwsCredentialRepository;
import com.sequenceiq.cloudbreak.repository.AzureCredentialRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;

public class SimpleCredentialServiceTest {

    @InjectMocks
    private SimpleCredentialService underTest;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AwsCredentialRepository awsCredentialRepository;

    @Mock
    private AwsCredentialConverter awsCredentialConverter;

    @Mock
    private AzureCertificateService azureCertificateService;

    @Mock
    private AzureCredentialRepository azureCredentialRepository;

    @Mock
    private AzureCredentialConverter azureCredentialConverter;

    @Mock
    private CredentialJson credentialJson;

    @Mock
    private WebsocketService websocketService;

    @Mock
    private AccountService accountService;

    private User user;

    private AwsCredential awsCredential;

    private AzureCredential azureCredential;

    @Before
    public void setUp() {
        underTest = new SimpleCredentialService();
        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setEmail("dummy@mymail.com");
        awsCredential = (AwsCredential) ServiceTestUtils.createCredential(user, CloudPlatform.AWS, UserRole.ACCOUNT_USER);
        azureCredential = (AzureCredential) ServiceTestUtils.createCredential(user, CloudPlatform.AZURE, UserRole.ACCOUNT_USER);
    }

//    @Test
//    public void testSaveAwsCredential() {
//        // GIVEN
//        given(credentialJson.getCloudPlatform()).willReturn(CloudPlatform.AWS);
//        given(awsCredentialConverter.convert(credentialJson)).willReturn(awsCredential);
//        given(awsCredentialRepository.save(awsCredential)).willReturn(awsCredential);
//        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
//        // WHEN
//        underTest.save(user, awsCredential);
//        // THEN
//        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
//        verify(azureCertificateService, times(0)).generateCertificate(any(AzureCredential.class), any(User.class));
//    }
//
//    @Test
//    public void testSaveAzureCredential() {
//        // GIVEN
//        given(credentialJson.getCloudPlatform()).willReturn(CloudPlatform.AZURE);
//        given(azureCredentialConverter.convert(credentialJson)).willReturn(azureCredential);
//        given(azureCredentialRepository.save(azureCredential)).willReturn(azureCredential);
//        doNothing().when(azureCertificateService).generateCertificate(azureCredential, user);
//        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
//        // WHEN
//        underTest.save(user, azureCredential);
//        // THEN
//        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
//        verify(azureCertificateService, times(1)).generateCertificate(any(AzureCredential.class), any(User.class));
//    }
//
//    @Test
//    public void testDeleteAwsCredential() {
//        // GIVEN
//        given(credentialRepository.findOne(1L)).willReturn(awsCredential);
//        doNothing().when(credentialRepository).delete(awsCredential);
//        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
//        // WHEN
//        underTest.delete(1L);
//        // THEN
//        verify(credentialRepository, times(1)).delete(awsCredential);
//    }
//
//    @Test(expected = NotFoundException.class)
//    public void testDeleteAwsCredentialWhenCredentialNotFound() {
//        // GIVEN
//        given(credentialRepository.findOne(1L)).willReturn(null);
//        // WHEN
//        underTest.delete(1L);
//    }
//
//    @Test
//    public void testGetAllForAccountAdminWithAccountUserWithCredentials() {
//        // GIVEN
//        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
//        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
//        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
//        // admin has credentials
//        admin.getAwsCredentials().add((AwsCredential) ServiceTestUtils.createCredential(admin, CloudPlatform.AWS, UserRole.ACCOUNT_ADMIN));
//        admin.getAzureCredentials().add((AzureCredential) ServiceTestUtils.createCredential(admin, CloudPlatform.AZURE, UserRole.ACCOUNT_ADMIN));
//        // cUser also has credentials
//        cUser.getAwsCredentials().add((AwsCredential) ServiceTestUtils.createCredential(admin, CloudPlatform.AWS, UserRole.ACCOUNT_USER));
//        given(accountService.accountUsers(account.getId())).willReturn(new HashSet<User>(Arrays.asList(cUser)));
//
//        // WHEN
//        Set<Credential> credentials = underTest.getAll(admin);
//
//        // THEN
//        Assert.assertNotNull(credentials);
//        Assert.assertTrue("The number of the returned blueprints is right", credentials.size() == 3);
//    }
//
//    @Test
//    public void testGetAllForAccountUserWithVisibleAccountCredentials() {
//
//        // GIVEN
//        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
//        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
//        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
//        // admin has credentials
//        admin.getAwsCredentials().add((AwsCredential) ServiceTestUtils.createCredential(admin, CloudPlatform.AWS, UserRole.ACCOUNT_ADMIN));
//        admin.getAzureCredentials().add((AzureCredential) ServiceTestUtils.createCredential(admin, CloudPlatform.AZURE, UserRole.ACCOUNT_USER));
//        // cUser also has credentials
//        cUser.getAwsCredentials().add((AwsCredential) ServiceTestUtils.createCredential(admin, CloudPlatform.AWS, UserRole.ACCOUNT_USER));
//        given(accountService.accountUserData(account.getId(), UserRole.ACCOUNT_USER)).willReturn(admin);
//
//        // WHEN
//        Set<Credential> credentials = underTest.getAll(cUser);
//
//        // THEN
//        Assert.assertNotNull(credentials);
//        Assert.assertTrue("The number of the returned blueprints is right", credentials.size() == 3);
//
//    }

}
