package com.sequenceiq.cloudbreak.converter


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo

@Component
class ContainerInfoToContainerConverter : AbstractConversionServiceAwareConverter<ContainerInfo, Container>() {

    override fun convert(source: ContainerInfo): Container {
        val container = Container()
        container.containerId = source.id
        container.name = source.name
        container.image = source.image
        container.host = source.host
        return container
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ContainerInfoToContainerConverter::class.java)
    }
}
