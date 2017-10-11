package com.sequenceiq.cloudbreak.shell.commands;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;

import org.apache.http.MethodNotSupportedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v1.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseTemplateCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class BaseTemplateCommandsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BaseTemplateCommands underTest;

    @Mock
    private ShellContext shellContext;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private TemplateEndpoint templateEndpoint;

    @Mock
    private ResponseTransformer responseTransformer;

    @Mock
    private OutputTransformer outputTransformer;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new BaseTemplateCommands(shellContext);

        given(shellContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.templateEndpoint()).willReturn(templateEndpoint);
        given(shellContext.responseTransformer()).willReturn(responseTransformer);
        given(shellContext.outputTransformer()).willReturn(outputTransformer);
        given(shellContext.exceptionTransformer()).willReturn(exceptionTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(any(OutPutType.class), anyObject(), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
    }

    @Test(expected = MethodNotSupportedException.class)
    public void selectTemplateByIdDropException() throws Exception {
        underTest.select(50L, null);
    }

    @Test
    public void showTemplateByIdWhichIsExist() throws Exception {
        given(templateEndpoint.get(anyLong())).willReturn(templateResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(50L, null, null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(templateEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void showTemplateByIdWhichIsNotExist() throws Exception {
        given(templateEndpoint.get(anyLong())).willThrow(new RuntimeException("not found"));
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willReturn(new RuntimeException("not found"));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("not found");

        underTest.show(51L, null, null);
    }

    @Test
    public void showTemplateByNameWhichIsExist() throws Exception {
        given(templateEndpoint.getPublic(anyString())).willReturn(templateResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(null, "test1", null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(templateEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void showTemplateByNameWhichIsNotExistThenThowNotFoundException() throws Exception {
        RuntimeException expectedException = new RuntimeException("not found");
        given(templateEndpoint.getPublic(anyString())).willThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException.getMessage()))).willReturn(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(expectedException.getMessage());

        underTest.show(null, "test1", null);
    }

    private TemplateResponse templateResponse() {
        TemplateResponse templateResponse = new TemplateResponse();
        templateResponse.setName("test1");
        templateResponse.setId(50L);
        return templateResponse;
    }
}