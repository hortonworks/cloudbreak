package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint

import java.util.ArrayList
import java.util.Arrays
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService

@Component
class SmartSenseConfigProvider {

    @Value("${cb.smartsense.configure:false}")
    private val configureSmartSense: Boolean = false

    @Inject
    private val blueprintProcessor: BlueprintProcessor? = null

    @Inject
    private val hostGroupService: HostGroupService? = null

    fun smartSenseIsConfigurable(blueprint: String): Boolean {
        return configureSmartSense && blueprintProcessor!!.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprint)
    }

    fun addToBlueprint(stack: Stack, blueprintText: String): String {
        var blueprintText = blueprintText
        val configs = ArrayList<BlueprintConfigurationEntry>()
        val credential = stack.credential
        val params = credential.attributes.map
        val smartSenseId = params["smartSenseId"].toString()
        if (configureSmartSense && StringUtils.isNoneEmpty(smartSenseId)) {
            val hostGroups = hostGroupService!!.getByCluster(stack.cluster.id)
            val hostGroupNames = hostGroups.stream().map(hostGroupNameMapper).collect(Collectors.toSet<String>())
            blueprintText = addSmartSenseServerToBp(blueprintText, hostGroups, hostGroupNames)
            blueprintText = blueprintProcessor!!.addComponentToHostgroups(HST_AGENT_COMPONENT, hostGroupNames, blueprintText)
            configs.addAll(smartSenseServerConfigs)
            configs.add(BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.smartsense.id", smartSenseId))
            configs.addAll(getSmartSenseGatewayConfigs(stack))
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, true)
        }
        return blueprintText
    }

    private fun addSmartSenseServerToBp(blueprintText: String, hostGroups: Set<HostGroup>, hostGroupNames: Set<String>): String {
        var blueprintText = blueprintText
        if (!blueprintProcessor!!.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprintText)) {
            var aHostGroupName = hostGroupNames.stream().findFirst().get()
            val hostGroupWithOneNode = hostGroups.stream().filter({ hostGroup -> hostGroup.getHostMetadata().size == 1 }).map(hostGroupNameMapper).findFirst()
            if (hostGroupWithOneNode.isPresent()) {
                aHostGroupName = hostGroupWithOneNode.get()
            }
            blueprintText = blueprintProcessor.addComponentToHostgroups(HST_SERVER_COMPONENT, Arrays.asList<String>(aHostGroupName), blueprintText)
        }
        return blueprintText
    }

    private val hostGroupNameMapper: Function<HostGroup, String>
        get() = { hostGroup -> hostGroup.getName() }

    private val smartSenseServerConfigs: Collection<BlueprintConfigurationEntry>
        get() {
            val configs = ArrayList<BlueprintConfigurationEntry>()
            configs.add(BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.account.name", "Hortonworks Data Platform AWS Marketplace"))
            configs.add(BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.notification.email", "aws-marketplace@hortonworks.com"))
            return configs
        }

    private fun getSmartSenseGatewayConfigs(stack: Stack): Collection<BlueprintConfigurationEntry> {
        val configs = ArrayList<BlueprintConfigurationEntry>()
        val privateIp = stack.gatewayInstanceGroup.instanceMetaData.stream().findFirst().get().getPrivateIp()
        configs.add(BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "gateway.host", privateIp))
        configs.add(BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "gateway.enabled", "true"))
        return configs
    }

    companion object {
        private val SMART_SENSE_SERVER_CONFIG_FILE = "hst-server-conf"
        private val HST_SERVER_COMPONENT = "HST_SERVER"
        private val HST_AGENT_COMPONENT = "HST_AGENT"
    }
}
