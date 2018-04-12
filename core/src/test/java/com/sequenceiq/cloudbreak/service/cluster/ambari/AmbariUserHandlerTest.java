package com.sequenceiq.cloudbreak.service.cluster.ambari;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AmbariUserHandlerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariClientFactory ambariClientFactory;

    @InjectMocks
    private AmbariUserHandler underTest = new AmbariUserHandler();

    @Test
    public void testCreateAmbariUserWhenNoExceptionOccuredThenEverythingShouldWorksFine() throws CloudbreakException {
        String newUserName = "newUserName";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(newUserName, newPassword, true)).thenReturn(ambariClient);

        AmbariClient ambariClientResult = underTest.createAmbariUser(newUserName, newPassword, stack, ambariClient);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(newUserName, newPassword, true);
        verify(ambariClientFactory, times(0)).getAmbariClient(stack, newUserName, newPassword);
        verify(ambariClient, times(0)).ambariServerVersion();
    }

    @Test
    public void testCreateAmbariUserWhenExceptionOccuredThenTryingToCreateNewAmbariClientAndQueryAmbariVersion() throws CloudbreakException {
        String newUserName = "newUserName";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(newUserName, newPassword, true))
                .thenThrow(new AmbariServiceException("failed", new AmbariOperationFailedException("failed")));
        when(ambariClient.ambariServerVersion()).thenReturn("2.4");
        when(ambariClientFactory.getAmbariClient(stack, newUserName, newPassword)).thenReturn(ambariClient);

        AmbariClient ambariClientResult = underTest.createAmbariUser(newUserName, newPassword, stack, ambariClient);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(newUserName, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, newUserName, newPassword);
        verify(ambariClient, times(1)).ambariServerVersion();
    }

    @Test
    public void testCreateAmbariUserWhenExceptionOccuredThenTryingToCreateNewClientAndGetAmbariVersionAndThrowExceptionThenShouldThrowCloudbreakException()
            throws CloudbreakException {
        String newUserName = "newUserName";
        String newPassword = "newPassword";
        Stack stack = TestUtil.stack();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.createUser(newUserName, newPassword, true)).thenThrow(new AmbariServiceException("failed"));
        when(ambariClient.ambariServerVersion()).thenReturn("2.4");
        when(ambariClientFactory.getAmbariClient(stack, newUserName, newPassword)).thenThrow(new AmbariServiceException("failed"));

        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("failed");

        AmbariClient ambariClientResult = underTest.createAmbariUser(newUserName, newPassword, stack, ambariClient);

        Assert.assertEquals(ambariClient, ambariClientResult);

        verify(ambariClient, times(1)).createUser(newUserName, newPassword, true);
        verify(ambariClientFactory, times(1)).getAmbariClient(stack, newUserName, newPassword);
        verify(ambariClient, times(0)).ambariServerVersion();
    }
}