package com.sequenceiq.cloudbreak.shell.commands;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
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

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BaseStackCommands underTest;

    @Mock
    private ShellContext shellContext;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private StackV1Endpoint stackV1Endpoint;

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
        given(cloudbreakClient.stackEndpoint()).willReturn(stackV1Endpoint);
        given(shellContext.responseTransformer()).willReturn(responseTransformer);
        given(shellContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(any(OutPutType.class), anyObject(), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
        given(shellContext.exceptionTransformer()).willReturn(exceptionTransformer);
    }

    @Test
    public void selectStackByIdWhichIsExist() {
        given(stackV1Endpoint.get(anyLong(), anySet())).willReturn(stackResponse());

        String select = underTest.select(50L, null);

        Assert.assertEquals("Stack selected, id: 50", select);
    }

    @Test
    public void selectStackByIdWhichIsNotExist() {
        given(stackV1Endpoint.get(anyLong(), anySet())).willThrow(new RuntimeException("not found"));
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willReturn(new RuntimeException("not found"));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("not found");

        underTest.select(51L, null);
    }

    @Test
    public void selectStackByNameWhichIsExist() {
        given(stackV1Endpoint.getPublic(anyString(), anySet())).willReturn(stackResponse());

        String select = underTest.select(null, "test1");

        Assert.assertEquals("Stack selected, name: test1", select);
    }

    @Test
    public void selectStackByNameWhichIsNotExistThenThowNotFoundException() {
        given(stackV1Endpoint.getPublic(anyString(), anySet())).willThrow(new RuntimeException("not found"));
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willReturn(new RuntimeException("not found"));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("not found");

        underTest.select(null, "test1");
    }

    @Test
    public void showStackByIdWhichIsExist() {
        given(stackV1Endpoint.get(anyLong(), anySet())).willReturn(stackResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(50L, null, null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(stackV1Endpoint, times(1)).get(anyLong(), anySet());
    }

    @Test
    public void showStackByIdWhichIsNotExist() {
        given(stackV1Endpoint.get(anyLong(), anySet())).willThrow(new RuntimeException("not found"));
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willReturn(new RuntimeException("not found"));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("not found");

        underTest.show(51L, null, null);
    }

    @Test
    public void showStackByNameWhichIsExist() {
        given(stackV1Endpoint.getPublic(anyString(), anySet())).willReturn(stackResponse());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(null, "test1", null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(stackV1Endpoint, times(1)).getPublic(anyString(), anySet());
    }

    @Test
    public void showStackByNameWhichIsNotExistThenThowNotFoundException() {
        given(stackV1Endpoint.getPublic(anyString(), anySet())).willThrow(new RuntimeException("not found"));
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willReturn(new RuntimeException("not found"));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("not found");

        underTest.show(null, "test1", null);
    }

    private StackResponse stackResponse() {
        StackResponse stackResponse = new StackResponse();
        stackResponse.setName("test1");
        stackResponse.setId(50L);
        stackResponse.setCredentialId(1L);
        stackResponse.setNetworkId(1L);
        return stackResponse;
    }
}
