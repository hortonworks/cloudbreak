package com.sequenceiq.cloudbreak.shell.commands

import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Matchers.any
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyVararg

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.common.InstanceGroupCommands
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer

class InstanceGroupCommandsTest {
    private var hostGroup = InstanceGroup("master")
    private val dummyTemplateId = InstanceGroupTemplateId(DUMMY_TEMPLATE_ID)
    private val dummyTemplateName = InstanceGroupTemplateName(DUMMY_TEMPLATE)

    @InjectMocks
    private var underTest: InstanceGroupCommands? = null

    @Mock
    private val cloudbreakClient: CloudbreakClient? = null

    @Mock
    private val mockClient: TemplateEndpoint? = null

    @Mock
    private val mockContext: ShellContext? = null

    @Mock
    private val outputTransformer: OutputTransformer? = null

    private var dummyResult: TemplateResponse? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        underTest = InstanceGroupCommands(mockContext)
        hostGroup = InstanceGroup("master")
        MockitoAnnotations.initMocks(this)
        dummyResult = TemplateResponse()
        dummyResult!!.id = java.lang.Long.valueOf(DUMMY_TEMPLATE_ID)
        given(mockContext!!.cloudbreakClient()).willReturn(cloudbreakClient)
        given(cloudbreakClient!!.templateEndpoint()).willReturn(mockClient)
        given(mockContext.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
    }

    @Test
    @Throws(Exception::class)
    fun testConfigureByTemplateId() {
        underTest!!.create(hostGroup, DUMMY_NODE_COUNT, false, dummyTemplateId, null)
        Mockito.verify(mockContext, Mockito.times(1)).putInstanceGroup(Matchers.anyString(), any<InstanceGroupEntry>(InstanceGroupEntry::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testConfigureByTemplateName() {
        given(mockClient!!.getPublic(DUMMY_TEMPLATE)).willReturn(dummyResult)
        underTest!!.create(hostGroup, DUMMY_NODE_COUNT, false, null, dummyTemplateName)
        Mockito.verify(mockClient, Mockito.times(1)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(1)).putInstanceGroup(Matchers.anyString(), any<InstanceGroupEntry>(InstanceGroupEntry::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testConfigureByTemplateIdAndName() {
        underTest!!.create(hostGroup, DUMMY_NODE_COUNT, false, dummyTemplateId, dummyTemplateName)
        Mockito.verify(mockContext, Mockito.times(1)).putInstanceGroup(Matchers.anyString(), any<InstanceGroupEntry>(InstanceGroupEntry::class.java))
        Mockito.verify(mockClient, Mockito.times(0)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testConfigureByTemplateNameWhenTemplateNotFound() {
        given(mockClient!!.getPublic(DUMMY_TEMPLATE)).willReturn(null)
        underTest!!.create(hostGroup, DUMMY_NODE_COUNT, false, null, dummyTemplateName)
        Mockito.verify(mockClient, Mockito.times(1)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(0)).putInstanceGroup(Matchers.anyString(), any<InstanceGroupEntry>(InstanceGroupEntry::class.java))
    }

    companion object {

        private val DUMMY_NODE_COUNT = 1
        private val DUMMY_TEMPLATE = "dummy-template"
        private val DUMMY_TEMPLATE_ID = "50"
    }
}
