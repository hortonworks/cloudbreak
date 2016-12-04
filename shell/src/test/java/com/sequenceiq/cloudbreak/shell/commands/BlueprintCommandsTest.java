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

import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.common.BlueprintCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;

public class BlueprintCommandsTest {
    private static final Long BLUEPRINT_ID = 50L;

    private static final String BLUEPRINT_NAME = "dummyName";

    @InjectMocks
    private BlueprintCommands underTest;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private BlueprintEndpoint blueprintEndpoint;

    @Mock
    private ShellContext mockContext;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    private BlueprintResponse dummyResult;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new BlueprintCommands(mockContext);

        dummyResult = new BlueprintResponse();
        dummyResult.setId(BLUEPRINT_ID);
        given(mockContext.isMarathonMode()).willReturn(false);
        given(mockContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.blueprintEndpoint()).willReturn(blueprintEndpoint);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(new RuntimeException());
    }

    @Test
    public void testSelectBlueprintById() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.select(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(1)).get(anyLong());
        verify(mockContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintByIdAndName() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.select(BLUEPRINT_ID, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).get(anyLong());
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(mockContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintByName() throws Exception {
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult);
        underTest.select(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).getPublic(anyString());
        verify(mockContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintWithoutIdAndName() throws Exception {
        underTest.select(null, null);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testSelectBlueprintByNameNotFound() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(null);
        underTest.select(BLUEPRINT_ID, null);
        verify(mockContext, times(0)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test(expected = RuntimeException.class)
    public void testShowBlueprintById() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.show(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(1)).get(anyLong());
    }

    @Test(expected = RuntimeException.class)
    public void testShowBlueprintByName() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult);
        underTest.show(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(0)).get(anyLong());
        verify(blueprintEndpoint, times(1)).getPublic(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testShowBlueprintByIdAndName() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.show(BLUEPRINT_ID, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testDeleteBlueprintById() throws Exception {
        doNothing().when(blueprintEndpoint).deletePublic(BLUEPRINT_ID.toString());
        underTest.delete(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteBlueprintByName() throws Exception {
        doNothing().when(blueprintEndpoint).deletePublic(BLUEPRINT_NAME);
        underTest.delete(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).deletePublic(anyString());
    }

}
