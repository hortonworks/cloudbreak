package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.doNothing;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.common.SssdConfigCommands;
import com.sequenceiq.cloudbreak.shell.completion.SssdProviderType;
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType;
import com.sequenceiq.cloudbreak.shell.completion.SssdTlsReqcertType;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;

public class SssdConfigCommandsTest {
    private static final Long CONFIG_ID = 50L;
    private static final String CONFIG_NAME = "dummyName";

    @InjectMocks
    private SssdConfigCommands underTest;

    @Mock
    private SssdConfigEndpoint sssdConfigEndpoint;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private ShellContext mockContext;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Mock
    private File mockFile;

    @Mock
    private OutputTransformer outputTransformer;

    private SssdConfigResponse dummyResult;

    private File dummyFile;

    @Before
    public void setUp() throws Exception {
        underTest = new SssdConfigCommands(mockContext);
        MockitoAnnotations.initMocks(this);
        dummyResult = new SssdConfigResponse();
        dummyResult.setId(CONFIG_ID);
        dummyResult.setName(CONFIG_NAME);
        dummyResult.setProviderType(com.sequenceiq.cloudbreak.api.model.SssdProviderType.LDAP);
        dummyResult.setSchema(com.sequenceiq.cloudbreak.api.model.SssdSchemaType.RFC2307);
        dummyResult.setTlsReqcert(com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType.NEVER);
        String classPackage = getClass().getPackage().getName().replaceAll("\\.", "/");
        Resource resource = new ClassPathResource(classPackage + "/" + getClass().getSimpleName() + ".class");
        dummyFile = resource.getFile();
        given(cloudbreakClient.sssdConfigEndpoint()).willReturn(sssdConfigEndpoint);
        given(sssdConfigEndpoint.postPrivate(any(SssdConfigRequest.class))).willReturn(new IdJson(1L));
        given(sssdConfigEndpoint.postPublic(any(SssdConfigRequest.class))).willReturn(new IdJson(1L));
        given(sssdConfigEndpoint.get(anyLong())).willReturn(dummyResult);
        given(sssdConfigEndpoint.getPublic(anyString())).willReturn(dummyResult);
        given(mockContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(RuntimeException.class);
        given(mockContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
    }

    @Test
    public void testSelectWithId() throws Exception {
        underTest.select(CONFIG_ID, null);
        verify(sssdConfigEndpoint, times(1)).get(anyLong());
        verify(sssdConfigEndpoint, times(0)).getPublic(anyString());
        verify(mockContext, times(1)).addSssdConfig(anyString());
    }

    @Test
    public void testSelectWithName() throws Exception {
        underTest.select(null, CONFIG_NAME);
        verify(sssdConfigEndpoint, times(0)).get(anyLong());
        verify(sssdConfigEndpoint, times(1)).getPublic(anyString());
        verify(mockContext, times(1)).addSssdConfig(anyString());
    }

    @Test
    public void testSelectWithoutIdorName() throws Exception {
        underTest.select(null, null);
        verify(sssdConfigEndpoint, times(0)).get(anyLong());
        verify(sssdConfigEndpoint, times(0)).getPublic(anyString());
        verify(mockContext, times(0)).addSssdConfig(anyString());
    }

    @Test
    public void testPublicAdd() {
        underTest.create("name", "desc", new SssdProviderType("LDAP"), "url", new SssdSchemaType("RFC2307"), "base", new SssdTlsReqcertType("NEVER"), null, null,
                null, true);
        verify(sssdConfigEndpoint, times(1)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(0)).postPrivate(any(SssdConfigRequest.class));
    }

    @Test
    public void testPrivateAdd() {
        underTest.create("name", "desc", new SssdProviderType("LDAP"), "url", new SssdSchemaType("RFC2307"), "base", new SssdTlsReqcertType("NEVER"), null, null,
                null, false);
        verify(sssdConfigEndpoint, times(0)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(1)).postPrivate(any(SssdConfigRequest.class));
    }

    @Test
    public void testUploadFileNotFound() {
        given(mockFile.exists()).willReturn(false);
        underTest.upload("name", "desc", mockFile, true);
        verify(mockFile, times(1)).exists();
        verify(sssdConfigEndpoint, times(0)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(0)).postPrivate(any(SssdConfigRequest.class));

    }

    @Test
    public void testPublicUpload() throws IOException {
        underTest.upload("name", "desc", dummyFile, true);
        verify(sssdConfigEndpoint, times(1)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(0)).postPrivate(any(SssdConfigRequest.class));
    }

    @Test
    public void testPrivateUpload() {
        given(mockFile.exists()).willReturn(true);
        underTest.upload("name", "desc", dummyFile, false);
        verify(sssdConfigEndpoint, times(0)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(1)).postPrivate(any(SssdConfigRequest.class));
    }

    @Test
    public void testShowSssdConfigById() throws Exception {
        underTest.show(CONFIG_ID, null);
        verify(sssdConfigEndpoint, times(1)).get(anyLong());
        verify(sssdConfigEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testShowSssdConfigByName() throws Exception {
        underTest.show(null, CONFIG_NAME);
        verify(sssdConfigEndpoint, times(0)).get(anyLong());
        verify(sssdConfigEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowSssdConfigWithoutIdAndName() throws Exception {
        underTest.show(null, null);
        verify(sssdConfigEndpoint, times(0)).get(anyLong());
        verify(sssdConfigEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testDeleteSssdConfigById() throws Exception {
        doNothing().when(sssdConfigEndpoint).delete(Long.valueOf(CONFIG_ID));
        underTest.delete(CONFIG_ID, null);
        verify(sssdConfigEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteSssdConfigByName() throws Exception {
        doNothing().when(sssdConfigEndpoint).deletePublic(CONFIG_NAME);
        underTest.delete(null, CONFIG_NAME);
        verify(sssdConfigEndpoint, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteSssdConfigByIdAndName() throws Exception {
        doNothing().when(sssdConfigEndpoint).delete(Long.valueOf(CONFIG_ID));
        underTest.delete(CONFIG_ID, CONFIG_NAME);
        verify(sssdConfigEndpoint, times(0)).deletePublic(anyString());
        verify(sssdConfigEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteSssdConfigWithoutIdAndName() throws Exception {
        underTest.delete(null, null);
        verify(sssdConfigEndpoint, times(0)).deletePublic(anyString());
        verify(sssdConfigEndpoint, times(0)).delete(anyLong());
    }
}
