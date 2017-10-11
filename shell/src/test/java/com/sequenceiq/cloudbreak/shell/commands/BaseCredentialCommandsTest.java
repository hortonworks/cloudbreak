package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseCredentialCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;

public class BaseCredentialCommandsTest {

    private static final String DUMMY_NAME = "dummyName";

    private static final Long DUMMY_ID = 60L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BaseCredentialCommands underTest;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ShellContext context;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Mock
    private OutputTransformer outputTransformer;

    private CredentialResponse dummyResult;

    private final RuntimeException expectedException = new RuntimeException("something not found");

    @Before
    public void setUp() throws Exception {
        underTest = new BaseCredentialCommands(context);
        MockitoAnnotations.initMocks(this);
        dummyResult = new CredentialResponse();
        dummyResult.setId(DUMMY_ID);
        given(cloudbreakClient.credentialEndpoint()).willReturn(credentialEndpoint);
        given(context.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
        given(context.cloudbreakClient()).willReturn(cloudbreakClient);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(anyString())).willThrow(expectedException);
        given(context.exceptionTransformer()).willReturn(exceptionTransformer);
    }

    @Test
    public void testSelectCredentialById() throws Exception {
        given(credentialEndpoint.get(DUMMY_ID)).willReturn(dummyResult);
        underTest.select(DUMMY_ID, null);
        verify(context, times(1)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialByName() throws Exception {
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(dummyResult);
        underTest.select(null, DUMMY_NAME);
        verify(context, times(1)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialByNameNotFound() throws Exception {
        Throwable exception = new BadRequestException("Credential not found");
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willThrow(exception);
        given(exceptionTransformer.transformToRuntimeException(any(BadRequestException.class))).willThrow(exception);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Credential not found");
        underTest.select(null, DUMMY_NAME);
        verify(context, times(0)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialWithoutIdAndName() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.select(null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(context, times(0)).setCredential(anyString());
    }

    @Test
    public void testShowCredentialById() throws Exception {
        given(credentialEndpoint.get(DUMMY_ID)).willReturn(dummyResult);
        underTest.show(DUMMY_ID, null, null);
        verify(credentialEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testShowCredentialByName() throws Exception {
        given(credentialEndpoint.get(DUMMY_ID)).willReturn(dummyResult);
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(dummyResult);
        underTest.show(null, DUMMY_NAME, null);
        verify(credentialEndpoint, times(0)).get(anyLong());
        verify(credentialEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowCredentialByNameNotFound() throws Exception {
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(null);
        RuntimeException ext = null;
        try {
            underTest.show(null, DUMMY_NAME, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(credentialEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowCredentialWithoutIdAndName() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.show(null, null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(credentialEndpoint, times(0)).get(anyLong());
    }

    @Test
    public void testDeleteCredentialById() throws Exception {
        underTest.delete(DUMMY_ID, null);
        verify(credentialEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteCredentialByName() throws Exception {
        underTest.delete(null, DUMMY_NAME);
        verify(credentialEndpoint, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteCredentialWithoutIdAndName() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.delete(null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(credentialEndpoint, times(0)).delete(anyLong());
        verify(credentialEndpoint, times(0)).deletePublic(anyString());
    }
}
