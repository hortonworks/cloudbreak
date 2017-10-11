package com.sequenceiq.cloudbreak.shell.commands;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.api.endpoint.v1.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseNetworkCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class BaseNetworkCommandsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BaseNetworkCommands underTest;

    @Mock
    private ShellContext shellContext;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private NetworkEndpoint networkEndpoint;

    @Mock
    private ResponseTransformer responseTransformer;

    @Mock
    private OutputTransformer outputTransformer;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new BaseNetworkCommands(shellContext);

        given(shellContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.networkEndpoint()).willReturn(networkEndpoint);
        given(shellContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(any(OutPutType.class), anyObject(), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
        given(shellContext.responseTransformer()).willReturn(responseTransformer);
        given(shellContext.exceptionTransformer()).willReturn(exceptionTransformer);
    }

    @Test
    public void selectNetworkByIdWhichIsExist() {
        given(shellContext.getNetworksByProvider()).willReturn(ImmutableMap.of(50L, "test1"));

        String select = underTest.select(50L, null);

        Assert.assertEquals(select, "Network is selected with id: " + 50L);
    }

    @Test
    public void selectNetworkByIdWhichIsNotExist() {
        given(shellContext.getNetworksByProvider()).willReturn(ImmutableMap.of(50L, "test1"));
        RuntimeException expectedException = new RuntimeException("Network not found");
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException.getMessage()))).willReturn(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(expectedException.getMessage());

        underTest.select(51L, null);
    }

    @Test
    public void selectNetworkByNameWhichIsExist() {
        given(networkEndpoint.getPublic(anyString())).willReturn(networkJson());

        String select = underTest.select(null, "test1");

        Assert.assertEquals("Network is selected with name: test1", select);
    }

    @Test
    public void selectNetworkByNameWhichIsNotExistThenThowNotFoundException() {
        RuntimeException expectedException = new RuntimeException("not found");
        given(networkEndpoint.getPublic(anyString())).willThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(expectedException.getMessage());

        underTest.select(null, "test1");
    }

    @Test
    public void showNetworkByIdWhichIsExist() throws Exception {
        given(networkEndpoint.get(anyLong())).willReturn(networkJson());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(50L, null, null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(networkEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void showNetworkByIdWhichIsNotExist() throws Exception {
        RuntimeException expectedException = new RuntimeException("not exists");
        when(networkEndpoint.get(anyLong())).thenThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(expectedException.getMessage());

        underTest.show(51L, null, null);
    }

    @Test
    public void showNetworkByNameWhichIsExist() throws Exception {
        given(networkEndpoint.getPublic(anyString())).willReturn(networkJson());
        given(responseTransformer.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"));

        String show = underTest.show(null, "test1", null);

        Assert.assertThat(show, containsString("id"));
        Assert.assertThat(show, containsString("name"));
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject(), Matchers.<String>anyVararg());
        verify(networkEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void showNetworkByNameWhichIsNotExistThenThowNotFoundException() throws Exception {
        RuntimeException expectedException = new RuntimeException("not found");
        given(networkEndpoint.getPublic(anyString())).willThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException.getMessage()))).willReturn(expectedException);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(expectedException.getMessage());

        underTest.show(null, "test1", null);
    }

    private NetworkResponse networkJson() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setName("test1");
        networkResponse.setId(50L);
        return networkResponse;
    }
}
