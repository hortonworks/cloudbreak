package com.sequenceiq.cloudbreak.shell.commands

import org.hamcrest.Matchers.containsString
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyMap
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyString
import org.mockito.Matchers.anyVararg
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import javax.ws.rs.NotFoundException

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.base.BaseStackCommands
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil

class BaseStackCommandsTest {
    @InjectMocks
    private var underTest: BaseStackCommands? = null

    @Mock
    private val shellContext: ShellContext? = null
    @Mock
    private val cloudbreakClient: CloudbreakClient? = null
    @Mock
    private val stackEndpoint: StackEndpoint? = null
    @Mock
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null
    @Mock
    private val cloudbreakShellUtil: CloudbreakShellUtil? = null
    @Mock
    private val exceptionTransformer: ExceptionTransformer? = null
    @Mock
    private val outputTransformer: OutputTransformer? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        underTest = BaseStackCommands(shellContext, cloudbreakShellUtil)

        given(shellContext!!.cloudbreakClient()).willReturn(cloudbreakClient)
        given(cloudbreakClient!!.stackEndpoint()).willReturn(stackEndpoint)
        given(shellContext.responseTransformer()).willReturn(responseTransformer)
        given(shellContext.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
    }

    @Test
    fun selectStackByIdWhichIsExist() {
        given(stackEndpoint!![anyLong()]).willReturn(stackResponse())

        val select = underTest!!.select(50L, null)

        Assert.assertEquals(select, "Stack selected, id: 50")
    }

    @Test(expected = RuntimeException::class)
    fun selectStackByIdWhichIsNotExist() {
        given(stackEndpoint!![anyLong()]).willThrow(NotFoundException("not found"))
        given(shellContext!!.exceptionTransformer()).willReturn(exceptionTransformer)
        given(exceptionTransformer!!.transformToRuntimeException(any<Exception>(Exception::class.java))).willReturn(RuntimeException("not found"))

        underTest!!.select(51L, null)
    }

    @Test
    fun selectStackByNameWhichIsExist() {
        given(stackEndpoint!!.getPublic(anyString())).willReturn(stackResponse())

        val select = underTest!!.select(null, "test1")

        Assert.assertEquals(select, "Stack selected, name: test1")
    }

    @Test(expected = RuntimeException::class)
    fun selectStackByNameWhichIsNotExistThenThowNotFoundException() {
        given(stackEndpoint!!.getPublic(anyString())).willThrow(NotFoundException("not found"))

        underTest!!.select(null, "test1")
    }

    @Test
    @Throws(Exception::class)
    fun showStackByIdWhichIsExist() {
        given(stackEndpoint!![anyLong()]).willReturn(stackResponse())
        given(responseTransformer!!.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"))

        val show = underTest!!.show(50L, null)

        Assert.assertThat(show, containsString("id"))
        Assert.assertThat(show, containsString("name"))
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject<Any>(), Matchers.anyVararg<String>())
        verify(stackEndpoint, times(1))[anyLong()]
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun showStackByIdWhichIsNotExist() {
        given(stackEndpoint!![anyLong()]).willThrow(NotFoundException("not found"))

        underTest!!.show(51L, null)
    }

    @Test
    @Throws(Exception::class)
    fun showStackByNameWhichIsExist() {
        given(stackEndpoint!!.getPublic(anyString())).willReturn(stackResponse())
        given(responseTransformer!!.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"))

        val show = underTest!!.show(null, "test1")

        Assert.assertThat(show, containsString("id"))
        Assert.assertThat(show, containsString("name"))
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject<Any>(), Matchers.anyVararg<String>())
        verify(stackEndpoint, times(1)).getPublic(anyString())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun showStackByNameWhichIsNotExistThenThowNotFoundException() {
        given(stackEndpoint!!.getPublic(anyString())).willThrow(NotFoundException("not found"))

        underTest!!.show(null, "test1")
    }


    private fun stackResponse(): StackResponse {
        val stackResponse = StackResponse()
        stackResponse.name = "test1"
        stackResponse.id = 50L
        return stackResponse
    }
}
