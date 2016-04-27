package com.sequenceiq.cloudbreak.converter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.HostService;
import com.sequenceiq.cloudbreak.orchestrator.model.ServiceInfo;

@Component
public class ServiceInfoToHostServiceConverter extends AbstractConversionServiceAwareConverter<ServiceInfo, HostService> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfoToHostServiceConverter.class);

    @Override
    public HostService convert(ServiceInfo source) {
        HostService hostService = new HostService();
        hostService.setName(source.getName());
        hostService.setHost(source.getHost());
        return hostService;
    }
}
