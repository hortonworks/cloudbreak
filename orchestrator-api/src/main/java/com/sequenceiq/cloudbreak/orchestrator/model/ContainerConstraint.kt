package com.sequenceiq.cloudbreak.orchestrator.model

import java.util.ArrayList
import java.util.HashMap

import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding

class ContainerConstraint private constructor(builder: ContainerConstraint.Builder) {

    val cmd: Array<String>
    val instances: Int?
    private val ports: List<Int>
    private val constraints: List<List<String>>
    private val env: Map<String, String>
    val networkMode: String
    val tcpPortBinding: TcpPortBinding
    val containerName: ContainerName
    private val links: Map<String, String>
    val appName: String

    private val hosts: List<String>
    private val volumeBinds: Map<String, String>
    val cpu: Double?
    val mem: Double?
    val disk: Double?


    init {
        this.cmd = builder.cmd
        this.cpu = builder.cpus
        this.mem = builder.mem
        this.instances = builder.instances
        this.ports = builder.ports
        this.constraints = builder.constraints
        this.volumeBinds = builder.volumeBinds
        this.env = builder.env
        this.networkMode = builder.networkMode
        this.tcpPortBinding = builder.tcpPortBinding
        this.hosts = builder.hosts
        this.containerName = builder.containerName
        this.links = builder.links
        this.appName = builder.appName
        this.disk = builder.disk
    }

    fun getPorts(): MutableList<Int> {
        return ports
    }

    fun getConstraints(): MutableList<List<String>> {
        return constraints
    }

    fun getVolumeBinds(): MutableMap<String, String> {
        return volumeBinds
    }

    fun getEnv(): MutableMap<String, String> {
        return env
    }

    fun getHosts(): MutableList<String> {
        return hosts
    }

    fun getLinks(): MutableMap<String, String> {
        return links
    }

    class Builder {

        private var cmd: Array<String>? = null
        private var ports: MutableList<Int> = ArrayList()
        private var cpus: Double? = null
        private var mem: Double? = null
        private var instances: Int? = null
        private var constraints: MutableList<List<String>> = ArrayList()
        private var volumeBinds: MutableMap<String, String> = HashMap()
        private var env: MutableMap<String, String> = HashMap()
        private var networkMode: String? = null
        private var tcpPortBinding: TcpPortBinding? = null
        private var hosts: MutableList<String> = ArrayList()
        private var containerName: ContainerName? = null
        private var links: MutableMap<String, String> = HashMap()
        private var appName: String? = null
        private var disk: Double? = null

        fun containerConstraint(containerConstraint: ContainerConstraint): Builder {
            this.cmd = containerConstraint.cmd
            this.ports = containerConstraint.getPorts()
            this.cpus = containerConstraint.cpu
            this.mem = containerConstraint.mem
            this.instances = containerConstraint.instances
            this.constraints = containerConstraint.getConstraints()
            this.volumeBinds = containerConstraint.getVolumeBinds()
            this.env = containerConstraint.getEnv()
            this.networkMode = containerConstraint.networkMode
            this.tcpPortBinding = containerConstraint.tcpPortBinding
            this.hosts = containerConstraint.getHosts()
            this.containerName = containerConstraint.containerName
            this.links = containerConstraint.getLinks()
            this.appName = containerConstraint.appName
            this.disk = containerConstraint.disk
            return this
        }

        fun cmd(cmd: Array<String>): Builder {
            this.cmd = cmd
            return this
        }

        fun ports(ports: List<Int>): Builder {
            this.ports.addAll(ports)
            return this
        }

        fun cpus(numberOfCpus: Double?): Builder {
            this.cpus = numberOfCpus
            return this
        }

        fun memory(megaBytesOfMemory: Double?): Builder {
            this.mem = megaBytesOfMemory
            return this
        }

        fun withDiskSize(diskSize: Double?): Builder {
            this.disk = diskSize
            return this
        }

        fun instances(numberOfInstances: Int?): Builder {
            this.instances = numberOfInstances
            return this
        }

        fun constraints(constraints: List<List<String>>): Builder {
            this.constraints.addAll(constraints)
            return this
        }

        fun addVolumeBindings(volumeBinds: Map<String, String>): Builder {
            this.volumeBinds.putAll(volumeBinds)
            return this
        }

        fun addEnv(env: Map<String, String>): Builder {
            this.env.putAll(env)
            return this
        }

        fun networkMode(networkMode: String): Builder {
            this.networkMode = networkMode
            return this
        }

        fun tcpPortBinding(binding: TcpPortBinding): Builder {
            this.tcpPortBinding = binding
            return this
        }

        fun addHosts(hosts: List<String>): Builder {
            this.hosts.addAll(hosts)
            return this
        }

        fun withNamePrefix(namePrefix: String): Builder {
            this.containerName = ContainerName(null, namePrefix)
            return this
        }

        fun withName(name: String): Builder {
            this.containerName = ContainerName(name, null)
            return this
        }

        fun addLink(hostContainerLink: String, link: String): Builder {
            this.links.put(hostContainerLink, link)
            return this
        }

        fun withAppName(appName: String): Builder {
            this.appName = appName
            return this
        }

        fun build(): ContainerConstraint {
            return ContainerConstraint(this)
        }
    }
}
