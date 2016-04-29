package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.common.InstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;

public class InstanceGroupCommandsTest {

    private static final Integer DUMMY_NODE_COUNT = 1;
    private static final String DUMMY_TEMPLATE = "dummy-template";
    private static final String DUMMY_TEMPLATE_ID = "50";
    private InstanceGroup hostGroup = new InstanceGroup("master");
    private InstanceGroupTemplateId dummyTemplateId = new InstanceGroupTemplateId(DUMMY_TEMPLATE_ID);
    private InstanceGroupTemplateName dummyTemplateName = new InstanceGroupTemplateName(DUMMY_TEMPLATE);

    @InjectMocks
    private InstanceGroupCommands underTest;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private TemplateEndpoint mockClient;

    @Mock
    private ShellContext mockContext;

    @Mock
    private OutputTransformer outputTransformer;

    private TemplateResponse dummyResult;

    @Before
    public void setUp() throws Exception {
        underTest = new InstanceGroupCommands(mockContext);
        hostGroup = new InstanceGroup("master");
        MockitoAnnotations.initMocks(this);
        dummyResult = new TemplateResponse();
        dummyResult.setId(Long.valueOf(DUMMY_TEMPLATE_ID));
        given(mockContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.templateEndpoint()).willReturn(mockClient);
        given(mockContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
    }

    @Test
    public void testConfigureByTemplateId() throws Exception {
        underTest.create(hostGroup, DUMMY_NODE_COUNT, dummyTemplateId, null);
        verify(mockContext, times(1)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
    }

    @Test
    public void testConfigureByTemplateName() throws Exception {
        given(mockClient.getPublic(DUMMY_TEMPLATE)).willReturn(dummyResult);
        underTest.create(hostGroup, DUMMY_NODE_COUNT, null, dummyTemplateName);
        verify(mockClient, times(1)).getPublic(anyString());
        verify(mockContext, times(1)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
    }

    @Test
    public void testConfigureByTemplateIdAndName() throws Exception {
        underTest.create(hostGroup, DUMMY_NODE_COUNT, dummyTemplateId, dummyTemplateName);
        verify(mockContext, times(1)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
        verify(mockClient, times(0)).getPublic(anyString());
    }

    @Test
    public void testConfigureByTemplateNameWhenTemplateNotFound() throws Exception {
        given(mockClient.getPublic(DUMMY_TEMPLATE)).willReturn(null);
        underTest.create(hostGroup, DUMMY_NODE_COUNT, null, dummyTemplateName);
        verify(mockClient, times(1)).getPublic(anyString());
        verify(mockContext, times(0)).putInstanceGroup(anyString(), any(InstanceGroupEntry.class));
    }
}
