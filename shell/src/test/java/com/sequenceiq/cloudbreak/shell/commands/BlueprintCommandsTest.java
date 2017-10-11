package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.common.BlueprintCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class BlueprintCommandsTest {

    private static final Long BLUEPRINT_ID = 50L;

    private static final String BLUEPRINT_NAME = "dummyName";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BlueprintCommands underTest;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private BlueprintEndpoint blueprintEndpoint;

    @Mock
    private ShellContext shellContext;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Mock
    private OutputTransformer outputTransformer;

    @Mock
    private ResponseTransformer responseTransformer;

    private BlueprintResponse dummyResult;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new BlueprintCommands(shellContext);

        dummyResult = new BlueprintResponse();
        dummyResult.setId(BLUEPRINT_ID);
        dummyResult.setAmbariBlueprint("");
        given(shellContext.isMarathonMode()).willReturn(false);
        given(shellContext.isYarnMode()).willReturn(false);
        given(shellContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.blueprintEndpoint()).willReturn(blueprintEndpoint);
        given(shellContext.exceptionTransformer()).willReturn(exceptionTransformer);
        given(shellContext.outputTransformer()).willReturn(outputTransformer);
        given(shellContext.responseTransformer()).willReturn(responseTransformer);
        given(shellContext.objectMapper()).willReturn(new ObjectMapper());
    }

    @Test
    public void testSelectBlueprintById() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.select(BLUEPRINT_ID, null);
        verify(blueprintEndpoint, times(1)).get(anyLong());
        verify(shellContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintByIdAndName() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.select(BLUEPRINT_ID, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).get(anyLong());
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(shellContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testSelectBlueprintByName() throws Exception {
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult);
        underTest.select(null, BLUEPRINT_NAME);
        verify(blueprintEndpoint, times(1)).getPublic(anyString());
        verify(shellContext, times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
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
        verify(shellContext, times(0)).setHint(Hints.CONFIGURE_INSTANCEGROUP);
    }

    @Test
    public void testShowBlueprintById() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        given(outputTransformer.render(any(), any())).willReturn("");
        given(responseTransformer.transformObjectToStringMap(any(), any())).willReturn(Collections.emptyMap());
        underTest.show(BLUEPRINT_ID, null, null);
        verify(blueprintEndpoint, times(0)).getPublic(anyString());
        verify(blueprintEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testShowBlueprintByName() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult);
        underTest.show(null, BLUEPRINT_NAME, null);
        verify(blueprintEndpoint, times(0)).get(anyLong());
        verify(blueprintEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowBlueprintByIdAndName() throws Exception {
        given(blueprintEndpoint.get(BLUEPRINT_ID)).willReturn(dummyResult);
        underTest.show(BLUEPRINT_ID, BLUEPRINT_NAME, null);
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
