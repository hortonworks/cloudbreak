package com.sequenceiq.cloudbreak.shell.commands;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.ws.rs.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseStackCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;

public class BaseStackCommandsTest {
    @InjectMocks
    private BaseStackCommands underTest;

    @Mock
    private ShellContext shellContext;
    @Mock
    private CloudbreakClient cloudbreakClient;
    @Mock
    private StackEndpoint stackEndpoint;
    @Mock
    private ResponseTransformer responseTransformer;
    @Mock
    private CloudbreakShellUtil cloudbreakShellUtil;
    @Mock
    private ExceptionTransformer exceptionTransformer;
    @Mock
    private OutputTransformer outputTransformer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new BaseStackCommands(shellContext, cloudbreakShellUtil);

        given(shellContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.stackEndpoint()).willReturn(stackEndpoint);
        given(shellContext.responseTransformer()).willReturn(responseTransformer);
        given(shellContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
    }

    @Test
    public void selectStackByIdWhichIsExist() {
        given(stackEndpoint.get(anyLong())).willReturn(stackResponse());

        String select = underTest.select(50L, null);

        Assert.assertEquals(select, "Stack selected, id: 50");
    }

    @Test(expected = RuntimeException.class)
    public void selectStackByIdWhichIsNotExist() {
        given(stackEndpoint.get(anyLong())).willThrow(new NotFoundException("not found"));
        given(shellContext.exceptionTransformer()).willReturn(exceptionTransformer);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willReturn(new RuntimeException("not found"));

        underTest.select(51L, null);
    }

    @Test
    public void selectStackByNameWhichIsExist() {
        given(stackEndpoint.getPublic(anyString())).willReturn(stackResponse());

        String select = underTest.select(null, "test1");

        Assert.assertEquals(select, "Stack selected, name: test1");
    }

    @Test(expected = RuntimeException.class)
    public void selectStackByNameWhichIsNotExistThenThowNotFoundException() {
        given(stackEndpoint.getPublic(anyString())).willThrow(new NotFoundException("not found"));

        underTest.select(null, "test1");
    }

    @Test
    public void showStackByIdWhichIsExist() {
        given(stackEndpoint.get(anyLong())).willReturn(stackResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(50L, null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(stackEndpoint, times(1)).get(anyLong());
    }

    @Test(expected = RuntimeException.class)
    public void showStackByIdWhichIsNotExist() {
        given(stackEndpoint.get(anyLong())).willThrow(new NotFoundException("not found"));

        underTest.show(51L, null);
    }

    @Test
    public void showStackByNameWhichIsExist() {
        given(stackEndpoint.getPublic(anyString())).willReturn(stackResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(null, "test1");

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(stackEndpoint, times(1)).getPublic(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void showStackByNameWhichIsNotExistThenThowNotFoundException() {
        given(stackEndpoint.getPublic(anyString())).willThrow(new NotFoundException("not found"));

        underTest.show(null, "test1");
    }

    private StackResponse stackResponse() {
        StackResponse stackResponse = new StackResponse();
        stackResponse.setName("test1");
        stackResponse.setId(50L);
        return stackResponse;
    }
}
