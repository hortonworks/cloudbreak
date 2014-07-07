package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.converter.AwsCredentialConverter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.AwsCredentialRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class SimpleCredentialServiceTest {

    @InjectMocks
    private SimpleCredentialService underTest;

    private User user;

    private AwsCredential awsCredential;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AwsCredentialRepository awsCredentialRepository;

    @Mock
    private AwsCredentialConverter awsCredentialConverter;

    @Mock
    private CredentialJson credentialJson;

    @Mock
    private WebsocketService websocketService;

    @Before
    public void setUp() {
        underTest = new SimpleCredentialService();
        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setEmail("dummy@mymail.com");
        awsCredential = new AwsCredential();
        awsCredential.setAwsCredentialOwner(user);
    }

    @Test
    public void testSaveAwsCredential() {
        //GIVEN
        given(credentialJson.getCloudPlatform()).willReturn(CloudPlatform.AWS);
        given(awsCredentialConverter.convert(credentialJson)).willReturn(awsCredential);
        given(awsCredentialRepository.save(awsCredential)).willReturn(awsCredential);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        //WHEN
        underTest.save(user, credentialJson);
        //THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
    }

    @Test
    public void testDeleteAwsCredential() {
        //GIVEN
        given(credentialRepository.findOne(1L)).willReturn(awsCredential);
        doNothing().when(credentialRepository).delete(awsCredential);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
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
}
