package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.json.MockJsonFactory;
import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@RunWith(MockitoJUnitRunner.class)
public class GcpCredentialConnectorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private GcpCredentialConnector underTest;

    @Mock
    private GcpContextBuilder contextBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GcpContext context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test that if some exception terminates the credential check operation,
     * or the checked credential is not valid. If that so, than the expected
     * CredentialStatus should be FAILED.
     *
     * @throws IOException this exception could thrown by the credential
     *                     checker method.
     */
    @Test
    public void testForNullExceptionOnVerifyPermissionCheck() throws IOException {
        final AuthenticatedContext authContext = createAuthContext();
        final String expectionReasonMessage = "exception message";
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        when(context.getProjectId()).thenReturn("some id");
        when(context.getServiceAccountId()).thenReturn("some service id");
        when(context.getCompute().regions().list(anyString()).executeUsingHead()).thenThrow(new BadRequestException(expectionReasonMessage));

        final CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
        Assert.assertTrue("Not the specified exception has come with the status", status.getException() instanceof BadRequestException);
        Assert.assertEquals("Not the expected exception message has come with the status", expectionReasonMessage, status.getException().getMessage());
    }

    /**
     * Test that if the credential authentication has passed, the returned
     * CredentialStatus is VERIFIED.
     *
     * @throws IOException this exception could thrown by the credential
     *                     checker method.
     */
    @Test
    public void testPassingVerifyPermissionCheck() throws IOException {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        when(context.getProjectId()).thenReturn("some id");
        when(context.getServiceAccountId()).thenReturn("some service id");

        final CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.VERIFIED, status.getStatus());
    }

    /**
     * Testing the GcpContext checking mechanism. If the inner created
     * GcpContext does not contains a valid project id then a FAILED
     * status should come back.
     */
    @Test
    public void testForFailedStatusBecauseMissingPrjId() {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        when(context.getProjectId()).thenReturn(null);

        final CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
    }

    /**
     * Testing the GcpContext checking mechanism. If the inner created
     * GcpContext does not contains a valid service account id then a FAILED
     * status should come back.
     */
    @Test
    public void testForFailedStatusBecauseMissingServiceAccId() {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        when(context.getProjectId()).thenReturn("some id");
        when(context.getServiceAccountId()).thenReturn(null);

        final CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
    }

    /**
     * Testing the GcpContext checking mechanism. If the inner created
     * GcpContext does not contains a valid Compute instance then a FAILED
     * status should come back.
     */
    @Test
    public void testForFailedStatusBecauseMissingCompute() {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        when(context.getProjectId()).thenReturn("some id");
        when(context.getServiceAccountId()).thenReturn("some service id");
        when(context.getCompute()).thenReturn(null);

        final CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
    }


    /**
     * On GCP there is no interactive login option, so when someone calls this
     * implementation, a UnsupportedOperationException should be thrown.
     */
    @Test
    public void testInteractiveLoginIsProhibitedOnGcp() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null);
    }


    /**
     * Test that if the delete function has called with a proper
     * AuthenticationContext, then the returning CredentialStatus
     * should be DELETED.
     */
    @Test
    public void testDeletePositive() {
        final CloudCredentialStatus status = underTest.delete(createAuthContext());

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.DELETED, status.getStatus());
    }

    /**
     * Test that if the create function has called with a proper
     * AuthenticationContext, then the returning CredentialStatus
     * should be CREATED.
     */
    @Test
    public void testCreatePositive() {
        final CloudCredentialStatus status = underTest.create(createAuthContext());

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.CREATED, status.getStatus());
    }

    private AuthenticatedContext createAuthContext() {
        return new AuthenticatedContext(createCloudContext(), createCloudCredential());
    }

    private CloudContext createCloudContext() {
        return new CloudContext(1L, "name", "platform", "owner");
    }

    private CloudCredential createCloudCredential() {
        return new CloudCredential(1L, "name", new HashMap<String, Object>() {
            {
                put("projectId", "some id");
                put("serviceAccountId", "some service account");
                put("compute", createDummyCompute());
            }
        });
    }

    private Compute createDummyCompute() {
        return new Compute(new MockHttpTransport(), new MockJsonFactory(), request -> {
        });
    }

}
