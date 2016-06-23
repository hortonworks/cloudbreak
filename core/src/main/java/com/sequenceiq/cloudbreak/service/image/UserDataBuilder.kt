package com.sequenceiq.cloudbreak.service.image

import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString

import java.io.IOException
import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.annotations.VisibleForTesting
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.Platform

import freemarker.template.Configuration
import freemarker.template.TemplateException

@Component
class UserDataBuilder {

    @Inject
    private val userDataBuilderParams: UserDataBuilderParams? = null

    @Inject
    private var freemarkerConfiguration: Configuration? = null

    internal fun buildUserData(cloudPlatform: Platform, pubKey: String, tmpSshKey: String, sshUser: String, parameters: PlatformParameters,
                               relocate: Boolean?): Map<InstanceGroupType, String> {
        val result = HashMap<InstanceGroupType, String>()
        for (type in InstanceGroupType.values()) {
            val userData = build(type, cloudPlatform, pubKey, tmpSshKey, sshUser, parameters, relocate)
            result.put(type, userData)
            LOGGER.debug("User data for {}, content; {}", type, userData)
        }

        return result
    }

    private fun build(type: InstanceGroupType, cloudPlatform: Platform, publicSssKey: String, tmpSshKey: String, sshUser: String, params: PlatformParameters,
                      relocate: Boolean?): String {
        val model = HashMap<String, Any>()
        model.put("cloudPlatform", cloudPlatform.value())
        model.put("platformDiskPrefix", params.scriptParams().diskPrefix)
        model.put("platformDiskStartLabel", params.scriptParams().startLabel)
        model.put("gateway", type == InstanceGroupType.GATEWAY)
        model.put("tmpSshKey", tmpSshKey)
        model.put("sshUser", sshUser)
        model.put("publicSshKey", publicSssKey)
        model.put("customUserData", userDataBuilderParams!!.customData)
        model.put("relocateDocker", relocate!!.booleanValue())
        return build(model)
    }

    private fun build(model: Map<String, Any>): String {
        try {
            return processTemplateIntoString(freemarkerConfiguration!!.getTemplate("init/init.ftl", "UTF-8"), model)
        } catch (e: IOException) {
            LOGGER.error(e.message, e)
            throw CloudConnectorException("Failed to process init script freemarker template", e)
        } catch (e: TemplateException) {
            LOGGER.error(e.message, e)
            throw CloudConnectorException("Failed to process init script freemarker template", e)
        }

    }

    @VisibleForTesting
    internal fun setFreemarkerConfiguration(freemarkerConfiguration: Configuration) {
        this.freemarkerConfiguration = freemarkerConfiguration
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(UserDataBuilder::class.java)
    }
}
