package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
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
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;

public class BlueprintCommandsTest {
    private static final String BLUEPRINT_ID = "50";
    private static final String BLUEPRINT_NAME = "dummyName";

    @InjectMocks
    private BlueprintCommands underTest;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private BlueprintEndpoint blueprintEndpoint;

    @Mock
    private CloudbreakContext mockContext;

    private BlueprintResponse dummyResult;

    @Before
    public void setUp() throws Exception {
        underTest = new BlueprintCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new BlueprintResponse();
        dummyResult.setId(BLUEPRINT_ID);
        given(cloudbreakClient.blueprintEndpoint()).willReturn(blueprintEndpoint);
    }

    @Test
    public void testSelectBlueprintById() throws Exception {
        given(blueprintEndpoint.get(Long.valueOf(BLUEPRINT_ID))).willReturn(dummyResult);
        underTest.selectBlueprint(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(1)).get(anyLong());
        verify(mockContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintByIdAndName() throws Exception {
        given(blueprintEndpoint.get(Long.valueOf(BLUEPRINT_ID))).willReturn(dummyResult);
        underTest.selectBlueprint(BLUEPRINT_ID, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).get(anyLong());
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(mockContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintByName() throws Exception {
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult);
        underTest.selectBlueprint(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).getPublic(anyString());
        verify(mockContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintWithoutIdAndName() throws Exception {
        underTest.selectBlueprint(null, null);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testSelectBlueprintByNameNotFound() throws Exception {
        given(blueprintEndpoint.get(Long.valueOf(BLUEPRINT_ID))).willReturn(null);
        underTest.selectBlueprint(BLUEPRINT_ID, null);
        verify(mockContext, times(0)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testShowBlueprintById() throws Exception {
        given(blueprintEndpoint.get(Long.valueOf(BLUEPRINT_ID))).willReturn(dummyResult);
        underTest.showBlueprint(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testShowBlueprintByName() throws Exception {
        given(blueprintEndpoint.get(Long.valueOf(BLUEPRINT_ID))).willReturn(dummyResult);
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult);
        underTest.showBlueprint(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(0)).get(anyLong());
        verify(blueprintEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowBlueprintByIdAndName() throws Exception {
        given(blueprintEndpoint.get(Long.valueOf(BLUEPRINT_ID))).willReturn(dummyResult);
        underTest.showBlueprint(BLUEPRINT_ID, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testDeleteBlueprintById() throws Exception {
        doNothing().when(blueprintEndpoint).deletePublic(BLUEPRINT_ID);
        underTest.deleteBlueprint(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteBlueprintByName() throws Exception {
        doNothing().when(blueprintEndpoint).deletePublic(BLUEPRINT_NAME);
        underTest.deleteBlueprint(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).deletePublic(anyString());
    }

}
