package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.anyString;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;

public class CredentialCommandsTest {

    private static final String DUMMY_DESCRIPTION = "dummyDescription";
    private static final String DUMMY_NAME = "dummyName";
    private static final String DUMMY_ID = "60";
    private static final String DUMMY_SUBSCRIPTION_ID = "dummySubscriptionId";
    private static final String DUMMY_SSH_KEY_PATH = "dummy";

    @InjectMocks
    private CredentialCommands underTest;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private CloudbreakContext context;
    @Mock
    private CredentialEndpoint cloudbreak;

    private CredentialResponse dummyResult;

    @Before
    public void setUp() {
        underTest = new CredentialCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new CredentialResponse();
        dummyResult.setId(Long.valueOf(DUMMY_ID));
    }

    @Test
    public void testSelectCredentialById() throws Exception {
        given(cloudbreak.get(Long.valueOf(DUMMY_ID))).willReturn(dummyResult);
        underTest.selectCredential(DUMMY_ID, null);
        verify(context, times(1)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialByName() throws Exception {
        given(cloudbreak.getPublic(DUMMY_NAME)).willReturn(dummyResult);
        underTest.selectCredential(null, DUMMY_NAME);
        verify(context, times(1)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialByNameNotFound() throws Exception {
        given(cloudbreak.getPublic(DUMMY_NAME)).willReturn(null);
        underTest.selectCredential(null, DUMMY_NAME);
        verify(context, times(0)).setCredential(anyString());
    }

    @Test
    public void testSelectCredentialWithoutIdAndName() throws Exception {
        underTest.selectCredential(null, null);
        verify(context, times(0)).setCredential(anyString());
    }

    @Test
    public void testShowCredentialById() throws Exception {
        given(cloudbreak.get(Long.valueOf(DUMMY_ID))).willReturn(dummyResult);
        underTest.showCredential(DUMMY_ID, null);
        verify(cloudbreak, times(1)).get(anyLong());
    }

    @Test
    public void testShowCredentialByName() throws Exception {
        given(cloudbreak.get(Long.valueOf(DUMMY_ID))).willReturn(dummyResult);
        given(cloudbreak.getPublic(DUMMY_NAME)).willReturn(dummyResult);
        underTest.showCredential(null, DUMMY_NAME);
        verify(cloudbreak, times(0)).get(anyLong());
        verify(cloudbreak, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowCredentialByNameNotFound() throws Exception {
        given(cloudbreak.getPublic(DUMMY_NAME)).willReturn(null);
        underTest.showCredential(null, DUMMY_NAME);
        verify(cloudbreak, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowCredentialWithoutIdAndName() throws Exception {
        underTest.showCredential(null, null);
        verify(cloudbreak, times(0)).get(anyLong());
    }

    @Test
    public void testDeleteCredentialById() throws Exception {
        underTest.deleteCredential(DUMMY_ID, null);
        verify(cloudbreak, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteCredentialByName() throws Exception {
        underTest.deleteCredential(null, DUMMY_NAME);
        verify(cloudbreak, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteCredentialWithoutIdAndName() throws Exception {
        underTest.deleteCredential(null, null);
        verify(cloudbreak, times(0)).delete(anyLong());
        verify(cloudbreak, times(0)).deletePublic(anyString());
    }

    private String getAbsolutePath(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(path).getFile());
        return file.getAbsolutePath();
    }


}
