package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionsV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersions;

@Component
public class SupportedVersionsToSupportedVersionsV4ResponseConverter {

    public SupportedVersionsV4Response convert(SupportedVersions source) {
        SupportedVersionsV4Response supportedVersionsV4Response = new SupportedVersionsV4Response();
        for (SupportedVersion supportedVersion : source.getSupportedVersions()) {

            SupportedVersionV4Response supportedVersionV4Response = new SupportedVersionV4Response();
            supportedVersionV4Response.setType(supportedVersion.getType());
            supportedVersionV4Response.setVersion(supportedVersion.getVersion());

            Set<SupportedServiceV4Response> services = new HashSet<>();
            for (SupportedService service : supportedVersion.getSupportedServices().getServices()) {
                SupportedServiceV4Response supportedServiceV4Response = new SupportedServiceV4Response();
                supportedServiceV4Response.setName(service.getName());
                supportedServiceV4Response.setDisplayName(service.getDisplayName());
                supportedServiceV4Response.setVersion(service.getVersion());
                supportedServiceV4Response.setIconKey(service.getIconKey());
                services.add(supportedServiceV4Response);
            }
            supportedVersionV4Response.setServices(services);

            supportedVersionsV4Response.getSupportedVersions().add(supportedVersionV4Response);
        }
        return supportedVersionsV4Response;
    }
}
