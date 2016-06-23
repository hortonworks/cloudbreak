package com.sequenceiq.cloudbreak.shell.commands

import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyVararg
import org.mockito.Mockito.anyString

import java.io.File

import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint
import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.base.BaseCredentialCommands
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer

class BaseCredentialCommandsTest {

    @InjectMocks
    private var underTest: BaseCredentialCommands? = null

    @Mock(answer = Answers.RETURNS_MOCKS)
    private val context: ShellContext? = null

    @Mock
    private val cloudbreakClient: CloudbreakClient? = null

    @Mock
    private val credentialEndpoint: CredentialEndpoint? = null

    @Mock
    private val exceptionTransformer: ExceptionTransformer? = null

    @Mock
    private val outputTransformer: OutputTransformer? = null

    private var dummyResult: CredentialResponse? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        underTest = BaseCredentialCommands(context)
        MockitoAnnotations.initMocks(this)
        dummyResult = CredentialResponse()
        dummyResult!!.id = java.lang.Long.valueOf(DUMMY_ID)
        given(cloudbreakClient!!.credentialEndpoint()).willReturn(credentialEndpoint)
        given(context!!.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
        given(context.cloudbreakClient()).willReturn(cloudbreakClient)
        given(exceptionTransformer!!.transformToRuntimeException(any<Exception>(Exception::class.java))).willThrow(RuntimeException::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testSelectCredentialById() {
        given(credentialEndpoint!![java.lang.Long.valueOf(DUMMY_ID)]).willReturn(dummyResult)
        underTest!!.select(DUMMY_ID, null)
        Mockito.verify(context, Mockito.times(1)).setCredential(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testSelectCredentialByName() {
        given(credentialEndpoint!!.getPublic(DUMMY_NAME)).willReturn(dummyResult)
        underTest!!.select(null, DUMMY_NAME)
        Mockito.verify(context, Mockito.times(1)).setCredential(Matchers.anyString())
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun testSelectCredentialByNameNotFound() {
        given(credentialEndpoint!!.getPublic(DUMMY_NAME)).willReturn(null)
        underTest!!.select(null, DUMMY_NAME)
        Mockito.verify(context, Mockito.times(0)).setCredential(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testSelectCredentialWithoutIdAndName() {
        underTest!!.select(null, null)
        Mockito.verify(context, Mockito.times(0)).setCredential(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowCredentialById() {
        given(credentialEndpoint!![java.lang.Long.valueOf(DUMMY_ID)]).willReturn(dummyResult)
        underTest!!.show(DUMMY_ID, null)
        Mockito.verify(credentialEndpoint, Mockito.times(1)).get(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testShowCredentialByName() {
        given(credentialEndpoint!![java.lang.Long.valueOf(DUMMY_ID)]).willReturn(dummyResult)
        given(credentialEndpoint.getPublic(DUMMY_NAME)).willReturn(dummyResult)
        underTest!!.show(null, DUMMY_NAME)
        Mockito.verify(credentialEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(credentialEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowCredentialByNameNotFound() {
        given(credentialEndpoint!!.getPublic(DUMMY_NAME)).willReturn(null)
        underTest!!.show(null, DUMMY_NAME)
        Mockito.verify(credentialEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowCredentialWithoutIdAndName() {
        underTest!!.show(null, null)
        Mockito.verify(credentialEndpoint, Mockito.times(0)).get(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteCredentialById() {
        underTest!!.delete(DUMMY_ID, null)
        Mockito.verify(credentialEndpoint, Mockito.times(1)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteCredentialByName() {
        underTest!!.delete(null, DUMMY_NAME)
        Mockito.verify(credentialEndpoint, Mockito.times(1)).deletePublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteCredentialWithoutIdAndName() {
        underTest!!.delete(null, null)
        Mockito.verify(credentialEndpoint, Mockito.times(0)).delete(anyLong())
        Mockito.verify(credentialEndpoint, Mockito.times(0)).deletePublic(Matchers.anyString())
    }

    private fun getAbsolutePath(path: String): String {
        val classLoader = javaClass.getClassLoader()
        val file = File(classLoader.getResource(path)!!.getFile())
        return file.getAbsolutePath()
    }

    companion object {
        private val DUMMY_NAME = "dummyName"
        private val DUMMY_ID = 60L
    }


}
