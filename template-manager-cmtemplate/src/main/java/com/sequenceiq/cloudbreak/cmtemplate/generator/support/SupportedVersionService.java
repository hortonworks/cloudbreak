package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.CmTemplateGeneratorConfigurationResolver;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersions;

@Service
public class SupportedVersionService {

    @Inject
    private CmTemplateGeneratorConfigurationResolver resolver;

    public SupportedVersions collectSupportedVersions() {
        SupportedVersions supportedVersions = new SupportedVersions();
        Set<SupportedVersion> supportedVersionsSet = new HashSet<>();
        resolver.cdhConfigurations().entrySet().forEach(entry -> {
            SupportedVersion supportedVersion = new SupportedVersion();
            supportedVersion.setType(entry.getKey().getStackType());
            supportedVersion.setVersion(entry.getKey().getVersion());
            SupportedServices supportedServices = new SupportedServices();
            for (String serviceName : entry.getValue()) {
                for (ServiceConfig serviceInformation : resolver.serviceInformations()) {
                    if (serviceInformation.getName().equals(serviceName)) {
                        SupportedService supportedService = new SupportedService();
                        supportedService.setName(serviceInformation.getName());
                        supportedService.setDisplayName(serviceInformation.getDisplayName());

                        supportedServices.getServices().add(supportedService);
                        break;
                    }
                }
            }
            supportedVersion.setSupportedServices(supportedServices);
            supportedVersionsSet.add(supportedVersion);
        });
        supportedVersions.setSupportedVersions(supportedVersionsSet);
        return supportedVersions;
    }

}
