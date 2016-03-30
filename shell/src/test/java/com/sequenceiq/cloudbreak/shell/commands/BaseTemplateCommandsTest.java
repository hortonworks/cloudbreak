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

import javax.ws.rs.NotFoundException;

import org.apache.http.MethodNotSupportedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseTemplateCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class BaseTemplateCommandsTest {
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new BaseTemplateCommands(shellContext);

        given(shellContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.templateEndpoint()).willReturn(templateEndpoint);
        given(shellContext.responseTransformer()).willReturn(responseTransformer);
        given(shellContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyObject(), Matchers.<String>anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject(), Matchers.<String>anyVararg())).willReturn("id 1 name test1");
    }

    @Test(expected = MethodNotSupportedException.class)
    public void selectTemplateByIdDropException() throws Exception {
        underTest.select(50L, null);
    }

    @Test
    public void showTemplateByIdWhichIsExist() throws Exception {
        given(templateEndpoint.get(anyLong())).willReturn(templateResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(50L, null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(templateEndpoint, times(1)).get(anyLong());
    }

    @Test(expected = RuntimeException.class)
    public void showTemplateByIdWhichIsNotExist() throws Exception {
        given(templateEndpoint.get(anyLong())).willThrow(new NotFoundException("not found"));

        underTest.show(51L, null);
    }

    @Test
    public void showTemplateByNameWhichIsExist() throws Exception {
        given(templateEndpoint.getPublic(anyString())).willReturn(templateResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(null, "test1");

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(templateEndpoint, times(1)).getPublic(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void showTemplateByNameWhichIsNotExistThenThowNotFoundException() throws Exception {
        given(templateEndpoint.getPublic(anyString())).willThrow(new NotFoundException("not found"));

        underTest.show(null, "test1");
    }

    private TemplateResponse templateResponse() {
        TemplateResponse templateResponse = new TemplateResponse();
        templateResponse.setName("test1");
        templateResponse.setId(50L);
        return templateResponse;
    }
}