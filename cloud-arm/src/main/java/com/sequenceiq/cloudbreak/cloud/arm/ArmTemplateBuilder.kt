package com.sequenceiq.cloudbreak.cloud.arm

import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString

import java.io.IOException
import java.util.HashMap

import javax.inject.Inject

import org.apache.commons.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStackView
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmSecurityView
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.Network

import freemarker.template.Configuration
import freemarker.template.TemplateException

@Service("ArmTemplateBuilder")
class ArmTemplateBuilder {

    @Value("${cb.arm.template.path:}")
    private val armTemplatePath: String? = null

    @Value("${cb.arm.parameter.path:}")
    private val armTemplateParametersPath: String? = null

    @Inject
    private val freemarkerConfiguration: Configuration? = null

    @Inject
    private val armUtils: ArmUtils? = null

    @Inject
    private val armStorage: ArmStorage? = null

    fun build(stackName: String, armCredentialView: ArmCredentialView, armStack: ArmStackView, cloudContext: CloudContext, cloudStack: CloudStack): String {
        try {
            val imageUrl = cloudStack.image.imageName
            val imageName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
            val network = cloudStack.network
            val model = HashMap<String, Any>()
            model.put("credential", armCredentialView)
            val rootDiskStorage = armStorage!!.getImageStorageName(armCredentialView, cloudContext,
                    armStorage.getPersistentStorageName(cloudStack.parameters),
                    armStorage.getArmAttachedStorageOption(cloudStack.parameters))
            model.put("storage_account_name", rootDiskStorage)
            model.put("image_storage_container_name", ArmStorage.IMAGES)
            model.put("storage_container_name", armStorage.getDiskContainerName(cloudContext))
            model.put("storage_vhd_name", imageName)
            model.put("stackname", stackName)
            model.put("region", cloudContext.location!!.region.value())
            model.put("subnet1Prefix", network.subnet.cidr)
            model.put("groups", armStack.groups)
            model.put("securities", ArmSecurityView(cloudStack.security))
            model.put("corecustomData", base64EncodedUserData(cloudStack.image.getUserData(InstanceGroupType.CORE)))
            model.put("gatewaycustomData", base64EncodedUserData(cloudStack.image.getUserData(InstanceGroupType.GATEWAY)))
            model.put("disablePasswordAuthentication", !armCredentialView.passwordAuthenticationRequired())
            model.put("existingVPC", armUtils!!.isExistingNetwork(network))
            model.put("resourceGroupName", armUtils.getCustomResourceGroupName(network))
            model.put("existingVNETName", armUtils.getCustomNetworkId(network))
            model.put("existingSubnetName", armUtils.getCustomSubnetId(network))
            val generatedTemplate = processTemplateIntoString(freemarkerConfiguration!!.getTemplate(armTemplatePath, "UTF-8"), model)
            LOGGER.debug("Generated Arm template: {}", generatedTemplate)
            return generatedTemplate
        } catch (e: IOException) {
            throw CloudConnectorException("Failed to process the Arm TemplateBuilder", e)
        } catch (e: TemplateException) {
            throw CloudConnectorException("Failed to process the Arm TemplateBuilder", e)
        }

    }

    fun buildParameters(credential: CloudCredential, network: Network, image: Image): String {
        try {
            return processTemplateIntoString(freemarkerConfiguration!!.getTemplate(armTemplateParametersPath, "UTF-8"), HashMap<Any, Any>())
        } catch (e: IOException) {
            throw CloudConnectorException("Failed to process the Arm TemplateParameterBuilder", e)
        } catch (e: TemplateException) {
            throw CloudConnectorException("Failed to process the Arm TemplateParameterBuilder", e)
        }

    }

    private fun base64EncodedUserData(data: String): String {
        return String(Base64.encodeBase64(String.format("%s", data).toByteArray()))
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ArmTemplateBuilder::class.java)
    }
}