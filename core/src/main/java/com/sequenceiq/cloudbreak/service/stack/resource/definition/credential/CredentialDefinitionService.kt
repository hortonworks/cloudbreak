package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import javax.inject.Inject

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.service.stack.resource.definition.MissingParameterException
import com.sequenceiq.cloudbreak.service.stack.resource.definition.ResourceDefinitionService
import com.sequenceiq.cloudbreak.util.JsonUtil

@Service
class CredentialDefinitionService {

    @Inject
    private val definitionService: ResourceDefinitionService? = null
    @Inject
    private val encryptor: PBEStringCleanablePasswordEncryptor? = null

    fun processProperties(cloudPlatform: Platform, properties: Map<String, Any>): Map<String, Any> {
        return processValues(getDefinition(cloudPlatform), properties, false)
    }

    fun revertProperties(cloudPlatform: Platform, properties: Map<String, Any>): Map<String, Any> {
        return processValues(getDefinition(cloudPlatform), properties, true)
    }

    private fun getDefinition(cloudPlatform: Platform): Definition {
        val json = definitionService!!.getResourceDefinition(cloudPlatform.value(), RESOURCE_TYPE)
        try {
            return JsonUtil.readValue<Definition>(json, Definition::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    private fun processValues(definition: Definition, properties: Map<String, Any>, revert: Boolean): Map<String, Any> {
        val processed = HashMap<String, Any>()
        processed.putAll(processValues(properties, definition.defaultValues, revert))
        val selector = properties[SELECTOR]
        if (selector != null) {
            processed.put(SELECTOR, selector)
            processed.putAll(processValues(properties, collectSelectorValues(definition, selector.toString()), revert))
        }
        return processed
    }

    private fun collectSelectorValues(definition: Definition, selectorName: String): List<Value> {
        val values = ArrayList<Value>()
        val selectors = definition.selectors
        var currentSelector = selectorName
        var element: Selector
        while ((element = findSelector(selectors, currentSelector)) != null) {
            values.addAll(element.values)
            currentSelector = element.parent
        }
        return values
    }

    private fun findSelector(selectors: List<Selector>, selector: String): Selector? {
        for (s in selectors) {
            if (s.name == selector) {
                return s
            }
        }
        return null
    }

    private fun processValues(properties: Map<String, Any>, values: List<Value>, revert: Boolean): Map<String, Any> {
        val processed = HashMap<String, Any>()
        for (value in values) {
            val key = value.name
            var property = getProperty(properties, key, isOptional(value))
            if (property != null && !property.isEmpty() && isEncrypted(value)) {
                property = if (revert) encryptor!!.decrypt(property) else encryptor!!.encrypt(property)
            }
            processed.put(key, property)
        }
        return processed
    }

    private fun isEncrypted(value: Value): Boolean {
        val encrypted = value.encrypted
        return isNotNull(encrypted) && encrypted!!
    }

    private fun isOptional(value: Value): Boolean {
        val optional = value.optional
        return isNotNull(optional) && optional!!
    }

    private fun isNotNull(`object`: Any?): Boolean {
        return null != `object`
    }

    private fun getProperty(properties: Map<String, Any>, key: String, optional: Boolean): String? {
        val value = properties[key]
        if (value == null && !optional) {
            throw MissingParameterException(String.format("Missing '%s' property!", key))
        }
        return value?.toString()
    }

    companion object {

        private val SELECTOR = "selector"
        private val RESOURCE_TYPE = "credential"
    }

}
