package com.sequenceiq.cloudbreak.shell.commands

import org.hamcrest.Matchers.containsString
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyMap
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyVararg

import javax.ws.rs.NotFoundException

import org.apache.http.MethodNotSupportedException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.base.BaseTemplateCommands
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

class BaseTemplateCommandsTest {
    @InjectMocks
    private var underTest: BaseTemplateCommands? = null

    @Mock
    private val shellContext: ShellContext? = null
    @Mock
    private val cloudbreakClient: CloudbreakClient? = null
    @Mock
    private val templateEndpoint: TemplateEndpoint? = null
    @Mock
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null
    @Mock
    private val outputTransformer: OutputTransformer? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        underTest = BaseTemplateCommands(shellContext)

        given(shellContext!!.cloudbreakClient()).willReturn(cloudbreakClient)
        given(cloudbreakClient!!.templateEndpoint()).willReturn(templateEndpoint)
        given(shellContext.responseTransformer()).willReturn(responseTransformer)
        given(shellContext.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
    }

    @Test(expected = MethodNotSupportedException::class)
    @Throws(Exception::class)
    fun selectTemplateByIdDropException() {
        underTest!!.select(50L, null)
    }

    @Test
    @Throws(Exception::class)
    fun showTemplateByIdWhichIsExist() {
        given(templateEndpoint!![anyLong()]).willReturn(templateResponse())
        given(responseTransformer!!.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"))

        val show = underTest!!.show(50L, null)

        Assert.assertThat(show, containsString("id"))
        Assert.assertThat(show, containsString("name"))
        Mockito.verify(responseTransformer, Mockito.times(1)).transformObjectToStringMap(anyObject<Any>(), Matchers.anyVararg<String>())
        Mockito.verify(templateEndpoint, Mockito.times(1)).get(anyLong())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun showTemplateByIdWhichIsNotExist() {
        given(templateEndpoint!![anyLong()]).willThrow(NotFoundException("not found"))

        underTest!!.show(51L, null)
    }

    @Test
    @Throws(Exception::class)
    fun showTemplateByNameWhichIsExist() {
        given(templateEndpoint!!.getPublic(Matchers.anyString())).willReturn(templateResponse())
        given(responseTransformer!!.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"))

        val show = underTest!!.show(null, "test1")

        Assert.assertThat(show, containsString("id"))
        Assert.assertThat(show, containsString("name"))
        Mockito.verify(responseTransformer, Mockito.times(1)).transformObjectToStringMap(anyObject<Any>(), Matchers.anyVararg<String>())
        Mockito.verify(templateEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun showTemplateByNameWhichIsNotExistThenThowNotFoundException() {
        given(templateEndpoint!!.getPublic(Matchers.anyString())).willThrow(NotFoundException("not found"))

        underTest!!.show(null, "test1")
    }

    private fun templateResponse(): TemplateResponse {
        val templateResponse = TemplateResponse()
        templateResponse.name = "test1"
        templateResponse.id = 50L
        return templateResponse
    }
}