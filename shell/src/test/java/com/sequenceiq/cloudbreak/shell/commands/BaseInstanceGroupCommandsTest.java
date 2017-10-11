package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v1.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseInstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;

public class BaseInstanceGroupCommandsTest {

    private static final Integer DUMMY_NODE_COUNT = 1;

    private static final String DUMMY_TEMPLATE = "dummy-template";

    private static final String DUMMY_TEMPLATE_ID = "50";

    private InstanceGroup hostGroup = new InstanceGroup("master");

    private final InstanceGroupTemplateId dummyTemplateId = new InstanceGroupTemplateId(DUMMY_TEMPLATE_ID);

    private final InstanceGroupTemplateName dummyTemplateName = new InstanceGroupTemplateName(DUMMY_TEMPLATE);

    private final SecurityGroupId dummySecurityGroupId = new SecurityGroupId("1");

    private final Map<String, Object> params = new HashMap<>();

    @InjectMocks
    private BaseInstanceGroupCommands underTest;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private TemplateEndpoint mockClient;

    @Mock
    private ShellContext mockContext;

    @Mock
    private OutputTransformer outputTransformer;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    private TemplateResponse dummyResult;

    private final RuntimeException expectedException = new RuntimeException("something not found");

    @Before
    public void setUp() throws Exception {
        underTest = new BaseInstanceGroupCommands(mockContext);
        hostGroup = new InstanceGroup("master");
        MockitoAnnotations.initMocks(this);
        dummyResult = new TemplateResponse();
        dummyResult.setId(Long.valueOf(DUMMY_TEMPLATE_ID));
        given(mockContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.templateEndpoint()).willReturn(mockClient);
        given(mockContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(anyString())).willThrow(expectedException);
        given(mockContext.exceptionTransformer()).willReturn(exceptionTransformer);
    }

    @Test
    public void testConfigureByTemplateId() throws Exception {
        underTest.create(hostGroup, DUMMY_NODE_COUNT, false, dummyTemplateId, null, dummySecurityGroupId, null, params);
        verify(mockContext, times(1)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
    }

    @Test
    public void testConfigureByTemplateName() throws Exception {
        given(mockClient.getPublic(DUMMY_TEMPLATE)).willReturn(dummyResult);
        underTest.create(hostGroup, DUMMY_NODE_COUNT, false, null, dummyTemplateName, dummySecurityGroupId, null, params);
        verify(mockClient, times(1)).getPublic(anyString());
        verify(mockContext, times(1)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
    }

    @Test
    public void testConfigureByTemplateIdAndName() throws Exception {
        underTest.create(hostGroup, DUMMY_NODE_COUNT, false, dummyTemplateId, dummyTemplateName, dummySecurityGroupId, null, params);
        verify(mockContext, times(1)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
        verify(mockClient, times(0)).getPublic(anyString());
    }

    @Test
    public void testConfigureByTemplateNameWhenTemplateNotFound() throws Exception {
        given(mockClient.getPublic(DUMMY_TEMPLATE)).willReturn(null);
        RuntimeException ext = null;
        try {
            underTest.create(hostGroup, DUMMY_NODE_COUNT, false, null, dummyTemplateName, dummySecurityGroupId, null, params);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(mockClient, times(1)).getPublic(anyString());
        verify(mockContext, times(0)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
    }
}
