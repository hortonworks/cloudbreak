package com.sequenceiq.cloudbreak.ambari;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AmbariUserHandler.class)
public class AmbariUserHandlerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @MockBean
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private final AmbariUserHandler underTest = new AmbariUserHandler();

    private HttpClientConfig clientConfig = new HttpClientConfig("1.1.1.1");

    @Test
    public void testCreateAmbariUserWhenNoExceptionOccuredThenEverythingShouldWorksFine() throws CloudbreakException {
        String newUserName = "newUserName";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(newUserName, newPassword, true)).thenReturn(ambariClient);
        when(ambariClient.getUser(eq(newUserName))).thenReturn("user");
        when(ambariClientFactory.getAmbariClient(eq(stack), eq(newUserName), eq(newPassword), any(HttpClientConfig.class))).thenReturn(ambariClient);

        AmbariClient ambariClientResult = underTest.createAmbariUser(newUserName, newPassword, stack, ambariClient, clientConfig);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(newUserName, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, newUserName, newPassword, clientConfig);
        verify(ambariClient, times(1)).getUser(newUserName);
    }

    @Test
    public void testCreateAmbariUserWhenExceptionOccuredThenTryingToCreateNewAmbariClientAndQueryUser() throws CloudbreakException {
        String newUserName = "newUserName";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(newUserName, newPassword, true))
                .thenThrow(new AmbariServiceException("failed", new AmbariOperationFailedException("failed")));
        when(ambariClient.getUser(eq(newUserName))).thenReturn("user");
        when(ambariClientFactory.getAmbariClient(eq(stack), eq(newUserName), eq(newPassword), any(HttpClientConfig.class))).thenReturn(ambariClient);

        AmbariClient ambariClientResult = underTest.createAmbariUser(newUserName, newPassword, stack, ambariClient, clientConfig);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(newUserName, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, newUserName, newPassword, clientConfig);
        verify(ambariClient, times(1)).getUser(newUserName);
    }

    @Test
    public void testCreateAmbariUserWhenExceptionOccuredThenTryingToCreateNewClientAndQueryUserAndThrowExceptionThenShouldThrowCloudbreakException()
            throws CloudbreakException {
        String newUserName = "newUserName";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(newUserName, newPassword, true)).thenThrow(new AmbariServiceException("failed"));
        when(ambariClient.getUser(eq(newUserName))).thenThrow(new AmbariConnectionException("can not connect to ambari"));
        when(ambariClientFactory.getAmbariClient(stack, newUserName, newPassword, clientConfig)).thenReturn(ambariClient);

        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("Can not use user: " + newUserName);

        AmbariClient ambariClientResult = underTest.createAmbariUser(newUserName, newPassword, stack, ambariClient, clientConfig);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(newUserName, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, newUserName, newPassword, clientConfig);
        verify(ambariClient, times(1)).getUser(newUserName);
    }

    @Test
    public void testChangeAmbariPasswordWhenNoExceptionOccuredThenEverythingShouldWorksFine() throws CloudbreakException {
        String userName = "cloudbreak";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(userName, newPassword, true)).thenReturn(ambariClient);
        when(ambariClient.getUser(eq(userName))).thenReturn("user");
        when(ambariClientFactory.getAmbariClient(eq(stack), eq(userName), eq(newPassword), any(HttpClientConfig.class))).thenReturn(ambariClient);

        AmbariClient ambariClientResult = underTest.changeAmbariPassword(userName, oldPassword, newPassword, stack, ambariClient, clientConfig);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).changePassword(userName, oldPassword, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, userName, newPassword, clientConfig);
        verify(ambariClient, times(1)).getUser(userName);
    }

    @Test
    public void testChangeAmbariPasswordWhenExceptionOccuredThenTryingToCreateNewAmbariClientAndQueryUser() throws CloudbreakException {
        String userName = "cloudbreak";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(userName, newPassword, true))
                .thenThrow(new AmbariServiceException("failed", new AmbariOperationFailedException("failed")));
        when(ambariClient.getUser(eq(userName))).thenReturn("user");
        when(ambariClientFactory.getAmbariClient(eq(stack), eq(userName), eq(newPassword), any(HttpClientConfig.class))).thenReturn(ambariClient);

        AmbariClient ambariClientResult = underTest.changeAmbariPassword(userName, oldPassword, newPassword, stack, ambariClient, clientConfig);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).changePassword(userName, oldPassword, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, userName, newPassword, clientConfig);
        verify(ambariClient, times(1)).getUser(userName);
    }

    @Test
    public void testChangeAmbariPasswordWhenExceptionOccuredThenTryingToCreateNewClientAndQueryUserAndThrowExceptionThenShouldThrowCloudbreakException()
            throws CloudbreakException {
        String userName = "cloudbreak";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(userName, newPassword, true)).thenThrow(new AmbariServiceException("failed"));
        when(ambariClient.getUser(eq(userName))).thenThrow(new AmbariConnectionException("can not connect to ambari"));
        when(ambariClientFactory.getAmbariClient(stack, userName, newPassword, clientConfig)).thenReturn(ambariClient);

        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("Can not use user: " + userName);

        AmbariClient ambariClientResult = underTest.changeAmbariPassword(userName, oldPassword, newPassword, stack, ambariClient, clientConfig);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(userName, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, userName, newPassword, clientConfig);
        verify(ambariClient, times(1)).getUser(userName);
    }
}