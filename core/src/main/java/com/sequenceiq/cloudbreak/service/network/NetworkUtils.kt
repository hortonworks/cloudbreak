package com.sequenceiq.cloudbreak.service.network

import com.sequenceiq.cloudbreak.service.network.ExposedService.ACCUMULO_MASTER
import com.sequenceiq.cloudbreak.service.network.ExposedService.ACCUMULO_TSERVER
import com.sequenceiq.cloudbreak.service.network.ExposedService.AMBARI
import com.sequenceiq.cloudbreak.service.network.ExposedService.ATLAS
import com.sequenceiq.cloudbreak.service.network.ExposedService.CONSUL
import com.sequenceiq.cloudbreak.service.network.ExposedService.CONTAINER_LOGS
import com.sequenceiq.cloudbreak.service.network.ExposedService.ELASTIC_SEARCH
import com.sequenceiq.cloudbreak.service.network.ExposedService.FALCON
import com.sequenceiq.cloudbreak.service.network.ExposedService.HBASE_MASTER
import com.sequenceiq.cloudbreak.service.network.ExposedService.HBASE_MASTER_WEB
import com.sequenceiq.cloudbreak.service.network.ExposedService.HBASE_REGION
import com.sequenceiq.cloudbreak.service.network.ExposedService.HBASE_REGION_INFO
import com.sequenceiq.cloudbreak.service.network.ExposedService.HIVE_METASTORE
import com.sequenceiq.cloudbreak.service.network.ExposedService.HIVE_SERVER
import com.sequenceiq.cloudbreak.service.network.ExposedService.HIVE_SERVER_HTTP
import com.sequenceiq.cloudbreak.service.network.ExposedService.HTTPS
import com.sequenceiq.cloudbreak.service.network.ExposedService.JOB_HISTORY_SERVER
import com.sequenceiq.cloudbreak.service.network.ExposedService.KIBANA
import com.sequenceiq.cloudbreak.service.network.ExposedService.KNOX_GW
import com.sequenceiq.cloudbreak.service.network.ExposedService.NAMENODE
import com.sequenceiq.cloudbreak.service.network.ExposedService.OOZIE
import com.sequenceiq.cloudbreak.service.network.ExposedService.RANGER
import com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_IPC
import com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_SCHEDULER
import com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_WEB
import com.sequenceiq.cloudbreak.service.network.ExposedService.SHIPYARD
import com.sequenceiq.cloudbreak.service.network.ExposedService.SPARK_HISTORY_SERVER
import com.sequenceiq.cloudbreak.service.network.ExposedService.SSH
import com.sequenceiq.cloudbreak.service.network.ExposedService.STORM
import com.sequenceiq.cloudbreak.service.network.ExposedService.SWARM
import com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELIN_UI
import com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELIN_WEB_SOCKET

import java.util.ArrayList
import java.util.LinkedList

import com.google.common.base.Optional
import com.sequenceiq.cloudbreak.cloud.model.EndpointRule
import com.sequenceiq.cloudbreak.domain.SecurityRule
import com.sequenceiq.cloudbreak.domain.Stack

class NetworkUtils private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {

        private val ports = ArrayList<Port>()

        init {
            ports.add(Port(SSH, "22", "tcp"))
            ports.add(Port(HTTPS, "443", "tcp"))
            ports.add(Port(AMBARI, "8080", "tcp"))
            ports.add(Port(CONSUL, "8500", "tcp"))
            ports.add(Port(NAMENODE, "50070", "tcp"))
            ports.add(Port(RESOURCEMANAGER_WEB, "8088", "tcp"))
            ports.add(Port(RESOURCEMANAGER_SCHEDULER, "8030", "tcp"))
            ports.add(Port(RESOURCEMANAGER_IPC, "8050", "tcp"))
            ports.add(Port(JOB_HISTORY_SERVER, "19888", "tcp"))
            ports.add(Port(HBASE_MASTER_WEB, "16010", "tcp"))
            ports.add(Port(HBASE_MASTER, "60000", "tcp"))
            ports.add(Port(HBASE_REGION, "16020", "tcp"))
            ports.add(Port(HBASE_REGION_INFO, "16030", "tcp"))
            ports.add(Port(HIVE_METASTORE, "9083", "tcp"))
            ports.add(Port(HIVE_SERVER, "10000", "tcp"))
            ports.add(Port(HIVE_SERVER_HTTP, "10001", "tcp"))
            ports.add(Port(ACCUMULO_MASTER, "9999", "tcp"))
            ports.add(Port(ACCUMULO_TSERVER, "9997", "tcp"))
            ports.add(Port(ATLAS, "21000", "tcp"))
            ports.add(Port(KNOX_GW, "8443", "tcp"))
            ports.add(Port(FALCON, "15000", "tcp"))
            ports.add(Port(STORM, "8744", "tcp"))
            ports.add(Port(OOZIE, "11000", "tcp"))
            ports.add(Port(SPARK_HISTORY_SERVER, "18080", "tcp"))
            ports.add(Port(CONTAINER_LOGS, "8042", "tcp"))
            ports.add(Port(ZEPPELIN_WEB_SOCKET, "9996", "tcp"))
            ports.add(Port(ZEPPELIN_UI, "9995", "tcp"))
            ports.add(Port(RANGER, "6080", "tcp"))
            ports.add(Port(KIBANA, "3080", "tcp"))
            ports.add(Port(ELASTIC_SEARCH, "9200", "tcp"))
            ports.add(Port(SWARM, "3376", "tcp"))
            ports.add(Port(SHIPYARD, "7070", "tcp"))
        }

        val portsWithoutAclRules: List<Port>
            get() = ports

        fun getPorts(stack: Optional<Stack>): List<Port> {
            val result = ArrayList<Port>()

            if (stack.isPresent) {
                val stackInstance = stack.get()
                val aclRules = createACLRules(stackInstance)
                for (rule in stackInstance.securityGroup.securityRules) {
                    for (portNumber in rule.ports) {
                        val port = getPortByPortNumberAndProtocol(portNumber, rule.protocol)
                        if (port != null) {
                            result.add(Port(port.exposedService, portNumber, portNumber, rule.protocol, aclRules))
                        }
                    }
                }

            } else {
                result.addAll(ports)
            }

            return result
        }

        private fun createACLRules(stack: Stack): List<EndpointRule> {
            val rules = LinkedList<EndpointRule>()
            for (rule in stack.securityGroup.securityRules) {
                rules.add(EndpointRule(EndpointRule.Action.PERMIT.text, rule.cidr))
            }
            val internalRule = EndpointRule(EndpointRule.Action.PERMIT.toString(), stack.network.subnetCIDR)
            rules.add(internalRule)
            rules.add(EndpointRule.DENY_RULE)
            return rules
        }

        private fun getPortByPortNumberAndProtocol(portNumber: String, protocol: String): Port? {
            for (port in ports) {
                if (portNumber == port.port && protocol == port.protocol) {
                    return port
                }
            }
            return null
        }

        fun getPortByServiceName(exposedService: ExposedService): Port? {
            for (port in ports) {
                if (port.exposedService == exposedService) {
                    return port
                }
            }
            return null
        }
    }
}
