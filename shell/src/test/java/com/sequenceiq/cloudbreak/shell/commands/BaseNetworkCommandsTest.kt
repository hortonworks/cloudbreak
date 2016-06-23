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
import org.mockito.Mockito.`when`

import javax.ws.rs.NotFoundException

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint
import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.base.BaseNetworkCommands
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

class BaseNetworkCommandsTest {

    @InjectMocks
    private var underTest: BaseNetworkCommands? = null

    @Mock
    private val shellContext: ShellContext? = null
    @Mock
    private val cloudbreakClient: CloudbreakClient? = null
    @Mock
    private val networkEndpoint: NetworkEndpoint? = null
    @Mock
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null
    @Mock
    private val outputTransformer: OutputTransformer? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        underTest = BaseNetworkCommands(shellContext)

        given(shellContext!!.cloudbreakClient()).willReturn(cloudbreakClient)
        given(cloudbreakClient!!.networkEndpoint()).willReturn(networkEndpoint)
        given(shellContext.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
        given(shellContext.responseTransformer()).willReturn(responseTransformer)
    }

    @Test
    fun selectNetworkByIdWhichIsExist() {
        given(shellContext!!.networksByProvider).willReturn(ImmutableMap.of(50L, "test1"))

        val select = underTest!!.select(50L, null)

        Assert.assertEquals(select, "Network is selected with id: " + 50L)
    }

    @Test
    fun selectNetworkByIdWhichIsNotExist() {
        given(shellContext!!.networksByProvider).willReturn(ImmutableMap.of(50L, "test1"))

        val select = underTest!!.select(51L, null)

        Assert.assertEquals(select, "Network could not be found.")
    }

    @Test
    fun selectNetworkByNameWhichIsExist() {
        given(networkEndpoint!!.getPublic(anyString())).willReturn(networkJson())

        val select = underTest!!.select(null, "test1")

        Assert.assertEquals(select, "Network is selected with name: test1")
    }

    @Test(expected = RuntimeException::class)
    fun selectNetworkByNameWhichIsNotExistThenThowNotFoundException() {
        given(networkEndpoint!!.getPublic(anyString())).willThrow(NotFoundException("not found"))

        underTest!!.select(null, "test1")
    }

    @Test
    @Throws(Exception::class)
    fun showNetworkByIdWhichIsExist() {
        given(networkEndpoint!![anyLong()]).willReturn(networkJson())
        given(responseTransformer!!.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"))

        val show = underTest!!.show(50L, null)

        Assert.assertThat(show, containsString("id"))
        Assert.assertThat(show, containsString("name"))
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject<Any>(), Matchers.anyVararg<String>())
        verify(networkEndpoint, times(1))[anyLong()]
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun showNetworkByIdWhichIsNotExist() {
        `when`(networkEndpoint!![anyLong()]).thenThrow(NotFoundException("not found"))

        underTest!!.show(51L, null)
    }

    @Test
    @Throws(Exception::class)
    fun showNetworkByNameWhichIsExist() {
        given(networkEndpoint!!.getPublic(anyString())).willReturn(networkJson())
        given(responseTransformer!!.transformObjectToStringMap(anyMap())).willReturn(ImmutableMap.of("id", "1L", "name", "test1"))

        val show = underTest!!.show(null, "test1")

        Assert.assertThat(show, containsString("id"))
        Assert.assertThat(show, containsString("name"))
        verify(responseTransformer, times(1)).transformObjectToStringMap(anyObject<Any>(), Matchers.anyVararg<String>())
        verify(networkEndpoint, times(1)).getPublic(anyString())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun showNetworkByNameWhichIsNotExistThenThowNotFoundException() {
        given(networkEndpoint!!.getPublic(anyString())).willThrow(NotFoundException("not found"))

        underTest!!.show(null, "test1")
    }

    private fun networkJson(): NetworkJson {
        val networkJson = NetworkJson()
        networkJson.name = "test1"
        networkJson.id = "50"
        return networkJson
    }

}
