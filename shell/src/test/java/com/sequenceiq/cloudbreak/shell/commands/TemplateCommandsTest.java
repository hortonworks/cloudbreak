package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;

public class TemplateCommandsTest {
    private static final String TEMPLATE_ID = "50";
    private static final String TEMPLATE_NAME = "dummyName";

    @InjectMocks
    private TemplateCommands underTest;

    @Mock
    private TemplateEndpoint templateEndpoint;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    private TemplateResponse dummyResult;

    @Before
    public void setUp() throws Exception {
        underTest = new TemplateCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new TemplateResponse();
        dummyResult.setId(Long.valueOf(TEMPLATE_ID));
        given(cloudbreakClient.templateEndpoint()).willReturn(templateEndpoint);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(RuntimeException.class);
    }

    @Test(expected = RuntimeException.class)
    public void testShowTemplateById() throws Exception {
        given(templateEndpoint.get(Long.valueOf(TEMPLATE_ID))).willReturn(dummyResult);
        underTest.showTemplate(TEMPLATE_ID, null);
        verify(templateEndpoint, times(1)).get(anyLong());
    }

    @Test(expected = RuntimeException.class)
    public void testShowTemplateByName() throws Exception {
        given(templateEndpoint.getPublic(TEMPLATE_NAME)).willReturn(dummyResult);
        given(templateEndpoint.get(Long.valueOf(TEMPLATE_ID))).willReturn(dummyResult);
        underTest.showTemplate(null, TEMPLATE_NAME);
        verify(templateEndpoint, times(0)).get(anyLong());
        verify(templateEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowTemplateByNameNotFound() throws Exception {
        given(templateEndpoint.getPublic(TEMPLATE_NAME)).willReturn(null);
        underTest.showTemplate(null, TEMPLATE_NAME);
        verify(templateEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testDeleteTemplateById() throws Exception {
        underTest.deleteTemplate(TEMPLATE_ID, null);
        verify(templateEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteTemplateByName() throws Exception {
        underTest.deleteTemplate(null, TEMPLATE_NAME);
        verify(templateEndpoint, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteTemplateWithoutIdAndName() throws Exception {
        underTest.deleteTemplate(null, null);
        verify(templateEndpoint, times(0)).delete(anyLong());
        verify(templateEndpoint, times(0)).deletePublic(anyString());
    }
}
