package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.anyString;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint;
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

    @Before
    public void setUp() throws Exception {
        underTest = new BaseCredentialCommands(context);
        MockitoAnnotations.initMocks(this);
        dummyResult = new CredentialResponse();
        dummyResult.setId(Long.valueOf(DUMMY_ID));
        given(cloudbreakClient.credentialEndpoint()).willReturn(credentialEndpoint);
        given(context.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
        given(context.cloudbreakClient()).willReturn(cloudbreakClient);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(RuntimeException.class);
    }

    @Test
    public void testSelectCredentialById() throws Exception {
        given(credentialEndpoint.get(Long.valueOf(DUMMY_ID))).willReturn(dummyResult);
        underTest.select(DUMMY_ID, null);
        verify(context, times(1)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialByName() throws Exception {
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(dummyResult);
        underTest.select(null, DUMMY_NAME);
        verify(context, times(1)).setCredential(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testSelectCredentialByNameNotFound() throws Exception {
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(null);
        underTest.select(null, DUMMY_NAME);
        verify(context, times(0)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialWithoutIdAndName() throws Exception {
        underTest.select(null, null);
        verify(context, times(0)).setCredential(anyString());
    }

    @Test
    public void testShowCredentialById() throws Exception {
        given(credentialEndpoint.get(Long.valueOf(DUMMY_ID))).willReturn(dummyResult);
        underTest.show(DUMMY_ID, null);
        verify(credentialEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testShowCredentialByName() throws Exception {
        given(credentialEndpoint.get(Long.valueOf(DUMMY_ID))).willReturn(dummyResult);
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(dummyResult);
        underTest.show(null, DUMMY_NAME);
        verify(credentialEndpoint, times(0)).get(anyLong());
        verify(credentialEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowCredentialByNameNotFound() throws Exception {
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(null);
        underTest.show(null, DUMMY_NAME);
        verify(credentialEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowCredentialWithoutIdAndName() throws Exception {
        underTest.show(null, null);
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
        underTest.delete(null, null);
        verify(credentialEndpoint, times(0)).delete(anyLong());
        verify(credentialEndpoint, times(0)).deletePublic(anyString());
    }

    private String getAbsolutePath(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(path).getFile());
        return file.getAbsolutePath();
    }


}
