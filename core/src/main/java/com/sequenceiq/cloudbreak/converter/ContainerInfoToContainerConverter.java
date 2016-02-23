package com.sequenceiq.cloudbreak.converter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;

@Component
public class ContainerInfoToContainerConverter extends AbstractConversionServiceAwareConverter<ContainerInfo, Container> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerInfoToContainerConverter.class);

    @Override
    public Container convert(ContainerInfo source) {
        Container container = new Container();
        container.setContainerId(source.getId());
        container.setName(source.getName());
        container.setImage(source.getImage());
        container.setHost(source.getHost());
        return container;
    }
}
