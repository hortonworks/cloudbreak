package com.sequenceiq.cloudbreak.shell.commands

import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Mockito.doNothing

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.common.BlueprintCommands
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer

class BlueprintCommandsTest {

    @InjectMocks
    private var underTest: BlueprintCommands? = null

    @Mock
    private val cloudbreakClient: CloudbreakClient? = null

    @Mock
    private val blueprintEndpoint: BlueprintEndpoint? = null

    @Mock
    private val mockContext: ShellContext? = null

    @Mock
    private val exceptionTransformer: ExceptionTransformer? = null

    private var dummyResult: BlueprintResponse? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        underTest = BlueprintCommands(mockContext)

        dummyResult = BlueprintResponse()
        dummyResult!!.id = BLUEPRINT_ID.toString()
        given(mockContext!!.isMarathonMode).willReturn(false)
        given(mockContext.cloudbreakClient()).willReturn(cloudbreakClient)
        given(cloudbreakClient!!.blueprintEndpoint()).willReturn(blueprintEndpoint)
        given(exceptionTransformer!!.transformToRuntimeException(any<Exception>(Exception::class.java))).willThrow(RuntimeException::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testSelectBlueprintById() {
        given(blueprintEndpoint!![java.lang.Long.valueOf(BLUEPRINT_ID)]).willReturn(dummyResult)
        underTest!!.select(BLUEPRINT_ID, null)
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).get(anyLong())
        Mockito.verify(mockContext, Mockito.times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP)
    }

    @Test
    @Throws(Exception::class)
    fun testSelectBlueprintByIdAndName() {
        given(blueprintEndpoint!![java.lang.Long.valueOf(BLUEPRINT_ID)]).willReturn(dummyResult)
        underTest!!.select(BLUEPRINT_ID, BLUEPRINT_NAME)
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).get(anyLong())
        Mockito.verify(blueprintEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP)
    }

    @Test
    @Throws(Exception::class)
    fun testSelectBlueprintByName() {
        given(blueprintEndpoint!!.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult)
        underTest!!.select(null, BLUEPRINT_NAME)
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(1)).setHint(Hints.CONFIGURE_INSTANCEGROUP)
    }

    @Test
    @Throws(Exception::class)
    fun testSelectBlueprintWithoutIdAndName() {
        underTest!!.select(null, null)
        Mockito.verify(blueprintEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
        Mockito.verify(blueprintEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testSelectBlueprintByNameNotFound() {
        given(blueprintEndpoint!![java.lang.Long.valueOf(BLUEPRINT_ID)]).willReturn(null)
        underTest!!.select(BLUEPRINT_ID, null)
        Mockito.verify(mockContext, Mockito.times(0)).setHint(Hints.CONFIGURE_INSTANCEGROUP)
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun testShowBlueprintById() {
        given(blueprintEndpoint!![java.lang.Long.valueOf(BLUEPRINT_ID)]).willReturn(dummyResult)
        underTest!!.show(BLUEPRINT_ID, null)
        Mockito.verify(blueprintEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).get(anyLong())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun testShowBlueprintByName() {
        given(blueprintEndpoint!![java.lang.Long.valueOf(BLUEPRINT_ID)]).willReturn(dummyResult)
        given(blueprintEndpoint.getPublic(BLUEPRINT_NAME)).willReturn(dummyResult)
        underTest!!.show(null, BLUEPRINT_NAME)
        Mockito.verify(blueprintEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun testShowBlueprintByIdAndName() {
        given(blueprintEndpoint!![java.lang.Long.valueOf(BLUEPRINT_ID)]).willReturn(dummyResult)
        underTest!!.show(BLUEPRINT_ID, BLUEPRINT_NAME)
        Mockito.verify(blueprintEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).get(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteBlueprintById() {
        doNothing().`when`<BlueprintEndpoint>(blueprintEndpoint).deletePublic(BLUEPRINT_ID.toString())
        underTest!!.delete(BLUEPRINT_ID, null)
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteBlueprintByName() {
        doNothing().`when`<BlueprintEndpoint>(blueprintEndpoint).deletePublic(BLUEPRINT_NAME)
        underTest!!.delete(null, BLUEPRINT_NAME)
        Mockito.verify(blueprintEndpoint, Mockito.times(1)).deletePublic(Matchers.anyString())
    }

    companion object {
        private val BLUEPRINT_ID = 50L
        private val BLUEPRINT_NAME = "dummyName"
    }

}
