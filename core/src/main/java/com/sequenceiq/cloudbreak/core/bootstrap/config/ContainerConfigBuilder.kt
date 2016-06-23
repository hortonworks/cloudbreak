package com.sequenceiq.cloudbreak.core.bootstrap.config

import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig

class ContainerConfigBuilder private constructor(builder: ContainerConfigBuilder.Builder) {

    private val containerConfig: ContainerConfig

    init {
        this.containerConfig = ContainerConfig(builder.name, builder.version)
    }

    class Builder(imageNameAndVersion: String) {

        private val name: String

        private val version: String

        init {
            val image = imageNameAndVersion.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            this.name = image[0]
            this.version = if (image.size > 1) image[1] else "latest"
        }

        fun build(): ContainerConfig {
            return ContainerConfigBuilder(this).containerConfig
        }

    }
}
