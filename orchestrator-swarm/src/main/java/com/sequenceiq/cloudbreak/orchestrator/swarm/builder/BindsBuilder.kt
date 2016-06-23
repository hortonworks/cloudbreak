package com.sequenceiq.cloudbreak.orchestrator.swarm.builder

import java.util.ArrayList

import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath

class BindsBuilder {

    private val binds: MutableList<Bind>

    init {
        binds = ArrayList<Bind>()
    }

    fun addDockerSocket(): BindsBuilder {
        return add("/var/run/docker.sock")
    }

    fun addDockerSocket(containerPath: String): BindsBuilder {
        return add("/var/run/docker.sock", containerPath)
    }

    fun addLog(logVolumePath: LogVolumePath): BindsBuilder {
        return add(logVolumePath.hostPath, logVolumePath.containerPath)
    }

    fun addLog(logVolumePath: LogVolumePath, vararg subdirs: String): BindsBuilder {
        for (subdir in subdirs) {
            add(logVolumePath.hostPath + "/" + subdir, logVolumePath.containerPath + "/" + subdir)
        }
        return this
    }

    fun add(vararg paths: String): BindsBuilder {
        for (path in paths) {
            add(path)
        }
        return this
    }


    fun add(paths: Collection<String>): BindsBuilder {
        for (path in paths) {
            add(path)
        }
        return this
    }

    fun add(path: String): BindsBuilder {
        return add(path, path)
    }

    fun add(hostPath: String, containerPath: String): BindsBuilder {
        val bind = Bind(hostPath, Volume(containerPath))
        binds.add(bind)
        return this
    }

    fun build(): Array<Bind> {
        return binds.toArray<Bind>(arrayOfNulls<Bind>(binds.size))
    }
}
