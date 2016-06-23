package com.sequenceiq.cloudbreak.cloud.openstack.heat

import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isNoneEmpty

import java.io.IOException
import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView
import com.sequenceiq.cloudbreak.cloud.openstack.view.OpenStackGroupView

import freemarker.template.Configuration
import freemarker.template.TemplateException

@Service
class HeatTemplateBuilder {

    @Value("${cb.openstack.heat.template.path:}")
    private val openStackHeatTemplatePath: String? = null

    @Inject
    private val openStackUtil: OpenStackUtils? = null
    @Inject
    private val freemarkerConfiguration: Configuration? = null

    fun build(stackName: String, groups: List<Group>, security: Security, instanceUserData: Image, existingNetwork: Boolean,
              existingSubnet: Boolean, assignFloatingIp: Boolean): String {
        try {
            val novaInstances = OpenStackGroupView(groups).flatNovaView
            val model = HashMap<String, Any>()
            model.put("cb_stack_name", openStackUtil!!.adjustStackNameLength(stackName))
            model.put("agents", novaInstances)
            model.put("core_user_data", formatUserData(instanceUserData.getUserData(InstanceGroupType.CORE)))
            model.put("gateway_user_data", formatUserData(instanceUserData.getUserData(InstanceGroupType.GATEWAY)))
            model.put("rules", security.rules)
            model.put("existingNetwork", existingNetwork)
            model.put("existingSubnet", existingSubnet)
            model.put("assignFloatingIp", assignFloatingIp)
            val generatedTemplate = processTemplateIntoString(freemarkerConfiguration!!.getTemplate(openStackHeatTemplatePath, "UTF-8"), model)
            LOGGER.debug("Generated Heat template: {}", generatedTemplate)
            return generatedTemplate
        } catch (e: IOException) {
            throw CloudConnectorException("Failed to process the OpenStack HeatTemplateBuilder", e)
        } catch (e: TemplateException) {
            throw CloudConnectorException("Failed to process the OpenStack HeatTemplateBuilder", e)
        }

    }

    fun buildParameters(auth: AuthenticatedContext, network: Network, image: Image, existingNetwork: Boolean, existingSubnetCidr: String): Map<String, String> {
        val osCredential = KeystoneCredentialView(auth)
        val neutronView = NeutronNetworkView(network)
        val parameters = HashMap<String, String>()
        if (neutronView.assignFloatingIp()) {
            parameters.put("public_net_id", neutronView.publicNetId)
        }
        parameters.put("image_id", image.imageName)
        parameters.put("key_name", osCredential.keyPairName)
        if (existingNetwork) {
            parameters.put("app_net_id", openStackUtil!!.getCustomNetworkId(network))
            if (isNoneEmpty(existingSubnetCidr)) {
                parameters.put("subnet_id", openStackUtil.getCustomSubnetId(network))
            } else {
                parameters.put("router_id", openStackUtil.getCustomRouterId(network))
            }
        }
        parameters.put("app_net_cidr", if (isBlank(existingSubnetCidr)) neutronView.subnetCIDR else existingSubnetCidr)
        return parameters
    }

    private fun formatUserData(userData: String): String {
        val lines = userData.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        val sb = StringBuilder()
        for (i in lines.indices) {
            // be aware of the OpenStack Heat template formatting
            sb.append("            " + lines[i] + "\n")
        }
        return sb.toString()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HeatTemplateBuilder::class.java)
    }

}