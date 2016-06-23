package com.sequenceiq.cloudbreak.shell.commands

import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyVararg
import org.mockito.Mockito.doNothing

import java.io.File
import java.io.IOException

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.common.SssdConfigCommands
import com.sequenceiq.cloudbreak.shell.completion.SssdProviderType
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType
import com.sequenceiq.cloudbreak.shell.completion.SssdTlsReqcertType
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer

class SssdConfigCommandsTest {

    @InjectMocks
    private var underTest: SssdConfigCommands? = null

    @Mock
    private val sssdConfigEndpoint: SssdConfigEndpoint? = null

    @Mock
    private val cloudbreakClient: CloudbreakClient? = null

    @Mock
    private val mockContext: ShellContext? = null

    @Mock
    private val exceptionTransformer: ExceptionTransformer? = null

    @Mock
    private val mockFile: File? = null

    @Mock
    private val outputTransformer: OutputTransformer? = null

    private var dummyResult: SssdConfigResponse? = null

    private var dummyFile: File? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        underTest = SssdConfigCommands(mockContext)
        MockitoAnnotations.initMocks(this)
        dummyResult = SssdConfigResponse()
        dummyResult!!.id = CONFIG_ID
        dummyResult!!.name = CONFIG_NAME
        dummyResult!!.providerType = com.sequenceiq.cloudbreak.api.model.SssdProviderType.LDAP
        dummyResult!!.schema = com.sequenceiq.cloudbreak.api.model.SssdSchemaType.RFC2307
        dummyResult!!.tlsReqcert = com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType.NEVER
        val classPackage = javaClass.getPackage().getName().replace("\\.".toRegex(), "/")
        val resource = ClassPathResource(classPackage + "/" + javaClass.getSimpleName() + ".class")
        dummyFile = resource.file
        given(cloudbreakClient!!.sssdConfigEndpoint()).willReturn(sssdConfigEndpoint)
        given(sssdConfigEndpoint!!.postPrivate(any<SssdConfigRequest>(SssdConfigRequest::class.java))).willReturn(IdJson(1L))
        given(sssdConfigEndpoint.postPublic(any<SssdConfigRequest>(SssdConfigRequest::class.java))).willReturn(IdJson(1L))
        given(sssdConfigEndpoint[anyLong()]).willReturn(dummyResult)
        given(sssdConfigEndpoint.getPublic(Matchers.anyString())).willReturn(dummyResult)
        given(mockContext!!.cloudbreakClient()).willReturn(cloudbreakClient)
        given(exceptionTransformer!!.transformToRuntimeException(any<Exception>(Exception::class.java))).willThrow(RuntimeException::class.java)
        given(mockContext.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
    }

    @Test
    @Throws(Exception::class)
    fun testSelectWithId() {
        underTest!!.select(CONFIG_ID, null)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).get(anyLong())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(1)).addSssdConfig(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testSelectWithName() {
        underTest!!.select(null, CONFIG_NAME)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(1)).addSssdConfig(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testSelectWithoutIdorName() {
        underTest!!.select(null, null)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
        Mockito.verify(mockContext, Mockito.times(0)).addSssdConfig(Matchers.anyString())
    }

    @Test
    fun testPublicAdd() {
        underTest!!.create("name", "desc", SssdProviderType("LDAP"), "url", SssdSchemaType("RFC2307"), "base", SssdTlsReqcertType("NEVER"), null, null,
                null, true)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).postPublic(any<SssdConfigRequest>(SssdConfigRequest::class.java))
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).postPrivate(any<SssdConfigRequest>(SssdConfigRequest::class.java))
    }

    @Test
    fun testPrivateAdd() {
        underTest!!.create("name", "desc", SssdProviderType("LDAP"), "url", SssdSchemaType("RFC2307"), "base", SssdTlsReqcertType("NEVER"), null, null,
                null, false)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).postPublic(any<SssdConfigRequest>(SssdConfigRequest::class.java))
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).postPrivate(any<SssdConfigRequest>(SssdConfigRequest::class.java))
    }

    @Test
    fun testUploadFileNotFound() {
        given(mockFile!!.exists()).willReturn(false)
        underTest!!.upload("name", "desc", mockFile, true)
        Mockito.verify(mockFile, Mockito.times(1)).exists()
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).postPublic(any<SssdConfigRequest>(SssdConfigRequest::class.java))
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).postPrivate(any<SssdConfigRequest>(SssdConfigRequest::class.java))

    }

    @Test
    @Throws(IOException::class)
    fun testPublicUpload() {
        underTest!!.upload("name", "desc", dummyFile, true)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).postPublic(any<SssdConfigRequest>(SssdConfigRequest::class.java))
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).postPrivate(any<SssdConfigRequest>(SssdConfigRequest::class.java))
    }

    @Test
    fun testPrivateUpload() {
        given(mockFile!!.exists()).willReturn(true)
        underTest!!.upload("name", "desc", dummyFile, false)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).postPublic(any<SssdConfigRequest>(SssdConfigRequest::class.java))
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).postPrivate(any<SssdConfigRequest>(SssdConfigRequest::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testShowSssdConfigById() {
        underTest!!.show(CONFIG_ID, null)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).get(anyLong())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowSssdConfigByName() {
        underTest!!.show(null, CONFIG_NAME)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowSssdConfigWithoutIdAndName() {
        underTest!!.show(null, null)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteSssdConfigById() {
        doNothing().`when`<SssdConfigEndpoint>(sssdConfigEndpoint).delete(java.lang.Long.valueOf(CONFIG_ID))
        underTest!!.delete(CONFIG_ID, null)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteSssdConfigByName() {
        doNothing().`when`<SssdConfigEndpoint>(sssdConfigEndpoint).deletePublic(CONFIG_NAME)
        underTest!!.delete(null, CONFIG_NAME)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).deletePublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteSssdConfigByIdAndName() {
        doNothing().`when`<SssdConfigEndpoint>(sssdConfigEndpoint).delete(java.lang.Long.valueOf(CONFIG_ID))
        underTest!!.delete(CONFIG_ID, CONFIG_NAME)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).deletePublic(Matchers.anyString())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(1)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteSssdConfigWithoutIdAndName() {
        underTest!!.delete(null, null)
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).deletePublic(Matchers.anyString())
        Mockito.verify(sssdConfigEndpoint, Mockito.times(0)).delete(anyLong())
    }

    companion object {
        private val CONFIG_ID = 50L
        private val CONFIG_NAME = "dummyName"
    }
}
