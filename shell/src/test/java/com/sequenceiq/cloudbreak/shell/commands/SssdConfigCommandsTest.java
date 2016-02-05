package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;

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
    private CloudbreakContext mockContext;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    private SssdConfigResponse dummyResult;

    @Before
    public void setUp() throws Exception {
        underTest = new SssdConfigCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new SssdConfigResponse();
        dummyResult.setId(CONFIG_ID);
        dummyResult.setName(CONFIG_NAME);
        given(cloudbreakClient.sssdConfigEndpoint()).willReturn(sssdConfigEndpoint);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(RuntimeException.class);
    }

    @Test
    public void testSelectWithId() throws Exception {
        given(sssdConfigEndpoint.get(anyLong())).willReturn(dummyResult);
        underTest.select(CONFIG_ID.toString(), null);
        verify(sssdConfigEndpoint, times(1)).get(anyLong());
        verify(sssdConfigEndpoint, times(0)).getPublic(anyString());
        verify(mockContext, times(1)).addSssdConfig(anyString());
    }

    @Test
    public void testSelectWithName() throws Exception {
        given(sssdConfigEndpoint.getPublic(anyString())).willReturn(dummyResult);
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
        given(sssdConfigEndpoint.postPublic(any(SssdConfigRequest.class))).willReturn(new IdJson(1L));
        underTest.add("name", "desc", "ldap", "url", "rfc2307", "base", true);
        verify(sssdConfigEndpoint, times(1)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(0)).postPrivate(any(SssdConfigRequest.class));
    }

    @Test
    public void testPrivateAdd() {
        given(sssdConfigEndpoint.postPrivate(any(SssdConfigRequest.class))).willReturn(new IdJson(1L));
        underTest.add("name", "desc", "ldap", "url", "rfc2307", "base", false);
        verify(sssdConfigEndpoint, times(0)).postPublic(any(SssdConfigRequest.class));
        verify(sssdConfigEndpoint, times(1)).postPrivate(any(SssdConfigRequest.class));
    }

    @Test
    public void testShowSssdConfigById() throws Exception {
        given(sssdConfigEndpoint.get(CONFIG_ID)).willReturn(dummyResult);
        underTest.show(CONFIG_ID.toString(), null);
        verify(sssdConfigEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testShowSssdConfigByName() throws Exception {
        given(sssdConfigEndpoint.getPublic(CONFIG_NAME)).willReturn(dummyResult);
        given(sssdConfigEndpoint.get(CONFIG_ID)).willReturn(dummyResult);
        underTest.show(null, CONFIG_NAME);
        verify(sssdConfigEndpoint, times(0)).get(anyLong());
        verify(sssdConfigEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowSssdConfigWithoutIdAndName() throws Exception {
        underTest.show(null, null);
        verify(sssdConfigEndpoint, times(0)).get(anyLong());
    }

    @Test
    public void testDeleteSssdConfigById() throws Exception {
        doNothing().when(sssdConfigEndpoint).delete(Long.valueOf(CONFIG_ID));
        underTest.delete(CONFIG_ID.toString(), null);
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
        underTest.delete(CONFIG_ID.toString(), CONFIG_NAME);
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
