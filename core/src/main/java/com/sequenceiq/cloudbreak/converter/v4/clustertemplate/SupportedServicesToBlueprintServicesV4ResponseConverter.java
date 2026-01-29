package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintServicesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedServiceV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;

@Component
public class SupportedServicesToBlueprintServicesV4ResponseConverter {

    public BlueprintServicesV4Response convert(SupportedServices source) {
        BlueprintServicesV4Response blueprintServicesV4Response = new BlueprintServicesV4Response();

        Set<SupportedServiceV4Response> services = new TreeSet<>();
        for (SupportedService service : source.getServices()) {
            SupportedServiceV4Response supportedServiceV4Response = new SupportedServiceV4Response();
            supportedServiceV4Response.setDisplayName(service.getDisplayName());
            supportedServiceV4Response.setVersion(service.getVersion());
            supportedServiceV4Response.setName(service.getName());
            supportedServiceV4Response.setIconKey(service.getIconKey());
            services.add(supportedServiceV4Response);
        }
        blueprintServicesV4Response.setServices(services);
        return blueprintServicesV4Response;
    }
}
