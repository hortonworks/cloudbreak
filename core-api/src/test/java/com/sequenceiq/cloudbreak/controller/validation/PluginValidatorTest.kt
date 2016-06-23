package com.sequenceiq.cloudbreak.controller.validation

import java.util.Collections
import java.util.HashMap

import javax.validation.ConstraintValidatorContext

import org.apache.commons.codec.binary.Base64
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.validation.PluginValidator
import com.sequenceiq.cloudbreak.validation.ValidPlugin

@RunWith(MockitoJUnitRunner::class)
class PluginValidatorTest : AbstractValidatorTest() {

    @InjectMocks
    private val underTest: PluginValidator? = null

    @Mock
    private val constraintValidatorContext: ConstraintValidatorContext? = null

    @Mock
    private val validPlugin: ValidPlugin? = null

    @Before
    fun setUp() {
        underTest!!.initialize(validPlugin)
        BDDMockito.given(constraintValidatorContext!!.buildConstraintViolationWithTemplate(Matchers.anyString())).willReturn(constraintViolationBuilder)
    }

    @Test
    fun validPluginJsonWillReturnTrue() {
        val plugins = HashMap<String, ExecutionType>()
        plugins.put("http://github.com/user/consul-plugins-plugin1.git", ExecutionType.ALL_NODES)
        plugins.put("https://github.com/user/consul-plugins-plugin1.git", ExecutionType.ALL_NODES)
        plugins.put("git://github.com/user/consul-plugins-plugin1.git", ExecutionType.ALL_NODES)
        plugins.put("base64://" + Base64.encodeBase64String("plugin.toml:\nrecipe-pre-install:".toByteArray()), ExecutionType.ALL_NODES)
        Assert.assertTrue(underTest!!.isValid(plugins, constraintValidatorContext))
    }

    @Test
    fun inValidPluginNullReturnFalse() {
        Assert.assertEquals(underTest!!.isValid(null, constraintValidatorContext), false)
    }

    @Test
    fun inValidPluginEmptyReturnFalse() {
        Assert.assertFalse(underTest!!.isValid(emptyMap<String, ExecutionType>(), constraintValidatorContext))
    }

    @Test
    fun inValidPluginUrlJsonWillReturnFalse() {
        val plugins = HashMap<String, ExecutionType>()
        plugins.put("asd://github.com/user/plugin1.git", ExecutionType.ALL_NODES)
        Assert.assertFalse(underTest!!.isValid(plugins, constraintValidatorContext))
    }

    @Test
    fun inValidBase64MissingScriptWillReturnFalse() {
        val plugins = HashMap<String, ExecutionType>()
        plugins.put("base64://" + Base64.encodeBase64String("plugin.toml:".toByteArray()), ExecutionType.ALL_NODES)
        Assert.assertFalse(underTest!!.isValid(plugins, constraintValidatorContext))
    }

    @Test
    fun inValidBase64MissingPluginDotTomlWillReturnFalse() {
        val plugins = HashMap<String, ExecutionType>()
        plugins.put("base64://" + Base64.encodeBase64String("recipe-pre-install:\nrecipe-post-install:".toByteArray()), ExecutionType.ALL_NODES)
        Assert.assertFalse(underTest!!.isValid(plugins, constraintValidatorContext))
    }
}