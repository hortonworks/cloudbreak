package com.sequenceiq.cloudbreak.core.bootstrap.service.container

import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_DB
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_SERVER
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.CONSUL_WATCH
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.HAVEGED
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.KERBEROS
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.LDAP
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.LOGROTATE
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.REGISTRATOR
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.SHIPYARD
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.SHIPYARD_DB
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.DOMAIN_REALM
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.REALM

import java.util.ArrayList
import java.util.HashMap

import javax.inject.Inject

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils

@Component
class ContainerConstraintFactory {

    @Value("#{'${cb.docker.env.ldap}'.split('\\|')}")
    private val ldapEnvs: List<String>? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val hostGroupRepository: HostGroupRepository? = null

    fun getRegistratorConstraint(gatewayHostname: String, clusterName: String, gatewayPrivateIp: String): ContainerConstraint {
        return ContainerConstraint.Builder().withName(createContainerInstanceName(REGISTRATOR.name, clusterName)).networkMode(HOST_NETWORK_MODE).instances(1).addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/tmp/docker.sock")).addHosts(ImmutableList.of(gatewayHostname)).cmd(arrayOf("-ip", gatewayPrivateIp, "-resync", Integer.toString(REGISTRATOR_RESYNC_SECONDS), String.format("consul://%s:8500", gatewayPrivateIp))).build()
    }


    fun getAmbariServerDbConstraint(gatewayHostname: String?, clusterName: String): ContainerConstraint {
        val builder = ContainerConstraint.Builder().withName(createContainerInstanceName(AMBARI_DB.name, clusterName)).instances(1).networkMode(HOST_NETWORK_MODE).addVolumeBindings(ImmutableMap.of("/data/ambari-server/pgsql/data", "/var/lib/postgresql/data",
                HOST_VOLUME_PATH + "/consul-watch", HOST_VOLUME_PATH + "/consul-watch")).addEnv(ImmutableMap.of("POSTGRES_PASSWORD", "bigdata", "POSTGRES_USER", "ambari"))
        if (gatewayHostname != null) {
            builder.addHosts(ImmutableList.of(gatewayHostname))
        }
        return builder.build()
    }

    fun getAmbariServerConstraint(dbHostname: String, gatewayHostname: String, cloudPlatform: String, clusterName: String): ContainerConstraint {
        var env = String.format("/usr/sbin/init systemd.setenv=POSTGRES_DB=%s systemd.setenv=CLOUD_PLATFORM=%s", dbHostname, cloudPlatform)
        val builder = ContainerConstraint.Builder().withName(createContainerInstanceName(AMBARI_SERVER.name, clusterName)).instances(1).networkMode(HOST_NETWORK_MODE).tcpPortBinding(TcpPortBinding(AMBARI_PORT, "0.0.0.0", AMBARI_PORT)).addVolumeBindings(ImmutableMap.of(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf")).addEnv(ImmutableMap.of("SERVICE_NAME", "ambari-8080"))
        if (!StringUtils.isEmpty(gatewayHostname)) {
            builder.addHosts(ImmutableList.of(gatewayHostname))
        } else {
            env = env + " systemd.setenv=USE_CONSUL_DNS=false"
        }
        builder.cmd(arrayOf<String>(env))
        return builder.build()
    }

    fun getHavegedConstraint(gatewayHostname: String, clusterName: String): ContainerConstraint {
        return ContainerConstraint.Builder().withNamePrefix(createContainerInstanceName(HAVEGED.name, clusterName)).instances(1).addHosts(ImmutableList.of(gatewayHostname)).build()
    }

    fun getLdapConstraint(gatewayHostname: String): ContainerConstraint {
        val env = HashMap<String, String>()
        env.put("SERVICE_NAME", LDAP.name)
        env.put("NAMESERVER_IP", "127.0.0.1")
        for (ldapEnv in ldapEnvs!!) {
            val envValue = ldapEnv.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            if (envValue.size == 2) {
                env.put(envValue[0], envValue[1])
            } else {
                throw RuntimeException(String.format("Could not be parse LDAP parameter from value: '%s'!", ldapEnv))
            }
        }

        return ContainerConstraint.Builder().withNamePrefix(LDAP.name).instances(1).networkMode(HOST_NETWORK_MODE).tcpPortBinding(TcpPortBinding(LDAP_PORT, "0.0.0.0", LDAP_PORT)).addHosts(ImmutableList.of(gatewayHostname)).addEnv(env).build()
    }

    fun getKerberosServerConstraint(cluster: Cluster, gatewayHostname: String): ContainerConstraint {
        val kerberosConf = KerberosConfiguration(cluster.kerberosMasterKey, cluster.kerberosAdmin,
                cluster.kerberosPassword)

        val env = HashMap<String, String>()
        env.put("SERVICE_NAME", KERBEROS.name)
        env.put("NAMESERVER_IP", "127.0.0.1")
        env.put("REALM", REALM)
        env.put("DOMAIN_REALM", DOMAIN_REALM)
        env.put("KERB_MASTER_KEY", kerberosConf.masterKey)
        env.put("KERB_ADMIN_USER", kerberosConf.user)
        env.put("KERB_ADMIN_PASS", kerberosConf.password)

        return ContainerConstraint.Builder().withName(createContainerInstanceName(KERBEROS.name, cluster.name)).instances(1).networkMode(HOST_NETWORK_MODE).addVolumeBindings(ImmutableMap.of(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf")).addHosts(ImmutableList.of(gatewayHostname)).addEnv(env).build()
    }

    fun getShipyardDbConstraint(gatewayHostname: String): ContainerConstraint {
        return ContainerConstraint.Builder().withName(SHIPYARD_DB.name).instances(1).tcpPortBinding(TcpPortBinding(SHIPYARD_DB_CONTAINER_PORT, "0.0.0.0", SHIPYARD_DB_EXPOSED_PORT)).addHosts(ImmutableList.of(gatewayHostname)).addEnv(ImmutableMap.of("SERVICE_NAME", SHIPYARD_DB.name)).build()
    }

    fun getShipyardConstraint(gatewayHostname: String): ContainerConstraint {
        return ContainerConstraint.Builder().withName(SHIPYARD.name).instances(1).tcpPortBinding(TcpPortBinding(SHIPYARD_CONTAINER_PORT, "0.0.0.0", SHIPYARD_EXPOSED_PORT)).addHosts(ImmutableList.of(gatewayHostname)).addEnv(ImmutableMap.of("SERVICE_NAME", SHIPYARD.name)).addLink("swarm-manager", "swarm").addLink(SHIPYARD_DB.name, "rethinkdb").cmd(arrayOf("server", "-d", "tcp://swarm:3376")).build()
    }

    fun getAmbariAgentConstraint(ambariServerHost: String, ambariAgentApp: String, cloudPlatform: String,
                                 hostGroup: HostGroup, adjustment: Int?, hostBlackList: List<String>): ContainerConstraint {
        val hgConstraint = hostGroup.constraint
        val builder = ContainerConstraint.Builder().withNamePrefix(createContainerInstanceName(hostGroup, AMBARI_AGENT.name)).withAppName(ambariAgentApp).networkMode(HOST_NETWORK_MODE)
        if (hgConstraint.instanceGroup != null) {
            val instanceGroup = hgConstraint.instanceGroup
            val dataVolumeBinds = HashMap<String, String>()
            dataVolumeBinds.put(HADOOP_MOUNT_DIR, HADOOP_MOUNT_DIR)
            dataVolumeBinds.putAll(ImmutableMap.of("/data/jars", "/data/jars", HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH))

            builder.addVolumeBindings(dataVolumeBinds)
            if (adjustment != null) {
                val candidates = collectUpscaleCandidates(hostGroup.cluster.id, hostGroup.name, adjustment)
                builder.addHosts(getHosts(candidates, instanceGroup))
            } else {
                builder.addHosts(getHosts(null, instanceGroup))
            }
            builder.cmd(arrayOf<String>(String.format(
                    "/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=CLOUD_PLATFORM=%s", ambariServerHost, cloudPlatform)))
        }

        if (hgConstraint.constraintTemplate != null) {
            builder.cpus(hgConstraint.constraintTemplate.cpu)
            builder.memory(hgConstraint.constraintTemplate.memory)
            builder.constraints(getConstraints(hostBlackList))
            if (adjustment != null) {
                builder.instances(adjustment)
            } else {
                builder.instances(hgConstraint.hostCount)
            }
            builder.withDiskSize(hgConstraint.constraintTemplate.disk)
            builder.cmd(arrayOf<String>(String.format(
                    "/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=USE_CONSUL_DNS=false", ambariServerHost)))
        }

        return builder.build()
    }

    private fun getConstraints(hostBlackList: List<String>): List<List<String>> {
        val constraints = ArrayList<List<String>>()
        constraints.add(ImmutableList.of("hostname", "UNIQUE"))
        if (!hostBlackList.isEmpty()) {
            val sb = StringBuilder()
            for (i in hostBlackList.indices) {
                sb.append(hostBlackList[i])
                if (i < hostBlackList.size - 1) {
                    sb.append(HOSTNAME_SEPARATOR)
                }
            }
            constraints.add(ImmutableList.of("hostname", "UNLIKE", sb.toString()))
        }
        return constraints
    }

    fun getConsulWatchConstraint(hosts: List<String>): ContainerConstraint {
        return ContainerConstraint.Builder().withNamePrefix(CONSUL_WATCH.name).addEnv(ImmutableMap.of("CONSUL_HOST", "127.0.0.1")).networkMode(HOST_NETWORK_MODE).addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/var/run/docker.sock")).addHosts(hosts).build()
    }

    fun getLogrotateConstraint(hosts: List<String>): ContainerConstraint {
        return ContainerConstraint.Builder().withNamePrefix(LOGROTATE.name).networkMode(HOST_NETWORK_MODE).addVolumeBindings(ImmutableMap.of("/var/lib/docker/containers", "/var/lib/docker/containers")).addHosts(hosts).build()
    }

    private fun collectUpscaleCandidates(clusterId: Long?, hostGroupName: String, adjustment: Int?): List<String>? {
        val hostGroup = hostGroupRepository!!.findHostGroupInClusterByName(clusterId, hostGroupName)
        if (hostGroup.constraint.instanceGroup != null) {
            val instanceGroupId = hostGroup.constraint.instanceGroup.id
            val unusedHostsInInstanceGroup = instanceMetaDataRepository!!.findUnusedHostsInInstanceGroup(instanceGroupId)
            val hostNames = ArrayList<String>()
            for (instanceMetaData in unusedHostsInInstanceGroup) {
                hostNames.add(instanceMetaData.discoveryFQDN)
                if (hostNames.size >= adjustment) {
                    break
                }
            }
            return hostNames
        }
        return null
    }

    private fun getHosts(candidateAddresses: List<String>?, instanceGroup: InstanceGroup): List<String> {
        val hosts = ArrayList<String>()
        for (instanceMetaData in instanceMetaDataRepository!!.findAliveInstancesInInstanceGroup(instanceGroup.id)) {
            val fqdn = instanceMetaData.discoveryFQDN
            if (candidateAddresses == null || candidateAddresses.contains(fqdn)) {
                hosts.add(instanceMetaData.discoveryFQDN)
            }
        }
        return hosts
    }

    private fun createContainerInstanceName(hostGroup: HostGroup, containerName: String): String {
        val hostGroupName = hostGroup.name
        val clusterName = hostGroup.cluster.name
        return createContainerInstanceName(containerName, hostGroupName, clusterName)
    }

    private fun createContainerInstanceName(containerName: String, clusterName: String, hostGroupName: String = ""): String {
        val separator = "-"
        val sb = StringBuilder(containerName)
        if (!StringUtils.isEmpty(hostGroupName)) {
            sb.append(separator).append(hostGroupName)
        }
        if (!StringUtils.isEmpty(clusterName)) {
            sb.append(separator).append(clusterName)
        }
        return sb.toString()
    }

    companion object {
        private val CONTAINER_VOLUME_PATH = "/var/log"
        private val HADOOP_MOUNT_DIR = "/hadoopfs"
        private val HOST_VOLUME_PATH = VolumeUtils.getLogVolume("logs")
        private val HOST_NETWORK_MODE = "host"
        private val AMBARI_PORT = 8080
        private val SHIPYARD_CONTAINER_PORT = 8080
        private val SHIPYARD_EXPOSED_PORT = 7070
        private val SHIPYARD_DB_CONTAINER_PORT = 8080
        private val SHIPYARD_DB_EXPOSED_PORT = 7071
        private val LDAP_PORT = 389
        private val REGISTRATOR_RESYNC_SECONDS = 60
        private val HOSTNAME_SEPARATOR = "|"
    }


}
