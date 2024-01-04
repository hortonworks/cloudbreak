package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.CmTemplateGeneratorConfigurationResolver;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix.CdhService;
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
        resolver.cdhConfigurations().forEach((key, value) -> {
            SupportedVersion supportedVersion = new SupportedVersion();
            supportedVersion.setType(key.getStackType());
            supportedVersion.setVersion(key.getVersion());
            SupportedServices supportedServices = new SupportedServices();
            for (CdhService serviceName : value) {
                for (ServiceConfig serviceInformation : resolver.serviceConfigs()) {
                    if (serviceInformation.getName().equals(serviceName.getName())) {
                        SupportedService supportedService = new SupportedService();
                        supportedService.setName(serviceInformation.getName());
                        supportedService.setDisplayName(serviceInformation.getDisplayName());
                        supportedService.setVersion(serviceName.getVersion());
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
