package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.converter.AwsCredentialConverter;
import com.sequenceiq.cloudbreak.converter.AzureCredentialConverter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.HistoryEvent;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.AwsCredentialRepository;
import com.sequenceiq.cloudbreak.repository.AzureCredentialRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.history.HistoryService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    private HistoryService historyService;

    private User user;

    private AwsCredential awsCredential;

    private AzureCredential azureCredential;

    @Before
    public void setUp() {
        underTest = new SimpleCredentialService();
        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setEmail("dummy@mymail.com");
        awsCredential = new AwsCredential();
        awsCredential.setAwsCredentialOwner(user);
        azureCredential = new AzureCredential();
        azureCredential.setAzureCredentialOwner(user);
    }

    @Test
    public void testSaveAwsCredential() {
        //GIVEN
        given(credentialJson.getCloudPlatform()).willReturn(CloudPlatform.AWS);
        given(awsCredentialConverter.convert(credentialJson)).willReturn(awsCredential);
        given(awsCredentialRepository.save(awsCredential)).willReturn(awsCredential);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        doNothing().when(historyService).notify(any(ProvisionEntity.class), any(HistoryEvent.class));
        //WHEN
        underTest.save(user, credentialJson);
        //THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        verify(azureCertificateService, times(0)).generateCertificate(any(AzureCredential.class), any(User.class));
    }

    @Test
    public void testSaveAzureCredential() {
        // GIVEN
        given(credentialJson.getCloudPlatform()).willReturn(CloudPlatform.AZURE);
        given(azureCredentialConverter.convert(credentialJson)).willReturn(azureCredential);
        given(azureCredentialRepository.save(azureCredential)).willReturn(azureCredential);
        doNothing().when(azureCertificateService).generateCertificate(azureCredential, user);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        doNothing().when(historyService).notify(any(ProvisionEntity.class), any(HistoryEvent.class));
        // WHEN
        underTest.save(user, credentialJson);
        // THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        verify(azureCertificateService, times(1)).generateCertificate(any(AzureCredential.class), any(User.class));
    }

    @Test
    public void testDeleteAwsCredential() {
        //GIVEN
        given(credentialRepository.findOne(1L)).willReturn(awsCredential);
        doNothing().when(credentialRepository).delete(awsCredential);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        doNothing().when(historyService).notify(any(ProvisionEntity.class), any(HistoryEvent.class));
        //WHEN
        underTest.delete(1L);
        //THEN
        verify(credentialRepository, times(1)).delete(awsCredential);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteAwsCredentialWhenCredentialNotFound() {
        //GIVEN
        given(credentialRepository.findOne(1L)).willReturn(null);
        //WHEN
        underTest.delete(1L);
    }

    @Test
    public void testGetAll() {
        // GIVEN
        given(awsCredentialConverter.convertAllEntityToJson(anySetOf(AwsCredential.class))).willReturn(new HashSet<CredentialJson>());
        given(azureCredentialConverter.convertAllEntityToJson(anySetOf(AzureCredential.class))).willReturn(new HashSet<CredentialJson>());
        // WHEN
        underTest.getAll(user);
        // THEN
        verify(awsCredentialConverter, times(1)).convertAllEntityToJson(anySetOf(AwsCredential.class));
        verify(azureCredentialConverter, times(1)).convertAllEntityToJson(anySetOf(AzureCredential.class));
    }

    @Test
    public void testGetAwsCredential() {
        // GIVEN
        given(credentialRepository.findOne(1L)).willReturn(awsCredential);
        given(awsCredentialConverter.convert(any(AwsCredential.class))).willReturn(credentialJson);
        // WHEN
        underTest.get(1L);
        // THEN
        verify(awsCredentialConverter, times(1)).convert(any(AwsCredential.class));
    }

    @Test
    public void testGetAzureCredential() {
        // GIVEN
        given(credentialRepository.findOne(1L)).willReturn(azureCredential);
        given(azureCredentialConverter.convert(any(AzureCredential.class))).willReturn(credentialJson);
        // WHEN
        underTest.get(1L);
        // THEN
        verify(azureCredentialConverter, times(1)).convert(any(AzureCredential.class));
    }

}
