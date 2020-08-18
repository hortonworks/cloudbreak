package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.cloud.gcp.context.InvalidGcpContextException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@RunWith(MockitoJUnitRunner.class)
public class GcpCredentialConnectorTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final Map<String, Object> CREDENTIAL_PARAMETERS = new HashMap<>();

    private static final Map<String, Object> GCP_PARAMETERS = new HashMap<>();

    private static final Map<String, Object> GCP_JSON = new HashMap<>();

    static {
        GCP_JSON.put("credentialJson", "");

        GCP_PARAMETERS.put("compute", new Compute(new MockHttpTransport(), new MockJsonFactory(), request -> {
        }));
        GCP_PARAMETERS.put("serviceAccountId", "some service account");
        GCP_PARAMETERS.put("projectId", "some id");
        GCP_PARAMETERS.put("json", GCP_JSON);

        CREDENTIAL_PARAMETERS.put("gcp", GCP_PARAMETERS);
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private GcpCredentialConnector underTest;

    @Mock
    private GcpContextBuilder contextBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GcpContext context;

    @Mock
    private GcpCredentialVerifier gcpCredentialVerifier;

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
        AuthenticatedContext authContext = createAuthContext();
        String expectionReasonMessage = "exception message";
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        doThrow(new BadRequestException(expectionReasonMessage)).when(gcpCredentialVerifier).preCheckOfGooglePermission(context);

        CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
        Assert.assertTrue("Not the specified exception has come with the status", status.getException() instanceof BadRequestException);
        Assert.assertEquals("Not the expected exception message has come with the status", expectionReasonMessage, status.getException().getMessage());
    }

    /**
     * Test that if the credential authentication has passed, the returned
     * CredentialStatus is VERIFIED.
     */
    @Test
    public void testPassingVerifyPermissionCheck() {
        AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);

        CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.VERIFIED, status.getStatus());
    }

    /**
     * Testing the GcpContext checking mechanism. If the inner created
     * GcpContext does not contains a valid project id then a FAILED
     * status should come back.
     */
    @Test
    public void testForFailedStatusBecauseMissingPrjId() throws InvalidGcpContextException {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        doThrow(new NullPointerException()).when(gcpCredentialVerifier).checkGcpContextValidity(context);

        CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
    }

    /**
     * Testing the GcpContext checking mechanism. If the inner created
     * GcpContext does not contains a valid service account id then a FAILED
     * status should come back.
     */
    @Test
    public void testForFailedStatusBecauseMissingServiceAccId() throws InvalidGcpContextException {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        doThrow(new NullPointerException()).when(gcpCredentialVerifier).checkGcpContextValidity(context);

        CloudCredentialStatus status = underTest.verify(authContext);

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.FAILED, status.getStatus());
    }

    /**
     * Testing the GcpContext checking mechanism. If the inner created
     * GcpContext does not contains a valid Compute instance then a FAILED
     * status should come back.
     */
    @Test
    public void testForFailedStatusBecauseMissingCompute() throws IOException {
        final AuthenticatedContext authContext = createAuthContext();
        when(contextBuilder.contextInit(authContext.getCloudContext(), authContext, null, null, false)).thenReturn(context);
        doThrow(new NullPointerException()).when(gcpCredentialVerifier).preCheckOfGooglePermission(context);

        CloudCredentialStatus status = underTest.verify(authContext);

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
        CloudCredentialStatus status = underTest.delete(createAuthContext());

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
        CloudCredentialStatus status = underTest.create(createAuthContext());

        Assert.assertNotNull("The returned CloudCredentialStatus instance is null!", status);
        Assert.assertEquals("Invalid credential status has specified!", CredentialStatus.CREATED, status.getStatus());
    }

    private AuthenticatedContext createAuthContext() {
        return new AuthenticatedContext(createCloudContext(), createCloudCredential());
    }

    private CloudContext createCloudContext() {
        return new CloudContext(1L, "name", "platform", USER_ID, WORKSPACE_ID);
    }

    private CloudCredential createCloudCredential() {
        return new CloudCredential("crn", "name", CREDENTIAL_PARAMETERS, false);
    }
}
